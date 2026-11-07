/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.nbri_ehr;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import org.labkey.api.action.ApiSimpleResponse;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.SimpleApiJsonForm;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.ehr.EHRService;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.RuntimeValidationException;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.ValidEmail;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.nbri_ehr.dataentry.form.NBRIDailyHealthFormType;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class NBRI_EHRController extends SpringActionController
{
    private static final DefaultActionResolver _actionResolver = new DefaultActionResolver(NBRI_EHRController.class);
    private static final Logger LOG = LogHelper.getLogger(NBRI_EHRController.class, "Daily health API saves");
    public static final String NAME = "nbri_ehr";

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    public NBRI_EHRController()
    {
        setActionResolver(_actionResolver);
    }

    /**
     * Saves daily health log entries from the barcode scanner service. Each entry saves under its own task in its own
     * transaction, so one bad entry doesn't block the rest, and an entry already on file is reported rather than duplicated.
     */
    @RequiresPermission(InsertPermission.class)
    public static class SaveDailyHealthAction extends MutatingApiAction<SimpleApiJsonForm>
    {
        @Override
        public void validateForm(SimpleApiJsonForm form, Errors errors)
        {
            JSONObject json = form.getJsonObject();
            if (json == null || !(json.opt("entries") instanceof JSONArray))
                errors.reject(ERROR_MSG, "Expected a JSON body with an 'entries' array");
        }

        @Override
        public Object execute(SimpleApiJsonForm form, BindException errors)
        {
            JSONArray entries = form.getJsonObject().getJSONArray("entries");
            List<Map<String, Object>> results = new ArrayList<>();
            for (int i = 0; i < entries.length(); i++)
            {
                JSONObject entry = entries.optJSONObject(i);
                results.add(entry == null ? result(null, "error", List.of("Entry " + i + " is not an object")) : saveEntry(entry));
            }

            ApiSimpleResponse response = new ApiSimpleResponse();
            response.put("results", results);
            response.put("success", results.stream().noneMatch(r -> "error".equals(r.get("status"))));
            return response;
        }

        private Map<String, Object> saveEntry(JSONObject entry)
        {
            List<String> problems = new ArrayList<>();
            String logEntryId = StringUtils.trimToNull(entry.optString("logEntryId", null));
            if (logEntryId == null || !UUID_PATTERN.matcher(logEntryId).matches())
                problems.add("logEntryId must be a UUID");
            else
                logEntryId = logEntryId.toLowerCase();

            String animalId = StringUtils.trimToNull(entry.optString("animalId", null));
            if (animalId == null)
                problems.add("animalId is required");

            Date date = parseDate(entry.opt("observationDateTime"), problems);
            String technician = resolveTechnician(entry.optString("technician", null), problems);

            String area = StringUtils.trimToNull(entry.optString("locationArea", null));
            String enclosure = StringUtils.trimToNull(entry.optString("locationEnclosure", null));
            if (area == null || enclosure == null)
                problems.add("locationArea and locationEnclosure are required");

            List<Map<String, Object>> rows = new ArrayList<>();
            JSONArray codes = entry.optJSONArray("codes");
            if (codes == null || codes.isEmpty())
                problems.add("codes must list at least one code");
            else
            {
                for (int i = 0; i < codes.length(); i++)
                {
                    JSONObject code = codes.optJSONObject(i);
                    if (code == null || StringUtils.isBlank(code.optString("code", null)) || !(code.opt("sortOrder") instanceof Number))
                    {
                        problems.add("codes[" + i + "] needs a code and a numeric sortOrder");
                        continue;
                    }
                    Map<String, Object> row = new CaseInsensitiveHashMap<>();
                    row.put("Id", animalId);
                    row.put("date", date);
                    row.put("logEntryId", logEntryId);
                    row.put("sortOrder", code.getInt("sortOrder"));
                    row.put("code", code.getString("code").trim());
                    row.put("performedby", technician);
                    row.put("taskid", logEntryId);
                    row.put("QCStateLabel", EHRService.QCSTATES.Completed.getLabel());
                    rows.add(row);
                }
            }

            if (!problems.isEmpty())
                return result(logEntryId, "error", problems);

            TableInfo datasetTable = getTable("study", "dailyHealthLog");
            if (new TableSelector(datasetTable, new SimpleFilter(FieldKey.fromString("logEntryId"), logEntryId), null).exists())
                return result(logEntryId, "exists", List.of());

            Map<String, Object> extraContext = new CaseInsensitiveHashMap<>();
            extraContext.put("locationArea", area);
            extraContext.put("locationEnclosure", enclosure);
            extraContext.put("locationCage", StringUtils.trimToNull(entry.optString("locationCage", null)));

            BatchValidationException saveErrors = new BatchValidationException();
            try (DbScope.Transaction transaction = datasetTable.getSchema().getScope().ensureTransaction())
            {
                Map<String, Object> task = new CaseInsensitiveHashMap<>();
                task.put("taskid", logEntryId);
                task.put("title", NBRIDailyHealthFormType.NAME);
                task.put("formtype", NBRIDailyHealthFormType.NAME);
                task.put("category", "task");
                task.put("qcstate", EHRService.get().getQCStates(getContainer()).get(EHRService.QCSTATES.Completed.getLabel()).getRowId());
                task.put("assignedto", getUser().getUserId());
                Objects.requireNonNull(getTable("ehr", "tasks").getUpdateService()).insertRows(getUser(), getContainer(), List.of(task), saveErrors, null, null);

                if (!saveErrors.hasErrors())
                    Objects.requireNonNull(datasetTable.getUpdateService()).insertRows(getUser(), getContainer(), rows, saveErrors, null, extraContext);

                if (!saveErrors.hasErrors())
                {
                    transaction.commit();
                    return result(logEntryId, "created", List.of());
                }
            }
            catch (Exception e)
            {
                // entry-level checks run as the transaction commits
                if (e instanceof RuntimeValidationException rve)
                    return result(logEntryId, "error", List.of(rve.getValidationException().getMessage()));
                if (e instanceof BatchValidationException bve)
                    saveErrors = bve;
                else
                {
                    LOG.error("Unable to save daily health log entry " + logEntryId, e);
                    return result(logEntryId, "error", List.of(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName())));
                }
            }

            return result(logEntryId, "error", errorMessages(saveErrors));
        }

        private @Nullable Date parseDate(Object value, List<String> problems)
        {
            if (!(value instanceof String text) || text.isBlank())
            {
                problems.add("observationDateTime is required");
                return null;
            }

            // An offset is converted to server time; a bare local time is taken as server time already.
            try
            {
                return Date.from(OffsetDateTime.parse(text.trim()).toInstant());
            }
            catch (DateTimeParseException e)
            {
                try
                {
                    return Date.from(LocalDateTime.parse(text.trim()).atZone(ZoneId.systemDefault()).toInstant());
                }
                catch (DateTimeParseException e2)
                {
                    problems.add("observationDateTime must be an ISO 8601 date-time: " + text);
                    return null;
                }
            }
        }

        /** performedby holds the LabKey user id as text, matching what the data entry forms store. */
        private @Nullable String resolveTechnician(@Nullable String value, List<String> problems)
        {
            String technician = StringUtils.trimToNull(value);
            if (technician == null)
            {
                problems.add("technician is required");
                return null;
            }

            User user = null;
            try
            {
                user = UserManager.getUser(new ValidEmail(technician));
            }
            catch (ValidEmail.InvalidEmailException ignored)
            {
            }
            if (user == null)
                user = UserManager.getUserByDisplayName(technician);
            if (user == null || !user.isActive())
            {
                problems.add("technician " + technician + " is not an active LabKey user");
                return null;
            }
            return String.valueOf(user.getUserId());
        }

        private @NotNull TableInfo getTable(String schemaName, String queryName)
        {
            UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), schemaName);
            TableInfo table = schema == null ? null : schema.getTable(queryName);
            if (table == null)
                throw new IllegalStateException("Unable to find " + schemaName + "." + queryName + " in " + getContainer().getPath());
            return table;
        }

        private static List<String> errorMessages(BatchValidationException errors)
        {
            Set<String> messages = new HashSet<>();
            List<String> ordered = new ArrayList<>();
            for (ValidationException rowError : errors.getRowErrors())
            {
                String message = rowError.getMessage();
                if (message != null && messages.add(message))
                    ordered.add(message);
            }
            return ordered;
        }

        private static Map<String, Object> result(@Nullable String logEntryId, String status, List<String> errors)
        {
            Map<String, Object> result = new CaseInsensitiveHashMap<>();
            result.put("logEntryId", logEntryId);
            result.put("status", status);
            result.put("errors", errors);
            return result;
        }
    }
}

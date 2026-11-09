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
import org.labkey.api.action.ConfirmAction;
import org.labkey.api.action.MutatingApiAction;
import org.labkey.api.action.SimpleApiJsonForm;
import org.labkey.api.action.SpringActionController;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.ehr.EHRService;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryAction;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.RuntimeValidationException;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.ValidationException;
import org.labkey.api.reader.DataLoader;
import org.labkey.api.reader.TabLoader;
import org.labkey.api.resource.Resource;
import org.labkey.api.security.RequiresPermission;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.ValidEmail;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.util.HtmlString;
import org.labkey.api.util.URLHelper;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.view.HtmlView;
import org.labkey.nbri_ehr.dataentry.form.NBRIDailyHealthFormType;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
     * Saves daily health log entries from the barcode scanner service as one atomic batch: every new entry saves, each
     * under its own task, or none does. An entry already on file is reported rather than duplicated.
     */
    @RequiresPermission(InsertPermission.class)
    public static class SaveDailyHealthAction extends MutatingApiAction<SimpleApiJsonForm>
    {
        private static final String CREATED = "created";
        private static final String EXISTS = "exists";
        private static final String ERROR = "error";
        private static final String NOT_SAVED = "notSaved";

        private record ParsedEntry(@Nullable String logEntryId, List<Map<String, Object>> rows, Map<String, Object> extraContext) {}

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
            TableInfo datasetTable = getTable("study", "dailyHealthLog");
            List<Map<String, Object>> results = new ArrayList<>();
            Map<Integer, ParsedEntry> pending = new LinkedHashMap<>();
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < entries.length(); i++)
            {
                JSONObject json = entries.optJSONObject(i);
                if (json == null)
                {
                    results.add(result(null, ERROR, List.of("Entry " + i + " is not an object")));
                    continue;
                }

                List<String> problems = new ArrayList<>();
                ParsedEntry entry = parseEntry(json, problems);
                String logEntryId = entry.logEntryId();
                if (logEntryId != null && !seen.add(logEntryId))
                    problems.add("logEntryId " + logEntryId + " appears more than once in the batch");

                if (!problems.isEmpty())
                    results.add(result(logEntryId, ERROR, problems));
                else if (new TableSelector(datasetTable, new SimpleFilter(FieldKey.fromString("logEntryId"), logEntryId), null).exists())
                    results.add(result(logEntryId, EXISTS, List.of()));
                else
                {
                    results.add(result(logEntryId, NOT_SAVED, List.of()));
                    pending.put(i, entry);
                }
            }

            if (!pending.isEmpty() && results.stream().noneMatch(r -> ERROR.equals(r.get("status"))))
                saveAll(datasetTable, pending, results);

            ApiSimpleResponse response = new ApiSimpleResponse();
            response.put("results", results);
            response.put("success", results.stream().noneMatch(r -> ERROR.equals(r.get("status"))));
            return response;
        }

        private ParsedEntry parseEntry(JSONObject entry, List<String> problems)
        {
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

            Map<String, Object> extraContext = new CaseInsensitiveHashMap<>();
            extraContext.put("locationArea", area);
            extraContext.put("locationEnclosure", enclosure);
            extraContext.put("locationCage", StringUtils.trimToNull(entry.optString("locationCage", null)));
            return new ParsedEntry(logEntryId, rows, extraContext);
        }

        /** Stops at the first entry that fails: closing a LabKey transaction without committing aborts it as a whole. */
        private void saveAll(TableInfo datasetTable, Map<Integer, ParsedEntry> pending, List<Map<String, Object>> results)
        {
            try (DbScope.Transaction transaction = datasetTable.getSchema().getScope().ensureTransaction())
            {
                for (Map.Entry<Integer, ParsedEntry> item : pending.entrySet())
                {
                    List<String> entryErrors = insertEntry(datasetTable, item.getValue());
                    if (!entryErrors.isEmpty())
                    {
                        results.set(item.getKey(), result(item.getValue().logEntryId(), ERROR, entryErrors));
                        return;
                    }
                }
                transaction.commit();
            }
            catch (RuntimeValidationException e)
            {
                // entry-level checks run as the transaction commits, and name the entries they reject
                String message = e.getValidationException().getMessage();
                List<String> parts = List.of(message.split("; "));
                pending.forEach((i, entry) -> {
                    String logEntryId = Objects.requireNonNull(entry.logEntryId());
                    List<String> mine = parts.stream().filter(part -> part.contains(logEntryId)).toList();
                    if (!mine.isEmpty())
                        results.set(i, result(entry.logEntryId(), ERROR, mine));
                });
                if (results.stream().noneMatch(r -> ERROR.equals(r.get("status"))))
                    pending.forEach((i, entry) -> results.set(i, result(entry.logEntryId(), ERROR, List.of(message))));
                return;
            }
            catch (RuntimeException e)
            {
                LOG.error("Unable to save a batch of daily health log entries", e);
                String message = StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName());
                pending.forEach((i, entry) -> results.set(i, result(entry.logEntryId(), ERROR, List.of(message))));
                return;
            }

            pending.forEach((i, entry) -> results.set(i, result(entry.logEntryId(), CREATED, List.of())));
        }

        /** Returns the entry's errors; empty when its task and rows were inserted. */
        private List<String> insertEntry(TableInfo datasetTable, ParsedEntry entry)
        {
            BatchValidationException saveErrors = new BatchValidationException();
            try
            {
                Map<String, Object> task = new CaseInsensitiveHashMap<>();
                task.put("taskid", entry.logEntryId());
                task.put("title", NBRIDailyHealthFormType.NAME);
                task.put("formtype", NBRIDailyHealthFormType.NAME);
                task.put("category", "task");
                task.put("qcstate", EHRService.get().getQCStates(getContainer()).get(EHRService.QCSTATES.Completed.getLabel()).getRowId());
                task.put("assignedto", getUser().getUserId());
                Objects.requireNonNull(getTable("ehr", "tasks").getUpdateService()).insertRows(getUser(), getContainer(), List.of(task), saveErrors, null, null);

                if (!saveErrors.hasErrors())
                    Objects.requireNonNull(datasetTable.getUpdateService()).insertRows(getUser(), getContainer(), entry.rows(), saveErrors, null, entry.extraContext());
            }
            catch (BatchValidationException e)
            {
                saveErrors = e;
            }
            catch (RuntimeValidationException e)
            {
                return List.of(e.getValidationException().getMessage());
            }
            catch (Exception e)
            {
                LOG.error("Unable to save daily health log entry " + entry.logEntryId(), e);
                return List.of(StringUtils.defaultIfBlank(e.getMessage(), e.getClass().getSimpleName()));
            }

            List<String> messages = errorMessages(saveErrors);
            if (saveErrors.hasErrors() && messages.isEmpty())
                messages.add(StringUtils.defaultIfBlank(saveErrors.getMessage(), "Unable to save log entry " + entry.logEntryId()));
            return messages;
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

    /** Adds the standard daily health codes a folder is missing, leaving codes already there, including edited or retired ones, untouched. */
    @RequiresPermission(AdminPermission.class)
    public static class PopulateDailyHealthCodesAction extends ConfirmAction<Object>
    {
        @Override
        public ModelAndView getConfirmView(Object form, BindException errors)
        {
            return new HtmlView(HtmlString.of("Add the standard daily health codes that this folder doesn't have yet? Existing codes are left as they are."));
        }

        @Override
        public void validateCommand(Object form, Errors errors)
        {
        }

        @Override
        public boolean handlePost(Object form, BindException errors) throws Exception
        {
            TableInfo table = getCodeTable();
            Set<String> existing = new TableSelector(table.getColumn("code")).getArrayList(String.class).stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            Resource tsv = ModuleLoader.getInstance().getModule(NBRI_EHRModule.class).getModuleResource("data/dailyHealthCode.tsv");
            List<Map<String, Object>> rows = new ArrayList<>();
            try (DataLoader loader = DataLoader.get().createLoader(tsv, true, null, TabLoader.TSV_FILE_TYPE))
            {
                for (Map<String, Object> source : loader.load())
                {
                    Map<String, Object> row = new CaseInsensitiveHashMap<>();
                    source.forEach((name, value) -> row.put(name, value == null ? null : value.toString()));
                    if (!existing.contains(String.valueOf(row.get("code")).toUpperCase()))
                        rows.add(row);
                }
            }

            if (!rows.isEmpty())
            {
                BatchValidationException saveErrors = new BatchValidationException();
                Objects.requireNonNull(table.getUpdateService()).insertRows(getUser(), getContainer(), rows, saveErrors, null, null);
                if (saveErrors.hasErrors())
                    throw saveErrors;
            }
            return true;
        }

        @Override
        public @NotNull URLHelper getSuccessURL(Object form)
        {
            return QueryService.get().urlFor(getUser(), getContainer(), QueryAction.executeQuery, NBRI_EHRSchema.NAME, "dailyHealthCode");
        }

        private @NotNull TableInfo getCodeTable()
        {
            UserSchema schema = QueryService.get().getUserSchema(getUser(), getContainer(), NBRI_EHRSchema.NAME);
            TableInfo table = schema == null ? null : schema.getTable("dailyHealthCode");
            if (table == null)
                throw new IllegalStateException("Unable to find " + NBRI_EHRSchema.NAME + ".dailyHealthCode in " + getContainer().getPath());
            return table;
        }
    }
}

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
package org.labkey.nbri_ehr.query;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashMap;
import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerManager;
import org.labkey.api.data.ConvertHelper;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.Results;
import org.labkey.api.data.ResultsImpl;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.ehr.EHRDemographicsService;
import org.labkey.api.ehr.EHRService;
import org.labkey.api.ehr.notification.AbstractEHRNotification;
import org.labkey.api.ehr.security.EHRVeterinarianPermission;
import org.labkey.api.ldk.notification.NotificationService;
import org.labkey.api.puppeteer.PuppeteerService;
import org.labkey.api.query.BatchValidationException;
import org.labkey.api.query.DuplicateKeyException;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.InvalidKeyException;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.QueryUpdateServiceException;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.SecurityManager;
import org.labkey.api.security.User;
import org.labkey.api.security.UserManager;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.settings.AppProps;
import org.labkey.api.settings.LookAndFeelProperties;
import org.labkey.api.study.StudyService;
import org.labkey.api.util.FileUtil;
import org.labkey.api.util.GUID;
import org.labkey.api.util.JobRunner;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.view.ActionURL;
import org.labkey.nbri_ehr.NBRI_EHRManager;
import org.labkey.nbri_ehr.dataentry.form.NBRIClinicalObservationsFormType;
import org.labkey.nbri_ehr.dataentry.form.NBRINecropsyBaseFormType;
import org.labkey.nbri_ehr.notification.NBRIClinicalMoveNotification;
import org.labkey.nbri_ehr.notification.NBRIDeathNotification;
import org.labkey.nbri_ehr.notification.NBRINecropsyFinalNotification;
import org.labkey.nbri_ehr.notification.NBRINecropsyPreliminaryNotification;
import org.labkey.nbri_ehr.notification.NBRINecropsyRequestNotification;
import org.labkey.nbri_ehr.notification.NBRIPregnancyOutcomeNotification;
import org.labkey.nbri_ehr.notification.TriggerScriptNotification;
import org.labkey.vfs.FileLike;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class NBRI_EHRTriggerHelper
{
    private Container _container;
    private User _user;
    private static final Logger _log = LogManager.getLogger(NBRI_EHRTriggerHelper.class);
    private final Map<String,Object> _cachedDrugFormulary = new HashMap<>();
    private final Map<String,String> _cachedObservationTypeCategories = new HashMap<>();

    // Maps an originating observation order's taskid to the task its scheduled observations are grouped under,
    // for the duration of a single save batch (the same helper instance is reused across rows in the batch).
    private final Map<String,String> _scheduledObsTaskMap = new HashMap<>();

    private final SimpleDateFormat _dateFormat;

    // Animals named in the occupancy error before it switches to a count.
    private static final int MAX_NAMED_ANIMALS = 10;
    private static final String NECROPSY_ALIAS_CATEGORY = "A";

    public NBRI_EHRTriggerHelper(int userId, String containerId)
    {
        _user = UserManager.getUser(userId);
        if (_user == null)
            throw new RuntimeException("User does not exist: " + userId);

        _container = ContainerManager.getForId(containerId);
        if (_container == null)
            throw new RuntimeException("Container does not exist: " + containerId);

        _dateFormat = new SimpleDateFormat(LookAndFeelProperties.getInstance(_container).getDefaultDateFormat());

    }

    private TableInfo getTableInfo(String schemaName, String queryName)
    {
        UserSchema us = QueryService.get().getUserSchema(_user, _container, schemaName);
        if (us == null)
            throw new IllegalArgumentException("Unable to find schema: " + schemaName);

        TableInfo ti = us.getTable(queryName);
        if (ti == null)
            throw new IllegalArgumentException("Unable to find table: " + schemaName + "." + queryName);

        return ti;
    }

    public Map<String, Object> getExtraContext()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("quickValidation", true);
        map.put("generatedByServer", true);

        return map;
    }

    public String createHousingRecord(String id, Map<String, Object> row, String formName) throws QueryUpdateServiceException, DuplicateKeyException, SQLException, BatchValidationException, InvalidKeyException
    {
        BatchValidationException errors = new BatchValidationException();
        Date date = ConvertHelper.convert(row.get("date"), Date.class);
        String location = ConvertHelper.convert(row.get("cage"), String.class);
        String reason = ConvertHelper.convert(row.get("reason"), String.class);
        Integer performedby = ConvertHelper.convert(row.get("performedby"), Integer.class);
        if (id == null || date == null || location == null)
            return "Attempting to create a housing record with no id, date, or location";

        boolean updateRecord = false;
        Date enddate = ConvertHelper.convert(row.get("enddate"), Date.class);

        //check for a pre-existing death record
        Date deathDate = new TableSelector(getTableInfo("study", "deaths"), Collections.singleton("date"), new SimpleFilter(FieldKey.fromString("Id"), id), null).getObject(Date.class);
        if (deathDate != null)
        {
            if (deathDate.before(date))
            {
                return "Attempting to create a housing record that starts after the death date: " + _dateFormat.format(date);
            }
            else if (enddate == null || enddate.after(deathDate))
            {
                enddate = deathDate;
            }
        }

        TableInfo ti = getTableInfo("study", "housing");

        String taskId = ConvertHelper.convert(row.get("taskid"), String.class);
        if (taskId == null)
            return "Attempting to create " + formName + " record with no taskid";

        // If updating an existing arrival record with housing info, check if the housing record should be closed
        if (enddate == null)
        {
            SimpleFilter nextFilter = new SimpleFilter(FieldKey.fromString("Id"), id);
            nextFilter.addCondition(FieldKey.fromString("date"), date, CompareType.DATE_GTE);
            nextFilter.addCondition(FieldKey.fromString("taskid"), taskId, CompareType.NEQ); // Don't include the current record
            TableSelector ts = new TableSelector(ti, PageFlowUtil.set("date"), nextFilter, new Sort("date"));
            List<Date> dates = ts.getArrayList(Date.class);
            if (!dates.isEmpty())
                enddate = dates.getFirst();
        }

        String qcstate = ConvertHelper.convert(row.get("qcstate"), String.class);
        if (qcstate == null)
            return "Attempting to create " + formName + " record with no qcstate";

        // If there is already a housing record for this task, update that record
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), id);
        filter.addCondition(FieldKey.fromString("taskid"), taskId);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("lsid", "objectid"), filter, null);
        if (ts.exists())
            updateRecord = true;

        Map<String, Object> saveRow = new CaseInsensitiveHashMap<>();
        saveRow.put("Id", id);
        saveRow.put("date", date);
        saveRow.put("cage", location);
        saveRow.put("taskId", taskId);
        saveRow.put("qcstate", qcstate);
        saveRow.put("reason", reason);
        saveRow.put("performedby", performedby);
        if (updateRecord)
            saveRow.put("objectid", ts.getMap().get("objectid"));
        else
            saveRow.put("objectid", new GUID().toString());

        if (enddate != null)
            saveRow.put("enddate", enddate);

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(saveRow);

        if (updateRecord)
        {
            ti.getUpdateService().updateRows(_user, _container, rows, null, null, getExtraContext());
        }
        else
        {
            ti.getUpdateService().insertRows(_user, _container, rows, errors, null, getExtraContext());
        }

        if (errors.hasErrors())
            throw errors;

        return null;
    }

    public String saveBirthRecord(String id, Map<String, Object> row) throws QueryUpdateServiceException, DuplicateKeyException, SQLException, BatchValidationException, InvalidKeyException
    {
        Date date = ConvertHelper.convert(row.get("date"), Date.class);
        if (id == null || date == null)
            return "Attempting to create a birth record with no id or date";

        boolean updateRecord = false;

        //check for a pre-existing death record
        Date deathDate = new TableSelector(getTableInfo("study", "deaths"), Collections.singleton("date"), new SimpleFilter(FieldKey.fromString("Id"), id), null).getObject(Date.class);
        if (deathDate != null)
        {
            if (deathDate.before(date))
            {
                return "Attempting to create a birth record that starts after the death date: " + _dateFormat.format(date);
            }
        }

        String taskId = ConvertHelper.convert(row.get("taskid"), String.class);
        if (taskId == null) {
            return "Attempting to create a birth record with no taskid";
        }

        String qcstate = ConvertHelper.convert(row.get("qcstate"), String.class);
        if (qcstate == null) {
            return "Attempting to create a birth record with no qcstate";
        }

        Integer performedby = ConvertHelper.convert(row.get("performedby"), Integer.class);
        if (performedby == null) {
            return "Attempting to create a birth record with no performedby";
        }

        TableInfo ti = getTableInfo("study", "birth");

        // If there is already a housing record for this task, update that record
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), id);
        filter.addCondition(FieldKey.fromString("taskid"), taskId);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("lsid", "objectid"), filter, null);
        if (ts.exists())
        {
            updateRecord = true;
        }

        Map<String, Object> saveRow = new CaseInsensitiveHashMap<>();
        saveRow.put("Id", id);
        saveRow.put("date", date);
        saveRow.put("taskId", taskId);
        saveRow.put("qcstate", qcstate);
        saveRow.put("performedby", performedby);
        if (updateRecord)
        {
            saveRow.put("objectid", ts.getMap().get("objectid"));
        }
        else
        {
            saveRow.put("objectid", new GUID().toString());
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(saveRow);
        BatchValidationException errors = new BatchValidationException();

        if (updateRecord)
        {
            ti.getUpdateService().updateRows(_user, _container, rows, null, null, getExtraContext());
        }
        else
        {
            ti.getUpdateService().insertRows(_user, _container, rows, errors, null, getExtraContext());
        }

        if (errors.hasErrors())
            throw errors;

        return null;
    }

    public boolean animalIdExists(String id)
    {
        TableInfo ti = getTableInfo("study", "demographics");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), id);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("lsid"), filter, null);
        return ts.exists();
    }

    /**
     * Null both when the animal has no demographics record and when it has one carrying no generation; birth.js treats
     * the two the same, so the caller never needs to tell them apart.
     */
    public Integer getGeneration(String id)
    {
        TableInfo ti = getTableInfo("study", "demographics");
        if (null == ti.getColumn("generation"))
            throw new IllegalStateException("The demographics dataset has no 'generation' column. Import the reference study to add it.");

        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("generation"), new SimpleFilter(FieldKey.fromString("Id"), id), null);

        return ts.getObject(Integer.class);
    }

    public boolean birthExists(String id)
    {
        TableInfo ti = getTableInfo("study", "birth");
        if (ti != null)
        {
            SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), id);
            TableSelector ts = new TableSelector(ti, PageFlowUtil.set("lsid"), filter, null);
            return ts.exists();
        }
        return false;
    }

    public boolean deathExists(String id)
    {
        TableInfo ti = getTableInfo("study", "deaths");
        if (ti != null)
        {
            SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), id);
            TableSelector ts = new TableSelector(ti, PageFlowUtil.set("lsid"), filter, null);
            return ts.exists();
        }
        return false;
    }

    /**
     * Derives the denormalized birth/death values on study.demographics from the birth and deaths event records, which
     * are authoritative, and returns only the animals whose stored values disagree. The result is intended to be handed
     * straight to the shared trigger helper's updateDemographicsRecord(), so that lsid resolution and the demographics
     * cache recache stay in the single place that already handles them.
     * <p>
     * Only public (Completed) event records count, so a record still in data entry never overwrites a saved value.
     * <p>
     * calculated_status is deliberately absent from the result. It belongs to the shared status recalc, which owns the
     * death/departure/re-arrival precedence.
     * <p>
     * Note that a stored value is only ever cleared for death, by the AFTER_DELETE handler on study.deaths. There is no
     * equivalent handler on study.birth, so deleting a birth record leaves demographics.birth at its last known value
     * and this method will not heal it - a missing event record deliberately leaves the stored value alone.
     * <p>
     * Every lookup is set-based - one query per event dataset for the whole id list, not one per animal - because a
     * bulk save can pass hundreds of ids and per-animal SQL in a trigger exhausts the script's wall-clock budget.
     *
     * @param ids animals touched by the current save
     * @return rows ready for updateDemographicsRecord(); empty when nothing has drifted
     */
    public List<Map<String, Object>> computeDemographicsSync(List<String> ids)
    {
        // a JS array arrives as a Rhino NativeArray, whose inherited isEmpty() is always true; use size() instead
        //noinspection SizeReplaceableByIsEmpty
        if (ids == null || ids.size() == 0)
            return Collections.emptyList();

        Set<String> idSet = new HashSet<>(ids);

        Map<String, Date> births = getPublicEventDates("birth", idSet);
        Map<String, Date> deaths = getPublicEventDates("deaths", idSet);

        List<Map<String, Object>> updates = new ArrayList<>();

        TableInfo demographics = getTableInfo("study", "demographics");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), idSet, CompareType.IN);
        TableSelector ts = new TableSelector(demographics, PageFlowUtil.set("Id", "birth", "death"), filter, null);

        for (Map<String, Object> current : ts.getMapCollection())
        {
            String id = (String)current.get("Id");
            Map<String, Object> update = new CaseInsensitiveHashMap<>();

            // A public event record always wins. A missing one is NOT evidence the stored value is wrong - ETL-loaded
            // and pre-dataset animals legitimately carry a date with no event row - so it leaves the value alone.
            Date birth = births.get(id);
            if (birth != null && differsByDay(birth, (Date)current.get("birth")))
                update.put("birth", birth);

            Date death = deaths.get(id);
            if (death != null && differsByDay(death, (Date)current.get("death")))
                update.put("death", death);

            if (!update.isEmpty())
            {
                update.put("Id", id);
                updates.add(update);
            }
        }

        if (!updates.isEmpty())
            _log.info("Demographics birth/death out of sync with event records for {} animal(s); updating", updates.size());

        return updates;
    }

    /** Most recent public event date per animal for a demographic event dataset, in a single query. */
    private Map<String, Date> getPublicEventDates(String queryName, Set<String> ids)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), ids, CompareType.IN);
        filter.addCondition(FieldKey.fromString("qcstate/publicdata"), true);

        Map<String, Date> ret = new HashMap<>();
        new TableSelector(getTableInfo("study", queryName), PageFlowUtil.set("Id", "date"), filter, null)
                .forEachMap(row -> {
                    String id = (String)row.get("Id");
                    Date date = ConvertHelper.convert(row.get("date"), Date.class);
                    // birth and deaths are demographic datasets (one row per animal), but tolerate duplicates from a
                    // legacy load by keeping the latest rather than picking arbitrarily.
                    if (date != null && (ret.get(id) == null || date.after(ret.get(id))))
                        ret.put(id, date);
                });

        return ret;
    }

    /**
     * Compares to day precision. Event dates are entered with the time stripped, but values that arrived by ETL or
     * predate that behavior can carry a time component; treating those as drift would rewrite the whole colony on the
     * first save.
     */
    private boolean differsByDay(Date a, Date b)
    {
        if (a == null || b == null)
            return !(a == null && b == null);

        return !DateUtils.isSameDay(a, b);
    }

    public boolean upsertWeightRecord(Map<String, Object> row) throws QueryUpdateServiceException, DuplicateKeyException, SQLException, BatchValidationException, InvalidKeyException
    {
        return upsertWeightRecord(row, true);
    }

    /**
     * When announceChanges is false, the nested weight trigger will not announce the modified id
     * (skipAnnounceChangedParticipants). Callers must mark study.weight as modified on the outer helper
     * (addTableModified) so the single announcement at trigger completion covers it.
     *
     * @return whether a weight record was written; false when there was nothing to record
     */
    public boolean upsertWeightRecord(Map<String, Object> row, boolean announceChanges) throws QueryUpdateServiceException, DuplicateKeyException, SQLException, BatchValidationException, InvalidKeyException
    {
        BatchValidationException errors = new BatchValidationException();
        Date date = ConvertHelper.convert(row.get("date"), Date.class);
        String taskId = ConvertHelper.convert(row.get("taskid"), String.class);

        TableInfo ti = getTableInfo("study", "weight");

        // If there is already a weight record for this task, update that record. A null taskid filter flips to
        // "taskid IS NULL" and would match unrelated historical weights, so task-less entry (e.g. a non-EHR bulk
        // import form) is insert-only.
        Map<String, Object> existingRecord = null;
        if (taskId != null)
        {
            SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), row.get("Id"));
            filter.addCondition(FieldKey.fromString("taskid"), taskId);
            TableSelector ts = new TableSelector(ti, PageFlowUtil.set("lsid", "objectid"), filter, null);
            existingRecord = ts.getMap();
        }

        Double weight = null;
        if (row.get("weight") != null)
        {
            weight = ConvertHelper.convert(row.get("weight"), Double.class);
        }

        Map<String, Object> context = getExtraContext();
        if (!announceChanges)
            context.put("skipAnnounceChangedParticipants", true);

        // Weight is optional, so with none entered there is nothing to record. Delete any record left by an earlier
        // save rather than blanking it: the weight trigger only WARNs on a null weight and the default threshold
        // filters that out, so the emptied record would survive the save.
        if (weight == null)
        {
            if (existingRecord == null)
                return false;

            Map<String, Object> keyRow = new CaseInsensitiveHashMap<>();
            keyRow.put("lsid", existingRecord.get("lsid"));
            ti.getUpdateService().deleteRows(_user, _container, List.of(keyRow), null, context);

            return true;
        }

        Map<String, Object> saveRow = new CaseInsensitiveHashMap<>();
        saveRow.put("Id", row.get("Id"));
        saveRow.put("date", date);
        saveRow.put("taskid", taskId);
        saveRow.put("qcstate", row.get("qcstate"));
        saveRow.put("performedby", row.get("performedby"));
        saveRow.put("objectid", existingRecord != null ? existingRecord.get("objectid") : new GUID().toString());
        saveRow.put("weight", weight);

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(saveRow);

        if (existingRecord != null)
        {
            ti.getUpdateService().updateRows(_user, _container, rows, null, null, context);
        }
        else
        {
            ti.getUpdateService().insertRows(_user, _container, rows, errors, null, context);
        }

        if (errors.hasErrors())
            throw errors;

        return true;
    }

    public void clinicalMoveNotification(final String animalId, final String date)
    {
        //check whether Death Notification is enabled
        if (!NotificationService.get().isActive(new NBRIClinicalMoveNotification(), _container) || !NotificationService.get().isServiceEnabled())
        {
            _log.info("NBRI Clinical Move notification service is not enabled, will not send clinical move notification.");
            return;
        }

        try (DbScope.Transaction transaction = StudyService.get().getDatasetSchema().getScope().ensureTransaction())
        {
            // Add a post commit task to run provider update in another thread once this transaction is complete.
            transaction.addCommitTask(() ->
                    JobRunner.getDefault().execute(() -> {
                        final Container container = _container;
                        final User user = _user;
                        NBRIClinicalMoveNotification notification = new NBRIClinicalMoveNotification();
                        String subject = "Clinical Move Notification: " + animalId;

                        // get recipients
                        Set<UserPrincipal> recipients = NotificationService.get().getRecipients(notification, container);
                        if (recipients.isEmpty())
                        {
                            _log.warn("No NBRI recipients set, skipping clinical move notification");
                            return;
                        }

                        String remark = (String) EHRDemographicsService.get().getAnimal(container, animalId).getActiveHousing().getFirst().get("remark");
                        String performedBy = (String) EHRDemographicsService.get().getAnimal(container, animalId).getActiveHousing().getFirst().get("performedBy");

                        //construct html for email notification
                        final StringBuilder html = new StringBuilder();
                        html.append("Animal ").append(PageFlowUtil.filter(animalId)).append(" has been moved for Veterinary Treatment on ").append(date).append(".<br>");
                        html.append("Performed By: ").append(PageFlowUtil.filter(performedBy)).append("<br>");
                        html.append("Remark: ").append(PageFlowUtil.filter(remark)).append("<br><br>");

                        //append animal details
                        appendAnimalDetails(html, animalId, container);

                        // send Clinical Move Notification
                        _log.debug("NBRI Clinical Move notification job sending email for animal {} in container {}", animalId, container.getPath());
                        TriggerScriptNotification.sendMessage(subject, html.toString(), recipients, container, user);
                    }), DbScope.CommitTaskOption.POSTCOMMIT);

            transaction.commit();
        }
    }

    public void sendDeathNotification(final String animalId)
    {
        //check whether Death Notification is enabled
        if (!NotificationService.get().isActive(new NBRIDeathNotification(), _container) || !NotificationService.get().isServiceEnabled())
        {
            _log.info("NBRI Death notification service is not enabled, will not send death notification.");
            return;
        }

        try (DbScope.Transaction transaction = StudyService.get().getDatasetSchema().getScope().ensureTransaction())
        {
            // Add a post commit task to run provider update in another thread once this transaction is complete.
            transaction.addCommitTask(() ->
                    JobRunner.getDefault().execute(() -> {
                        final Container container = _container;
                        final User user = _user;
                        String subject = "Death Notification: " + animalId;

                        // get recipients
                        Set<UserPrincipal> recipients = NotificationService.get().getRecipients(new NBRIDeathNotification(), container);
                        if (recipients.isEmpty())
                        {
                            _log.warn("No NBRI recipients set, skipping death notification");
                            return;
                        }

                        //get death info
                        TableInfo deaths = getTableInfo("study", "deathNotification");
                        TableSelector deathsTs = new TableSelector(deaths, PageFlowUtil.set("Id", "date", "performedBy"), new SimpleFilter(FieldKey.fromString("Id"), animalId), null);
                        final Mutable<Date> deathDate = new MutableObject<>();
                        final Mutable<String> performedBy = new MutableObject<>();
                        deathsTs.forEach(rs -> {
                            if (rs.getString("date") != null)
                            {
                                Date date = ConvertHelper.convert(rs.getString("date"), Date.class);
                                deathDate.setValue(date);
                                performedBy.setValue(rs.getString("performedBy"));
                            }
                        });

                        //construct html for email notification
                        final StringBuilder html = new StringBuilder();
                        if (deathDate.get() == null)
                        {
                            _log.error("NBRI death notification job found no death date for animal {} in container {}", animalId, _container.getPath());
                            html.append("Death date not found. Please contact system administrator.").append("<br>");
                            return;
                        }
                        html.append("Animal '").append(PageFlowUtil.filter(animalId)).append("' has been declared dead on '").append(_dateFormat.format(deathDate.get())).append("'.<br>");
                        html.append("Performed By: ").append(PageFlowUtil.filter(performedBy.get())).append("<br><br>");

                        //append animal details
                        appendAnimalDetails(html, animalId, container);

                        //append link to a new Necropsy Request
                        String url = new ActionURL("ehr", "dataEntryForm", container)
                                .addParameter("formType", NBRINecropsyBaseFormType.REQUEST).getURIString();
                        html.append("<a href='").append(PageFlowUtil.filter(url)).append("'>");

                        html.append("Click here to request a Necropsy</a><br>");

                        // send Death Notification
                        _log.debug("NBRI Death notification job sending email for animal {} in container {}", animalId, container.getPath());
                        TriggerScriptNotification.sendMessage(subject, html.toString(), recipients, container, user);
                    }), DbScope.CommitTaskOption.POSTCOMMIT);

            transaction.commit();
        }
    }

    /** True if a necropsy request for this animal exists on a task other than the given one. */
    public boolean necropsyRequestExists(String id, String taskid)
    {
        return existsOnOtherTask("necropsyRequest", id, taskid);
    }

    /** True if a necropsy for this animal exists on a task other than the given one. */
    public boolean necropsyExists(String id, String taskid)
    {
        return existsOnOtherTask("necropsy", id, taskid);
    }

    private boolean existsOnOtherTask(String queryName, String id, String taskid)
    {
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), id);
        if (taskid != null)
            filter.addCondition(FieldKey.fromString("taskid"), taskid, CompareType.NEQ_OR_NULL);

        return new TableSelector(getTableInfo("study", queryName), filter, null).exists();
    }

    public void upsertNecropsyAlias(String id, Object date, String accessionNo, String qcStateLabel, @Nullable String performedby) throws SQLException, BatchValidationException, QueryUpdateServiceException, DuplicateKeyException, InvalidKeyException
    {
        TableInfo ti = getTableInfo("study", "alias");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), id);
        filter.addCondition(FieldKey.fromString("category"), NECROPSY_ALIAS_CATEGORY);
        List<String> existing = new TableSelector(ti, PageFlowUtil.set("objectid"), filter, new Sort("date")).getArrayList(String.class);

        Map<String, Object> row = new CaseInsensitiveHashMap<>();
        row.put("Id", id);
        row.put("alias", accessionNo);
        row.put("category", NECROPSY_ALIAS_CATEGORY);
        row.put("date", ConvertHelper.convert(date, Date.class));
        row.put("QCStateLabel", qcStateLabel);
        row.put("performedby", performedby != null ? performedby : String.valueOf(_user.getUserId()));
        row.put("objectid", existing.isEmpty() ? new GUID().toString() : existing.getFirst());

        QueryUpdateService qus = Objects.requireNonNull(ti.getUpdateService(), "study.alias is not updatable");
        BatchValidationException errors = new BatchValidationException();
        if (existing.isEmpty())
            qus.insertRows(_user, _container, List.of(row), errors, null, getExtraContext());
        else
            qus.updateRows(_user, _container, List.of(row), null, errors, null, getExtraContext());

        if (errors.hasErrors())
            throw errors;
    }

    /** Removes the necropsy rows of a deleted necropsy task, including any a draft later stage left behind. */
    public void deleteNecropsyData(String taskid) throws QueryUpdateServiceException, SQLException, BatchValidationException, InvalidKeyException
    {
        if (taskid == null)
            return;

        for (String queryName : List.of("necropsyRequest", "necropsy", "procedureStaff", "grossPathology", "tissueMeasurements", "tissueDisposition", "preliminaryDiagnoses", "histology", "finalDiagnoses", "necropsyFinal"))
        {
            TableInfo ti = getTableInfo("study", queryName);
            QueryUpdateService qus = ti.getUpdateService();
            if (qus == null)
                continue;

            List<Map<String, Object>> keys = List.of(new TableSelector(ti, PageFlowUtil.set("lsid"), new SimpleFilter(FieldKey.fromString("taskid"), taskid), null).getMapArray());
            if (!keys.isEmpty())
                qus.deleteRows(_user, _container, keys, null, getExtraContext());
        }
    }

    /**
     * Emails the notification for the necropsy status just reached, once the save commits. The preliminary and final
     * reports carry the printable report as a PDF when the PDF service is available.
     */
    public void sendNecropsyNotification(final String taskid, final String objectid, final String status)
    {
        final String formType;
        final AbstractEHRNotification notification;
        switch (status == null ? "" : status)
        {
            case NBRI_EHRManager.NECROPSY_REQUEST_PENDING ->
            {
                formType = NBRINecropsyBaseFormType.REQUEST;
                notification = new NBRINecropsyRequestNotification();
            }
            case NBRI_EHRManager.NECROPSY_PRELIMINARY_SUBMITTED ->
            {
                formType = NBRINecropsyBaseFormType.PRELIMINARY;
                notification = new NBRINecropsyPreliminaryNotification();
            }
            case NBRI_EHRManager.NECROPSY_FINAL_SUBMITTED, NBRI_EHRManager.NECROPSY_FINAL_SUBMITTED_PENDING ->
            {
                formType = NBRINecropsyBaseFormType.FINAL;
                notification = new NBRINecropsyFinalNotification();
            }
            default ->
            {
                return;
            }
        }
        if (taskid == null)
            return;

        if (!NotificationService.get().isServiceEnabled() || !NotificationService.get().isActive(notification, _container))
        {
            _log.info("{} is not enabled, will not send it.", notification.getName());
            return;
        }

        try (DbScope.Transaction transaction = StudyService.get().getDatasetSchema().getScope().ensureTransaction())
        {
            transaction.addCommitTask(() ->
                    JobRunner.getDefault().execute(() -> {
                        Set<UserPrincipal> recipients = NotificationService.get().getRecipients(notification, _container);
                        if (recipients.isEmpty())
                        {
                            _log.warn("No recipients set, skipping {}", notification.getName());
                            return;
                        }

                        Map<String, Object> request = getNecropsyRequest(taskid);
                        if (request == null)
                        {
                            _log.warn("{} not sent: no necropsy request for task {}", notification.getName(), taskid);
                            return;
                        }

                        String id = (String) request.get("Id");
                        StringBuilder html = new StringBuilder();
                        html.append("Status: ").append(PageFlowUtil.filter(status)).append("<br>");
                        appendNecropsyRequestDetails(html, request);
                        appendAnimalDetails(html, id, _container);
                        appendNecropsyLinks(html, taskid, formType);

                        File pdf = NBRINecropsyBaseFormType.REQUEST.equals(formType) ? null : renderNecropsyReport(objectid);
                        try
                        {
                            TriggerScriptNotification.sendMessage(notification.getEmailSubject(_container) + ": " + id, html.toString(), recipients, _container, _user, pdf);
                        }
                        finally
                        {
                            if (pdf != null)
                                FileUtil.deleteDir(pdf.getParentFile());
                        }
                    }), DbScope.CommitTaskOption.POSTCOMMIT);

            transaction.commit();
        }
    }

    /** The task's request details, plus the animal from its necropsy request. */
    private @Nullable Map<String, Object> getNecropsyRequest(String taskid)
    {
        SimpleFilter taskFilter = new SimpleFilter(FieldKey.fromString("taskid"), taskid);
        String[] ids = new TableSelector(getTableInfo("study", "necropsyRequest"), PageFlowUtil.set("Id"), taskFilter, null).getArray(String.class);
        if (ids.length == 0)
            return null;

        TableInfo ti = getTableInfo("nbri_ehr", "necropsyTask");
        List<FieldKey> keys = List.of(
                FieldKey.fromString("Date"),
                FieldKey.fromString("SubmittingPI"),
                FieldKey.fromString("SubmittingVet/DisplayName"),
                FieldKey.fromString("PriorityCode"),
                FieldKey.fromString("Sensitive"),
                FieldKey.fromString("SensitiveReason"));
        final Map<FieldKey, ColumnInfo> cols = QueryService.get().getColumns(ti, keys);

        final Map<String, Object> request = new CaseInsensitiveHashMap<>();
        request.put("Id", ids[0]);
        new TableSelector(ti, cols.values(), taskFilter, null).forEach(object -> {
            Results rs = new ResultsImpl(object, cols);
            for (FieldKey key : keys)
                request.put(key.toString(), rs.getObject(key));
        });

        return request;
    }

    private void appendNecropsyRequestDetails(StringBuilder html, Map<String, Object> request)
    {
        Date date = ConvertHelper.convert(request.get("Date"), Date.class);
        boolean sensitive = Boolean.TRUE.equals(ConvertHelper.convert(request.get("Sensitive"), Boolean.class));

        html.append("Request Date: ").append(date == null ? "" : PageFlowUtil.filter(_dateFormat.format(date))).append("<br>");
        html.append("Submitting PI: ").append(PageFlowUtil.filter(request.get("SubmittingPI"))).append("<br>");
        html.append("Submitting Vet: ").append(PageFlowUtil.filter(request.get("SubmittingVet/DisplayName"))).append("<br>");
        html.append("Priority: ").append(PageFlowUtil.filter(request.get("PriorityCode"))).append("<br>");
        html.append("Sensitive: ").append(sensitive ? "Yes" : "No").append("<br>");
        if (sensitive)
            html.append("Sensitive Reason: ").append(PageFlowUtil.filter(request.get("SensitiveReason"))).append("<br>");
        html.append("<br>");
    }

    private void appendNecropsyLinks(StringBuilder html, String taskid, String formType)
    {
        String reportUrl = new ActionURL("ehr", "dataEntryFormDetails", _container)
                .addParameter("formType", formType).addParameter("taskid", taskid).getURIString();
        html.append("<br><a href='").append(PageFlowUtil.filter(reportUrl)).append("'>Click here to view this necropsy</a><br>");

        String nextStage = switch (formType)
        {
            case NBRINecropsyBaseFormType.REQUEST -> NBRINecropsyBaseFormType.PRELIMINARY;
            case NBRINecropsyBaseFormType.PRELIMINARY -> NBRINecropsyBaseFormType.FINAL;
            default -> null;
        };
        if (nextStage != null)
        {
            String enterUrl = new ActionURL("ehr", "dataEntryForm", _container)
                    .addParameter("formType", nextStage).addParameter("taskid", taskid).getURIString();
            html.append("<a href='").append(PageFlowUtil.filter(enterUrl)).append("'>Click here to enter ")
                    .append(NBRINecropsyBaseFormType.PRELIMINARY.equals(nextStage) ? "preliminary" : "final").append(" necropsy results</a><br>");
        }

        String allUrl = new ActionURL("query", "executeQuery", _container)
                .addParameter("schemaName", "nbri_ehr").addParameter("query.queryName", "necropsyTask").getURIString();
        html.append("<a href='").append(PageFlowUtil.filter(allUrl)).append("'>Click here to view all necropsies</a><br>");
    }

    /** Renders the printable necropsy report to a PDF in its own temp directory, or returns null if it can't. */
    private @Nullable File renderNecropsyReport(String objectid)
    {
        PuppeteerService puppeteer = PuppeteerService.get();
        if (puppeteer == null || !puppeteer.isAvailable())
        {
            _log.warn("PDF service is not available, sending the necropsy report without its PDF");
            return null;
        }

        FileLike dir = null;
        try (SecurityManager.TransformSession session = SecurityManager.createTransformSession(_user))
        {
            dir = FileUtil.createTempDirectoryFileLike("necropsyReport");
            File pdf = dir.resolveChild("NecropsyReport.pdf").toNioPathForWrite().toFile();
            URL url = new URI(new ActionURL("nbri_ehr", "necropsyReport.view", _container).addParameter("objectIds", objectid).getURIString()).toURL();
            try (OutputStream out = new FileOutputStream(pdf))
            {
                puppeteer.streamPDF(_user, url, null, null, pdf.getName(), session.getApiKey(), () -> out);
            }
            return pdf;
        }
        catch (Exception e)
        {
            _log.error("Unable to render the necropsy report PDF for necropsy task {}", objectid, e);
            if (dir != null)
                FileUtil.deleteDir(dir);
            return null;
        }
    }

    private void appendAnimalDetails(StringBuilder html, String id, final Container container)
    {
        String url = AppProps.getInstance().getBaseServerUrl() + AppProps.getInstance().getContextPath() + "/ehr" + container.getPath() + "/participantView.view?participantId=" + id;

        String tdFieldStyle = "\"border: 1px solid #000000;padding:5px;background-color:lightgray\"";
        String tdValueStyle = "\"border: 1px solid #000000;padding:5px;\"";

        String tableStyle = "\"" +
                "        border-collapse: collapse;" +
                "        border: 1px solid #000000;\"";

        String cage;
        List<Map<String, Object>> activeHousing = EHRDemographicsService.get().getAnimal(container, id).getActiveHousing();
        if (null != activeHousing && !activeHousing.isEmpty())
        {
            cage = (String) activeHousing.getFirst().get("cage/cage");
        }
        else
        {
            cage = "Not Found";
        }

        html.append("<table style=").append(tableStyle).append(">");
        html.append("<tr><td style=").append(tdFieldStyle).append(">").append("Id").append("</td>").append("<td style=").append(tdValueStyle).append(">").append(PageFlowUtil.filter(id)).append("</td></tr>");
        html.append("<tr><td style=").append(tdFieldStyle).append(">").append("Location").append("</td>").append("<td style=").append(tdValueStyle).append(">").append(PageFlowUtil.filter(cage)).append("</td></tr>");
        html.append("<tr><td style=").append(tdFieldStyle).append(">").append("Project").append("</td>").append("<td style=").append(tdValueStyle).append(">").append(PageFlowUtil.filter(getProject(id))).append("</td></tr>");
        html.append("<tr><td style=").append(tdFieldStyle).append(">").append("Protocol").append("</td>").append("<td style=").append(tdValueStyle).append(">").append(PageFlowUtil.filter(getProtocol(id))).append("</td></tr>");
        html.append("</table>");
        html.append("<br>");
        html.append("<a href='").append(url).append("'>");
        html.append("Click here to view this animal's clinical details</a><br>");
    }

    private String getProject(String id)
    {
        TableInfo ti = getTableInfo("study", "notificationAnimalProject");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), id);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("project"), filter, null);
        final Mutable<String> project = new MutableObject<>();
        ts.forEach(rs -> project.setValue(rs.getString("project")));
        return project.get();
    }

    private String getProtocol(String id)
    {
        TableInfo ti = getTableInfo("study", "protocolAssignment");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), id);
        filter.addCondition(FieldKey.fromString("enddate"), null, CompareType.ISBLANK);

        Set<FieldKey> keys = new HashSet<>();
        keys.add(FieldKey.fromString("protocol/title"));
        keys.add(FieldKey.fromString("protocol/InvestigatorId/LastName"));
        final Map<FieldKey, ColumnInfo> cols = QueryService.get().getColumns(ti, keys);
        TableSelector ts = new TableSelector(ti, cols.values(), filter, null);

        final Mutable<String> protocol = new MutableObject<>();

        ts.forEach(object -> {
            Results rs = new ResultsImpl(object, cols);
            String title = rs.getString(FieldKey.fromString("protocol/title"));
            String inves = rs.getString(FieldKey.fromString("protocol/InvestigatorId/LastName"));
            if (title == null)
            {
                protocol.setValue("None");
            }
            else
            {
                protocol.setValue(title + (inves == null ? "" : " - " + inves));
            }
        });
        return protocol.get();
    }

    public String createAssignmentRecord(String dataset, String id, Map<String, Object> row) throws SQLException, BatchValidationException, QueryUpdateServiceException, InvalidKeyException, DuplicateKeyException
    {
        BatchValidationException errors = new BatchValidationException();
        Date date = ConvertHelper.convert(row.get("date"), Date.class);
        if (id == null || date == null)
            return "Attempting to create an assignment record with no id or date";

        TableInfo ti = getTableInfo("study", dataset);

        String taskId = ConvertHelper.convert(row.get("taskid"), String.class);
        if (taskId == null) {
            return "Attempting to create an assignment record with no taskid";
        }

        String qcstate = ConvertHelper.convert(row.get("qcstate"), String.class);
        if (qcstate == null) {
            return "Attempting to create an assignment record with no qcstate";
        }

        String performedby = ConvertHelper.convert(row.get("performedby"), String.class);
        if (performedby == null) {
            return "Attempting to create an assignment record with no performedby";
        }

        boolean updateRecord = false;

        // If there is already an assignment record of this kind for this task, update that record
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), id);
        filter.addCondition(FieldKey.fromString("taskid"), taskId);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("lsid", "objectid"), filter, null);
        if (ts.exists())
        {
            updateRecord = true;
        }

        Map<String, Object> saveRow = new CaseInsensitiveHashMap<>();
        saveRow.put("Id", id);
        saveRow.put("date", date);
        saveRow.put("taskId", taskId);
        saveRow.put("qcstate", qcstate);
        saveRow.put("performedby", performedby);
        if (updateRecord)
        {
            saveRow.put("objectid", ts.getMap().get("objectid"));
        }
        else
        {
            saveRow.put("objectid", new GUID().toString());
        }

        String project = ConvertHelper.convert(row.get("project"), String.class);
        if (project != null)
        {
            saveRow.put("project", project);
        }
        else if (dataset.equalsIgnoreCase("assignment"))
        {
            return "Attempting to create a project assignment record with no project";
        }

        String protocol = ConvertHelper.convert(row.get("protocol"), String.class);
        if (protocol != null)
        {
            saveRow.put("protocol", protocol);
        }
        else if (dataset.equalsIgnoreCase("protocolAssignment"))
        {
            return "Attempting to create a protocol assignment record with no protocol";
        }

        String groupId = ConvertHelper.convert(row.get("groupId"), String.class);
        if (groupId != null)
        {
            saveRow.put("groupId", groupId);
        }
        else if (dataset.equalsIgnoreCase("animal_group_members"))
        {
            return "Attempting to create a group assignment record with no group";
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(saveRow);

        if (updateRecord)
        {
            ti.getUpdateService().updateRows(_user, _container, rows, null, null, getExtraContext());
        }
        else
        {
            ti.getUpdateService().insertRows(_user, _container, rows, errors, null, getExtraContext());
        }

        if (errors.hasErrors())
            throw errors;

        return null;
    }

    public int getUserId (String displayName)
    {
        User u = UserManager.getUserByDisplayName(displayName);
        return null != u ? u.getUserId() : -1;
    }

    public long totalHousingRecords(String location)
    {
        TableInfo ti = getTableInfo("study", "housing");

        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("isActive"), true);
        filter.addCondition(FieldKey.fromString("cage"), location);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("Id"), filter, null);

        return ts.getRowCount();
    }

    public long totalRecords(String schemaName, String queryName, String columnName, String value)
    {
        TableInfo ti = getTableInfo(schemaName, queryName);

        SimpleFilter filter = new SimpleFilter(FieldKey.fromString(columnName), value);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set(columnName), filter, null);

        return ts.getRowCount();
    }

    // Rooms and cages are scoped to the trigger's own container, but housing lives in the EHR study folder, which is
    // often an ancestor. Read from _container instead and the occupancy check finds nothing and passes vacuously.
    private TableInfo getHousingTableInfo()
    {
        Container studyContainer = EHRService.get().getEHRStudyContainer(_container);
        if (studyContainer == null)
            throw new IllegalStateException("No EHR study container is configured for: " + _container.getPath());

        UserSchema us = QueryService.get().getUserSchema(_user, studyContainer, "study");
        if (us == null)
            throw new IllegalArgumentException("Unable to find schema: study");

        TableInfo ti = us.getTable("housing");
        if (ti == null)
            throw new IllegalArgumentException("Unable to find table: study.housing");

        return ti;
    }

    /** Message naming the animals whose housing outlasts a room's proposed disabled date, or null when it is clear. */
    public String animalsRemainingInRoom(String room, Object dateDisabled)
    {
        Date disabled = toDate(dateDisabled);
        if (room == null || disabled == null)
            return null;

        FieldKey enddate = FieldKey.fromString("enddate");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("room"), room);
        // A record still in data entry is not yet an occupancy, so it must not block the room.
        filter.addCondition(FieldKey.fromParts("QCState", "PublicData"), true);
        filter.addClause(new SimpleFilter.OrClause(
                CompareType.ISBLANK.createFilterClause(enddate, null),
                CompareType.DATE_GT.createFilterClause(enddate, disabled)));

        Set<String> ids = new TreeSet<>();
        for (String id : new TableSelector(getHousingTableInfo(), PageFlowUtil.set("Id"), filter, null).getArrayList(String.class))
        {
            if (id != null)
                ids.add(id);
        }

        if (ids.isEmpty())
            return null;

        List<String> named = ids.stream().limit(MAX_NAMED_ANIMALS).toList();
        StringBuilder message = new StringBuilder("Cannot disable this room: ")
                .append(ids.size())
                .append(ids.size() == 1 ? " animal is" : " animals are")
                .append(" still housed there: ")
                .append(String.join(", ", named));

        if (ids.size() > named.size())
            message.append(", and ").append(ids.size() - named.size()).append(" more");

        return message.append(".").toString();
    }

    /**
     * Applies a room's disabled date to its cages. A cage carrying a date of its own keeps it; only cages with no date,
     * or with the date this room last cascaded, follow the room.
     */
    public int cascadeCageDateDisabled(String room, Object newValue, Object priorValue) throws QueryUpdateServiceException, SQLException, BatchValidationException, InvalidKeyException
    {
        Date newDate = toDate(newValue);
        Date priorDate = toDate(priorValue);

        if (room == null || sameInstant(newDate, priorDate))
            return 0;

        FieldKey dateDisabled = FieldKey.fromString("dateDisabled");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("room"), room);

        if (newDate == null)
        {
            // Clearing a room only undoes the cascade it previously applied.
            if (priorDate == null)
                return 0;

            filter.addCondition(dateDisabled, priorDate, CompareType.DATE_EQUAL);
        }
        else if (priorDate == null)
        {
            filter.addCondition(dateDisabled, null, CompareType.ISBLANK);
        }
        else
        {
            filter.addClause(new SimpleFilter.OrClause(
                    CompareType.ISBLANK.createFilterClause(dateDisabled, null),
                    CompareType.DATE_EQUAL.createFilterClause(dateDisabled, priorDate)));
        }

        TableInfo ti = getTableInfo("ehr_lookups", "cage");
        List<String> locations = new TableSelector(ti, PageFlowUtil.set("location"), filter, null).getArrayList(String.class);
        if (locations.isEmpty())
            return 0;

        List<Map<String, Object>> rows = new ArrayList<>();
        List<Map<String, Object>> keys = new ArrayList<>();
        for (String location : locations)
        {
            Map<String, Object> row = new CaseInsensitiveHashMap<>();
            row.put("location", location);
            row.put("dateDisabled", newDate);
            rows.add(row);

            Map<String, Object> key = new CaseInsensitiveHashMap<>();
            key.put("location", location);
            keys.add(key);
        }

        BatchValidationException errors = new BatchValidationException();
        ti.getUpdateService().updateRows(_user, _container, rows, keys, errors, null, getExtraContext());

        if (errors.hasErrors())
            throw errors;

        return rows.size();
    }

    /** A date of today is not in the future, whatever its time. */
    public boolean isAfterToday(Object value)
    {
        Date date = toDate(value);

        return date != null && DateUtils.truncate(date, Calendar.DATE).after(DateUtils.truncate(new Date(), Calendar.DATE));
    }

    // An absent field reaches a trigger helper as Rhino's Undefined, which ConvertHelper stringifies to "undefined"
    // and then fails to parse. Only null and a real value get past here.
    private Date toDate(Object value)
    {
        if (value == null || "undefined".equals(value.toString()))
            return null;

        return ConvertHelper.convert(value, Date.class);
    }

    // Timestamp.equals(Date) is asymmetric, so the same instant read from a row map and from the database can compare
    // unequal.
    private boolean sameInstant(Date a, Date b)
    {
        if (a == null || b == null)
            return a == b;

        return a.getTime() == b.getTime();
    }

    // A birth or pregnancy outcome row announces its own animal, which is the offspring rather than the dam, so the
    // dam has to be read back off the conception the row claims
    public String getConceptionDam(String conceptId)
    {
        if (conceptId == null)
            return null;

        TableInfo ti = getTableInfo("study", "conception");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("conceptId"), conceptId);

        return new TableSelector(ti, Collections.singleton("Id"), filter, null).getObject(String.class);
    }

    public boolean canCloseCase()
    {
        return _container.hasPermission(_user, EHRVeterinarianPermission.class);
    }

    public void closeDailyClinicalObs(String caseid, String enddate)
    {
        TableInfo ti = getTableInfo("study", "observation_order");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("caseid"), caseid);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("objectid"), filter, null);

        Map<String, Object>[] orders = ts.getMapArray();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> order : orders)
        {

            Map<String, Object> row = new CaseInsensitiveHashMap<>();
            row.put("objectid", order.get("objectid"));
            row.put("enddate", enddate);
            rows.add(row);
        }
        try
        {
            ti.getUpdateService().updateRows(_user, _container, rows, null, null, getExtraContext());
        }
        catch (Exception e)
        {
            _log.error("Error closing daily clinical observation order", e);
        }
    }

    public void ensureDailyClinicalObservationOrders(String id, String caseid, final Date date, String performedby, String qcstate, String taskid, List<Map<String, Object>> ordersInTransaction)
    {
        TableInfo ti = getTableInfo("study", "observation_order");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromParts("category","value"), "Activity");
        filter.addCondition(FieldKey.fromString("caseid"), caseid);
        filter.addCondition(FieldKey.fromParts("frequency"), "SID");
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("category","frequency"), filter, null);

        List<String> missing = new ArrayList<>(NBRI_EHRManager.DAILY_CLINICAL_OBS);
        ts.forEach(row -> {
            if (row.getString("category") != null)
                missing.remove(row.getString("category"));
        });

        ordersInTransaction.forEach(row -> {
            if (row.get("category") != null && row.get("frequency") != null && row.get("frequency").equals("SID"))
                missing.remove((String)row.get("category"));
        });

        if (!missing.isEmpty())
        {
            try
            {
                List<Map<String, Object>> rows = new ArrayList<>();

                // Get tomorrow's date at 8:00 AM
                LocalDateTime localDateTime = date.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                LocalDateTime nextDayAtEight = localDateTime.plusDays(1)
                        .withHour(8)
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);

                Date obsDate = Date.from(nextDayAtEight.atZone(ZoneId.systemDefault()).toInstant());

                for (String category : missing)
                {
                    Map<String, Object> row = new CaseInsensitiveHashMap<>();
                    row.put("category", category);
                    row.put("frequency", "SID");
                    row.put("caseid", caseid);
                    row.put("date", obsDate);
                    row.put("Id", id);
                    row.put("qcstate", qcstate);
                    row.put("area", "N/A");
                    row.put("performedby", performedby);
                    row.put("taskid", taskid);
                    row.put("type", "Clinical"); // TODO: Will need to update for behavior
                    rows.add(row);
                }

                BatchValidationException errors = new BatchValidationException();
                ti.getUpdateService().insertRows(_user, _container, rows, errors, null, getExtraContext());
                if (errors.hasErrors())
                    throw errors;
            }
            catch (Exception e)
            {
                _log.error("Error adding daily clinical observation orders", e);
            }
        }
    }

    /**
     * Returns the category of an observation type from ehr.observation_types, or null when the type has no
     * category or is not found. Cached for the life of the save batch.
     */
    public String getObservationTypeCategory(String observationType)
    {
        if (observationType == null)
            return null;

        if (!_cachedObservationTypeCategories.containsKey(observationType))
        {
            TableInfo ti = getTableInfo("ehr", "observation_types");
            SimpleFilter filter = new SimpleFilter(FieldKey.fromString("value"), observationType);
            List<String> categories = new TableSelector(ti, Collections.singleton("category"), filter, null).getArrayList(String.class);
            _cachedObservationTypeCategories.put(observationType, categories.isEmpty() ? null : categories.get(0));
        }

        return _cachedObservationTypeCategories.get(observationType);
    }

    // This helper function propagates clinical observations through clinical cases
    public Map<String, Object> handleScheduledObservations(Map<String, Object> row, String qcstate, String orderTasks) throws SQLException, BatchValidationException, QueryUpdateServiceException, DuplicateKeyException
    {
        Date scheduledDate = ConvertHelper.convert(row.get("scheduledDate"), Date.class);
        Date date = ConvertHelper.convert(row.get("date"), Date.class);
        String category = ConvertHelper.convert(row.get("category"), String.class);
        String observation = ConvertHelper.convert(row.get("observation"), String.class);
        String performedBy = ConvertHelper.convert(row.get("performedBy"), String.class);
        String taskid = ConvertHelper.convert(row.get("taskid"), String.class);
        String id = ConvertHelper.convert(row.get("Id"), String.class);

        // Get this animal's observation orders for these tasks. An order taskid is shared by every animal
        // ordered in the same session, so without the animal filter the entry spills onto those animals too.
        TableInfo ti = getTableInfo("study", "observationOrdersByDate");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("taskid"), orderTasks, CompareType.IN);
        filter.addCondition(FieldKey.fromString("category"), category);
        filter.addCondition(FieldKey.fromString("date"), scheduledDate);
        filter.addCondition(FieldKey.fromString("animalId"), id);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("category","caseId","animalId","area","objectid","type","taskid"), filter, null);
        ts.setNamedParameters(Map.of("StartDate", scheduledDate, "NumDays", "1"));

        Map<String, Object>[] orders = ts.getMapArray();
        Map<String, Object> triggerOrder = null;

        for (int i = 0; i < orders.length; i++)
        {
            Map<String, Object> order = orders[i];

            // Group every entry by the originating order's taskid into a new task per group.
            String orderTaskId = ConvertHelper.convert(order.get("taskid"), String.class);
            String groupTaskId = resolveGroupTaskId(orderTaskId, taskid, qcstate);

            // First order we find will fill out the information in the row passing through the trigger
            if (i == 0)
            {
                triggerOrder = new HashMap<>();
                triggerOrder.put("caseId", order.get("caseId"));
                triggerOrder.put("area", order.get("area"));
                triggerOrder.put("orderId", order.get("objectid"));
                triggerOrder.put("type", order.get("type"));
                triggerOrder.put("taskId", groupTaskId);
                continue;
            }

            // If there are multiple treatment orders that match insert the others here
            Map<String, Object> obsRow = new CaseInsensitiveHashMap<>();
            obsRow.put("caseId", order.get("caseId"));
            obsRow.put("category", order.get("category"));
            obsRow.put("date", date);
            obsRow.put("qcstate", qcstate);
            obsRow.put("Id", order.get("animalId"));
            obsRow.put("scheduledDate", scheduledDate);
            obsRow.put("area", order.get("area"));
            obsRow.put("observation", observation);
            obsRow.put("performedBy", performedBy);
            obsRow.put("orderId", order.get("objectid"));
            obsRow.put("type", order.get("type"));
            obsRow.put("taskid", groupTaskId);

            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(obsRow);

            BatchValidationException errors = new BatchValidationException();
            TableInfo obsTi = getTableInfo("study", "clinical_observations");
            obsTi.getUpdateService().insertRows(_user, _container, rows, errors, null, getExtraContext());
            if (errors.hasErrors())
                throw errors;
        }

        return triggerOrder;
    }

    /**
     * Resets the per-batch grouping map. Called from the clinical_observations onInit trigger so no
     * grouping state can leak between save batches.
     */
    public void clearScheduledObsTaskMap()
    {
        _scheduledObsTaskMap.clear();
    }

    /**
     * Resolves the task that a scheduled observation entry should be grouped under, keyed by the
     * originating observation order's taskid. The first distinct order taskid seen in a save batch
     * reuses the form's own task ({@code formTaskId}); each subsequent distinct order taskid gets a
     * freshly created task that clones the form task. This groups all observations that came from the
     * same order under one task, with a new task per additional group, while reusing the form's task
     * for the first group so it is not left empty.
     */
    private String resolveGroupTaskId(String orderTaskId, String formTaskId, String qcstate) throws SQLException, BatchValidationException, QueryUpdateServiceException, DuplicateKeyException
    {
        // Defensive: an order with no taskid can't be grouped, so fall back to the form's task.
        if (orderTaskId == null)
            return formTaskId;

        if (_scheduledObsTaskMap.containsKey(orderTaskId))
            return _scheduledObsTaskMap.get(orderTaskId);

        // Reuse the form's already-created task for the first group; create new tasks for the rest.
        String groupTaskId = _scheduledObsTaskMap.isEmpty() ? formTaskId : createTaskFromForm(formTaskId, qcstate);
        _scheduledObsTaskMap.put(orderTaskId, groupTaskId);
        return groupTaskId;
    }

    /**
     * Creates a new ehr.tasks record for a group of scheduled observations, cloning the form's task
     * ({@code formTaskId}) so the new task carries the same title/form/category/etc. Returns the new taskid.
     */
    private String createTaskFromForm(String formTaskId, String qcstate) throws SQLException, BatchValidationException, QueryUpdateServiceException, DuplicateKeyException
    {
        String newTaskId = new GUID().toString();
        TableInfo tasksTi = getTableInfo("ehr", "tasks");

        Map<String, Object> formTask = null;
        if (formTaskId != null)
        {
            SimpleFilter filter = new SimpleFilter(FieldKey.fromString("taskid"), formTaskId);
            formTask = new TableSelector(tasksTi, PageFlowUtil.set("title", "formtype", "category", "qcstate", "assignedto", "duedate", "caseid", "description"), filter, null).getMap();
        }

        Map<String, Object> taskRow = new CaseInsensitiveHashMap<>();
        taskRow.put("taskid", newTaskId);
        if (formTask != null)
        {
            taskRow.put("title", formTask.get("title"));
            taskRow.put("formtype", formTask.get("formtype"));
            taskRow.put("category", formTask.get("category"));
            taskRow.put("qcstate", formTask.get("qcstate"));
            taskRow.put("assignedto", formTask.get("assignedto"));
            taskRow.put("duedate", formTask.get("duedate"));
            taskRow.put("caseid", formTask.get("caseid"));
            taskRow.put("description", formTask.get("description"));
        }
        else
        {
            // Fallback if the form task is not visible yet: populate the required non-null columns.
            taskRow.put("title", "Clinical Observations");
            taskRow.put("category", "task");
            taskRow.put("formtype", NBRIClinicalObservationsFormType.NAME);
            taskRow.put("qcstate", qcstate);
            taskRow.put("assignedto", _user.getUserId());
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(taskRow);

        BatchValidationException errors = new BatchValidationException();
        tasksTi.getUpdateService().insertRows(_user, _container, rows, errors, null, getExtraContext());
        if (errors.hasErrors())
            throw errors;

        return newTaskId;
    }

    public boolean validateHousing(String id, String cage, Date date)
    {
        if (id == null || cage == null || date == null)
            return true;

        TableInfo ti = getTableInfo("study", "Housing");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), id);
        filter.addCondition(FieldKey.fromString("cage"), cage, CompareType.EQUAL);

        Date updatedDate = ConvertHelper.convert(date, Date.class);
        updatedDate = DateUtils.addMinutes(updatedDate, 1);  // temp fix
        filter.addCondition(FieldKey.fromString("date"), updatedDate, CompareType.LTE);
        filter.addClause(new SimpleFilter.OrClause(new CompareType.EqualsCompareClause(FieldKey.fromString("enddate"), CompareType.GT, date), new CompareType.CompareClause(FieldKey.fromString("enddate"), CompareType.ISBLANK, null)));
        filter.addCondition(FieldKey.fromString("qcstate/publicdata"), true, CompareType.EQUAL);

        TableSelector ts = new TableSelector(ti, Collections.singleton("Id"), filter, null);
        return ts.exists();
    }

    public void reportDataChange(String schema, String query, final List<String> ids)
    {
        EHRDemographicsService.get().reportDataChange(_container, schema, query, ids);
    }

    public Object getFormularyForDrug(String drugCode) throws SQLException
    {
        if (_cachedDrugFormulary.containsKey(drugCode))
            return _cachedDrugFormulary.get(drugCode);

        TableInfo ti = getTableInfo("ehr_lookups", "drug_defaults");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("code"), drugCode);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("code", "amount_max"), filter, null);
        Map<String, Object> drugFormulary = new HashMap<>();
        try (Results rs = ts.getResults())
        {
            for (Map<String, Object> r : rs)
            {
                drugFormulary.put("code", r.get("code"));
                drugFormulary.put("maxAmount", r.get("amount_max"));
            }
        }

        _cachedDrugFormulary.put(drugCode, drugFormulary);
        return drugFormulary;
    }

    public boolean isTreatmentOrderEntered(String treatmentid, String date)
    {
        TableInfo ti = getTableInfo("study", "drug");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("treatmentid"), treatmentid);
        filter.addCondition(FieldKey.fromString("scheduledDate"), ConvertHelper.convert(date, Date.class), CompareType.EQUAL);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("objectid"), filter, null);

        return ts.exists();
    }

    public boolean isProcedureOrderEntered(String orderid)
    {
        TableInfo ti = getTableInfo("study", "prc_order");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("objectid"), orderid);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("qcstate"), filter, null);
        List<Integer> qcstates = ts.getArrayList(Integer.class);
        if (qcstates.isEmpty() || qcstates.getFirst() == null)
            return false;

        return EHRService.get().getQCStates(_container).get("Completed").getRowId() == qcstates.getFirst();
    }

    public void markProcedureOrderComplete(List<String> orderids)
    {
        TableInfo ti = getTableInfo("study", "prc_order");
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String orderid : orderids)
        {
            Map<String, Object> r = new HashMap<>();
            r.put("objectid", orderid);
            r.put("qcstate", EHRService.get().getQCStates(_container).get("Completed").getRowId());
            rows.add(r);
        }

        try
        {
            ti.getUpdateService().updateRows(_user, _container, rows, null, null, getExtraContext());
        }
        catch (Exception e)
        {
            _log.error("Error marking procedure order complete", e);
        }
    }

    public void updateProcedureOrdersToCompleted(List<String> ids)
    {
        if (ids == null || ids.size() < 1) // Native array doesn't support isEmpty
        {
            _log.warn("No IDs provided to updateProcedureOrdersToCompleted");
            return;
        }

        TableInfo ti = getTableInfo("study", "prc_order");

        // Get the QC state IDs for "Request: Approved" and "Completed"
        Integer approvedQcStateId = EHRService.get().getQCStates(_container).get(EHRService.QCSTATES.RequestApproved.getLabel()).getRowId();
        Integer completedQcStateId = EHRService.get().getQCStates(_container).get(EHRService.QCSTATES.Completed.getLabel()).getRowId();

        // Query for rows matching the IDs and having "Request: Approved" status
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), ids, CompareType.IN);
        filter.addCondition(FieldKey.fromString("qcstate"), approvedQcStateId, CompareType.EQUAL);

        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("objectid"), filter, null);
        Map<String, Object>[] results = ts.getMapArray();

        if (results.length == 0)
        {
            _log.info("No Procedure Orders found with 'Request: Approved' status for the provided IDs");
            return;
        }

        // Build the update rows
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map<String, Object> result : results)
        {
            Map<String, Object> row = new HashMap<>();
            row.put("objectid", result.get("objectid"));
            row.put("qcstate", completedQcStateId);
            rows.add(row);
        }

        try
        {
            ti.getUpdateService().updateRows(_user, _container, rows, null, null, getExtraContext());
            _log.info("Successfully updated {} prc_order rows to 'Completed' status", rows.size());
        }
        catch (Exception e)
        {
            _log.error("Error updating prc_order rows to completed", e);
        }
    }

    public void sendPregnancyOutcomeNotification(final String animalId, Map<String, Object> row) throws Exception
    {
        //check whether Notification is enabled
        if (!NotificationService.get().isActive(new NBRIPregnancyOutcomeNotification(), _container) || !NotificationService.get().isServiceEnabled())
        {
            _log.info("NBRI Pregnancy Outcome notification service is not enabled, will not send notification.");
            return;
        }

        try (DbScope.Transaction transaction = Objects.requireNonNull(StudyService.get()).getDatasetSchema().getScope().ensureTransaction())
        {
            // Add a post commit task to run provider update in another thread once this transaction is complete.
            transaction.addCommitTask(() ->
                    JobRunner.getDefault().execute(() -> {
                        final Container container = _container;
                        final User user = _user;
                        String subject = "Pregnancy Outcome Notification for: " + animalId;

                        // get recipients
                        Set<UserPrincipal> recipients = NotificationService.get().getRecipients(new NBRIPregnancyOutcomeNotification(), container);
                        if (recipients.isEmpty())
                        {
                            _log.warn("No NBRI recipients set for pregnancy outcome notification, skipping notification");
                            return;
                        }
                        //get pregnancy outcome info
                        Date date = ConvertHelper.convert(row.get("date"), Date.class);
                        String result = ConvertHelper.convert(row.get("result"), String.class);
                        String outcome;
                        try
                        {
                            outcome = getPregnancyResultTitle(result);
                        }
                        catch (SQLException e)
                        {
                            throw new RuntimeException("Unable to find the outcome for value '" + result + "'", e);
                        }

                        //construct html for email notification
                        final StringBuilder html = new StringBuilder();
                        html.append("Pregnancy outcome for animal '").append(PageFlowUtil.filter(animalId)).
                                append("' recorded on '").append(_dateFormat.format(date)).append("': ").
                                append(PageFlowUtil.filter(outcome)).append("<br><br>");

                        //append animal details
                        appendAnimalDetails(html, animalId, container);

                        // send Pregnancy Outcome notification
                        _log.debug("NBRI Pregnancy Outcome notification job sending email for animal {} in container {}", animalId, container.getPath());
                        TriggerScriptNotification.sendMessage(subject, html.toString(), recipients, container, user);
                    }), DbScope.CommitTaskOption.POSTCOMMIT);

            transaction.commit();
        }
    }

    public String getPregnancyResultTitle(String val) throws SQLException
    {
        TableInfo ti = getTableInfo("ehr_lookups", "pregnancy_result");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("value"), val);
        TableSelector ts = new TableSelector(ti, PageFlowUtil.set("value", "title"), filter, null);
        String title = null;
        try (Results rs = ts.getResults())
        {
            for (Map<String, Object> r : rs)
            {
                String value = ConvertHelper.convert(r.get("value"), String.class);

                if (null != value && value.equals(val))
                {
                    title = ConvertHelper.convert(r.get("title"), String.class);
                }
            }
        }
        return title;
    }
}
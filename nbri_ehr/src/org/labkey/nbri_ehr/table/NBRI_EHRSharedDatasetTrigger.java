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
package org.labkey.nbri_ehr.table;

import org.apache.commons.beanutils.ConversionException;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.collections.CaseInsensitiveHashSet;
import org.labkey.api.data.Container;
import org.labkey.api.data.ConvertHelper;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.data.triggers.Trigger;
import org.labkey.api.qc.DataState;
import org.labkey.api.qc.QCStateManager;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;
import org.labkey.nbri_ehr.dataentry.form.NBRIBehavioralCasesFormType;
import org.labkey.nbri_ehr.dataentry.form.NBRICasesFormType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Shared dataset trigger to add triggers to act on all the study datasets.
 */
public class NBRI_EHRSharedDatasetTrigger implements Trigger
{
    private void transformAnimalIdToUpperCase(Map<String, Object> row)
    {
        if (row != null && row.containsKey("Id") && row.get("Id") != null)
        {
            row.put("Id", ((String) row.get("Id")).toUpperCase());
        }
    }

    private void verifyPerformedBy(TableInfo table, @Nullable Map<String, Object> newRow, ValidationException errors)
    {
        if (newRow != null && newRow.containsKey("performedby") && newRow.get("performedby") == null)
        {
            if (newRow.containsKey("QCStateLabel") && "Completed".equals(newRow.get("QCStateLabel")))
            {
                errors.addFieldError("performedby", "Performed By must be entered in all records before submitting final. Table: " + table.getTitle());
            }
        }
    }

    @Override
    public void beforeInsert(TableInfo table, Container c, User user, @Nullable QueryUpdateService.InsertOption insertOption, @Nullable Map<String, Object> newRow, ValidationException errors, Map<String, Object> extraContext)
    {
        transformAnimalIdToUpperCase(newRow);
        verifyPerformedBy(table, newRow, errors);
    }

    @Override
    public void beforeUpdate(TableInfo table, Container c,
                             User user, @Nullable QueryUpdateService.InsertOption insertOption, @Nullable Map<String, Object> newRow, @Nullable Map<String, Object> oldRow,
                             ValidationException errors, Map<String, Object> extraContext)
    {
        verifyPerformedBy(table, newRow, errors);
        guardCaseTask(table, c, user, newRow, oldRow, errors, extraContext);
    }

    // The EHR script trigger rewrites vetreview and vetreviewdate on every save
    private static final Set<String> CASE_BOOKKEEPING_FIELDS = new CaseInsensitiveHashSet("QCState", "QCStateLabel", "taskid", "description", "modified", "modifiedby", "created", "createdby", "lsid", "objectid", "container", "vetreview", "vetreviewdate");

    static final Set<String> CASE_FORM_TYPES = new CaseInsensitiveHashSet(NBRICasesFormType.NAME, NBRIBehavioralCasesFormType.NAME);

    // A case keeps its status and task unless the user chose to move it, and a draft never takes a submitted case back to In Progress.
    // Runs after the script triggers, which fill fields the update omitted from oldRow and put the form's target status on newRow.
    private void guardCaseTask(TableInfo table, Container c, User user, @Nullable Map<String, Object> newRow, @Nullable Map<String, Object> oldRow, ValidationException errors, @Nullable Map<String, Object> extraContext)
    {
        if (newRow == null || oldRow == null || !"cases".equalsIgnoreCase(table.getName()) || isValidateOnly(extraContext))
            return;

        Object caseTaskId = oldRow.get("taskid");
        Object rowTaskId = newRow.get("taskid");
        DataState oldState = getState(c, oldRow);
        DataState newState = getState(c, newRow);
        if (isDraft(newState) && oldState != null && !isDraft(oldState))
        {
            newRow.put("taskid", caseTaskId);
            if (caseFieldsChanged(newRow, oldRow))
                errors.addGlobalError("A draft can't change the case for " + newRow.get("Id") + " because it is " + oldState.getLabel() + ". Submit the form to save the case's changes.");
            else
                keepState(newRow, oldState);
            return;
        }

        // Forms stamp their own task on every case they load, so only a form save can carry a task to a case that has none
        if (caseTaskId == null && (rowTaskId == null || !isFormSave(extraContext)))
            return;

        boolean fromOwnTask = caseTaskId != null && rowTaskId != null && caseTaskId.toString().equalsIgnoreCase(rowTaskId.toString());
        if (fromOwnTask)
            return;

        if (!isDraft(newState) && claimsCase(c, user, rowTaskId, caseTaskId, oldState == null ? null : oldState.getLabel(), isMoveRequested(extraContext)))
            return;

        newRow.put("taskid", caseTaskId);
        if (oldState == null || newState == null || oldState.getRowId() == newState.getRowId())
            return;

        // Another task's unfinished work on the case would overwrite these edits
        if (caseTaskId != null && !"Completed".equals(oldState.getLabel()) && caseFieldsChanged(newRow, oldRow))
            errors.addGlobalError("The case for " + newRow.get("Id") + " belongs to another task with " + oldState.getLabel() + " status. Submit with " + oldState.getLabel() + " to save its changes, or edit it from its own task.");
        else
            keepState(newRow, oldState);
    }

    // A case form takes over a case that is Completed or on no task only when the user chose to move it there
    public static boolean claimsCase(Container c, User user, @Nullable Object rowTaskId, @Nullable Object caseTaskId, @Nullable String caseQcLabel, boolean moveRequested)
    {
        if (!moveRequested || rowTaskId == null || (caseTaskId != null && !"Completed".equals(caseQcLabel)))
            return false;

        UserSchema ehr = QueryService.get().getUserSchema(user, c, "ehr");
        TableInfo tasks = ehr == null ? null : ehr.getTable("tasks");
        if (tasks == null)
            return false;

        String formType = new TableSelector(tasks, Collections.singleton("formtype"), new SimpleFilter(FieldKey.fromString("taskid"), rowTaskId), null).getObject(String.class);
        return formType != null && CASE_FORM_TYPES.contains(formType);
    }

    private static boolean isFormSave(@Nullable Map<String, Object> extraContext)
    {
        return extraContext != null && "ehr-ext4DataEntry".equals(extraContext.get("importPathway"));
    }

    private static boolean isValidateOnly(@Nullable Map<String, Object> extraContext)
    {
        return extraContext != null && Stream.of("isValidateOnly", "validateOnly").anyMatch(key -> Boolean.TRUE.equals(ConvertHelper.convert(extraContext.get(key), Boolean.class)));
    }

    // Set by the case form's status dialog
    static boolean isMoveRequested(@Nullable Map<String, Object> extraContext)
    {
        return extraContext != null && Boolean.TRUE.equals(ConvertHelper.convert(extraContext.get("moveCase"), Boolean.class));
    }

    private static boolean isDraft(@Nullable DataState state)
    {
        return state != null && "In Progress".equals(state.getLabel());
    }

    private static void keepState(Map<String, Object> newRow, DataState state)
    {
        newRow.put("QCState", state.getRowId());
        newRow.put("QCStateLabel", state.getLabel());
    }

    // The EHR scripts null out oldRow's QCState and leave only its label
    private static @Nullable DataState getState(Container c, Map<String, Object> row)
    {
        String label = ConvertHelper.convert(row.get("QCStateLabel"), String.class);
        if (label != null)
            return QCStateManager.getInstance().getStateForLabel(c, label);

        Object rowId = row.get("QCState");
        return rowId == null ? null : QCStateManager.getInstance().getStateForRowId(c, ConvertHelper.convert(rowId, Long.class));
    }

    // Forms resend the case with every save, so only a value that differs from the stored row counts as an edit
    private static boolean caseFieldsChanged(Map<String, Object> newRow, Map<String, Object> oldRow)
    {
        return newRow.entrySet().stream()
                .filter(e -> !e.getKey().startsWith("_") && !CASE_BOOKKEEPING_FIELDS.contains(e.getKey()) && oldRow.containsKey(e.getKey()))
                .anyMatch(e -> !sameValue(e.getValue(), oldRow.get(e.getKey())));
    }

    private static boolean sameValue(@Nullable Object newValue, @Nullable Object oldValue)
    {
        if (newValue instanceof String s && s.isBlank())
            newValue = null;
        if (oldValue instanceof String s && s.isBlank())
            oldValue = null;

        try
        {
            // A blank checkbox reads as false
            if (newValue instanceof Boolean || oldValue instanceof Boolean)
                return Boolean.TRUE.equals(ConvertHelper.convert(newValue, Boolean.class)) == Boolean.TRUE.equals(ConvertHelper.convert(oldValue, Boolean.class));
            if (newValue == null || oldValue == null)
                return newValue == oldValue;

            // The form sends dates without milliseconds
            if (newValue instanceof Date || oldValue instanceof Date)
            {
                Date newDate = ConvertHelper.convert(newValue, Date.class);
                Date oldDate = ConvertHelper.convert(oldValue, Date.class);
                return newDate != null && oldDate != null && newDate.getTime() / 1000 == oldDate.getTime() / 1000;
            }
            if (newValue instanceof Number || oldValue instanceof Number)
            {
                BigDecimal newNumber = ConvertHelper.convert(newValue, BigDecimal.class);
                BigDecimal oldNumber = ConvertHelper.convert(oldValue, BigDecimal.class);
                return newNumber != null && oldNumber != null && newNumber.compareTo(oldNumber) == 0;
            }
        }
        catch (ConversionException e)
        {
            return false;
        }
        return newValue.toString().trim().equals(oldValue.toString().trim());
    }
}

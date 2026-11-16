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
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.triggers.Trigger;
import org.labkey.api.qc.DataState;
import org.labkey.api.qc.QCStateManager;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;

import java.math.BigDecimal;
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
        guardCaseTask(table, c, newRow, oldRow, errors, extraContext);
    }

    private static final Set<String> CASE_BOOKKEEPING_FIELDS = new CaseInsensitiveHashSet("QCState", "QCStateLabel", "taskid", "description", "modified", "modifiedby", "created", "createdby", "lsid", "objectid", "container");

    // A case never leaves the task that created it, and another task may change its status only when it leaves the case's fields alone.
    // Runs after the script triggers, which fill fields the update omitted from oldRow and put the form's target status on newRow.
    private void guardCaseTask(TableInfo table, Container c, @Nullable Map<String, Object> newRow, @Nullable Map<String, Object> oldRow, ValidationException errors, @Nullable Map<String, Object> extraContext)
    {
        if (newRow == null || oldRow == null || !"cases".equalsIgnoreCase(table.getName()))
            return;

        Object caseTaskId = oldRow.get("taskid");
        if (caseTaskId == null)
            return;

        Object rowTaskId = newRow.get("taskid");
        newRow.put("taskid", caseTaskId);
        if ((rowTaskId != null && caseTaskId.toString().equalsIgnoreCase(rowTaskId.toString())) || isValidateOnly(extraContext))
            return;

        DataState oldState = getState(c, oldRow);
        DataState newState = getState(c, newRow);
        if (oldState == null || newState == null || oldState.getRowId() == newState.getRowId())
            return;

        if (caseFieldsChanged(newRow, oldRow))
        {
            errors.addGlobalError("This case belongs to another task in " + oldState.getLabel() + ". Save case changes with the same status, or from the case's own task.");
        }
        else
        {
            newRow.put("QCState", oldState.getRowId());
            newRow.put("QCStateLabel", oldState.getLabel());
        }
    }

    private static boolean isValidateOnly(@Nullable Map<String, Object> extraContext)
    {
        return extraContext != null && Stream.of("isValidateOnly", "validateOnly").anyMatch(key -> Boolean.TRUE.equals(ConvertHelper.convert(extraContext.get(key), Boolean.class)));
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
        if (newValue == null || oldValue == null)
            return newValue == oldValue;

        try
        {
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

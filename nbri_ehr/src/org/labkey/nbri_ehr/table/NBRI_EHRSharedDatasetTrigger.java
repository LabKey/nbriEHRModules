/*
 * Copyright (c) 2024-2026 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.triggers.Trigger;
import org.labkey.api.query.QueryUpdateService;
import org.labkey.api.query.ValidationException;
import org.labkey.api.security.User;

import java.util.Map;

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
    }
}

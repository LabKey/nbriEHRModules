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
package org.labkey.nbri_ehr.demographics;

import org.labkey.api.data.CompareType;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.ehr.demographics.AbstractListDemographicsProvider;
import org.labkey.api.module.Module;
import org.labkey.api.query.FieldKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NecropsyStatusDemographicsProvider extends AbstractListDemographicsProvider
{

    public NecropsyStatusDemographicsProvider(Module module)
    {
        super(module, "study", "necropsyStatus", "necropsy_status");
        _supportsQCState = false;
    }

    @Override
    public boolean requiresRecalc(String schema, String query)
    {
        return ("study".equalsIgnoreCase(schema) && "deaths".equalsIgnoreCase(query)) ||
                ("study".equalsIgnoreCase(schema) && "demographics".equalsIgnoreCase(query));
    }

    @Override
    protected Collection<FieldKey> getFieldKeys()
    {
        Set<FieldKey> keys = new HashSet<>();
        keys.add(FieldKey.fromString("Id"));
        keys.add(FieldKey.fromString("necropsy_status"));

        return keys;
    }

    @Override
    protected SimpleFilter getFilter(Collection<String> ids)
    {
        List<String> qcStates = new ArrayList<>();
        qcStates.add("Completed");
        qcStates.add("Review Required");
        qcStates.add("Request: Pending");
        SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), ids, CompareType.IN);
        if (_supportsQCState)
        {
            filter.addCondition(FieldKey.fromString("QCState/Label"), qcStates, CompareType.IN);
        }

        return filter;
    }
}
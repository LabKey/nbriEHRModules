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
package org.labkey.nbri_ehr.demographics;

import org.labkey.api.data.CompareType;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.ehr.demographics.AbstractListDemographicsProvider;
import org.labkey.api.module.Module;
import org.labkey.api.query.FieldKey;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ProtocolAssignmentDemographicsProvider extends AbstractListDemographicsProvider
{
    public ProtocolAssignmentDemographicsProvider(Module owner)
    {
        super(owner, "study", "protocolAssignment", "protocolAssignments");
    }

    @Override
    protected Collection<FieldKey> getFieldKeys()
    {
        Set<FieldKey> keys = new HashSet<>();

        keys.add(FieldKey.fromString("lsid"));
        keys.add(FieldKey.fromString("Id"));
        keys.add(FieldKey.fromString("date"));
        keys.add(FieldKey.fromString("enddate"));
        keys.add(FieldKey.fromString("protocol"));
        keys.add(FieldKey.fromString("protocol/title"));
        keys.add(FieldKey.fromString("protocol/displayName"));
        keys.add(FieldKey.fromString("protocol/InvestigatorId"));
        keys.add(FieldKey.fromString("protocol/InvestigatorId/firstName"));
        keys.add(FieldKey.fromString("protocol/InvestigatorId/lastName"));
        keys.add(FieldKey.fromString("remark"));

        return keys;
    }

    @Override
    public SimpleFilter getFilter(Collection<String> ids)
    {
        SimpleFilter filter = super.getFilter(ids);
        filter.addCondition(FieldKey.fromString("enddate"), null, CompareType.ISBLANK);

        return filter;
    }

    @Override
    public boolean requiresRecalc(String schema, String query)
    {
        return ("study".equalsIgnoreCase(schema) && "protocolAssignment".equalsIgnoreCase(query)) ||
                ("ehr".equalsIgnoreCase(schema) && "protocol".equalsIgnoreCase(query));
    }
}

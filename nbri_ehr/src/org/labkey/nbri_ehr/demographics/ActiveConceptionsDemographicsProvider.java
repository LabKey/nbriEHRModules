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

import org.labkey.api.data.Sort;
import org.labkey.api.ehr.demographics.AbstractListDemographicsProvider;
import org.labkey.api.module.Module;
import org.labkey.api.query.FieldKey;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ActiveConceptionsDemographicsProvider extends AbstractListDemographicsProvider
{
    public ActiveConceptionsDemographicsProvider(Module module)
    {
        super(module, "study", "activeConceptions", "activeConceptions");
        // isActive already excludes non-public conceptions, and the query exposes no QCState column for the inherited filter to use
        _supportsQCState = false;
    }

    @Override
    public boolean requiresRecalc(String schema, String query)
    {
        return ("study".equalsIgnoreCase(schema) && ("birth".equalsIgnoreCase(query) || "pregnancy".equalsIgnoreCase(query))) ||
                ("nbri_ehr".equalsIgnoreCase(schema) && "Conception".equalsIgnoreCase(query));
    }

    @Override
    protected Collection<FieldKey> getFieldKeys()
    {
        Set<FieldKey> keys = new HashSet<>();
        keys.add(FieldKey.fromString("Id"));
        keys.add(FieldKey.fromString("ConceptId"));
        keys.add(FieldKey.fromString("ConceptDate"));

        return keys;
    }

    @Override
    protected Sort getSort()
    {
        return new Sort("-ConceptDate");
    }
}

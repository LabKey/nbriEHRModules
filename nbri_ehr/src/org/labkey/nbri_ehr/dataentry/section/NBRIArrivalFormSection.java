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
package org.labkey.nbri_ehr.dataentry.section;

import org.json.JSONObject;
import org.labkey.api.data.TableInfo;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.query.FieldKey;

import java.util.List;

public class NBRIArrivalFormSection extends BaseFormSection
{
    public NBRIArrivalFormSection()
    {
        super("study", "arrival", "Arrivals", "ehr-gridpanel", true, true, true);
    }

    @Override
    public JSONObject toJSON(DataEntryFormContext ctx, boolean includeFormElements)
    {
        JSONObject json = super.toJSON(ctx, includeFormElements);
        json.put("dataDependentCollapseHeader", true);
        return json;
    }

    @Override
    protected List<FieldKey> getFieldKeys(TableInfo ti)
    {
        List<FieldKey> keys = super.getFieldKeys(ti);

        keys.add(6, FieldKey.fromString("Id/demographics/dam"));
        keys.add(7, FieldKey.fromString("Id/demographics/sire"));
        keys.add(8, FieldKey.fromString("Id/demographics/species"));
        keys.add(9, FieldKey.fromString("Id/demographics/birth"));
        keys.add(10, FieldKey.fromString("Id/demographics/gender"));
        keys.add(12, FieldKey.fromString("Id/demographics/geographic_origin"));

        return keys;
    }
}

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
package org.labkey.nbri_ehr.dataentry.section;

import org.json.JSONObject;
import org.labkey.api.data.TableInfo;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.query.FieldKey;

import java.util.ArrayList;
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
        // super hands back the list registered for study.arrival when there is one, so copy before inserting
        List<FieldKey> keys = new ArrayList<>(super.getFieldKeys(ti));

        // anchor each insert to a named neighbour - which columns the metadata shows in the insert view decides
        // the index of everything after them
        keys.addAll(indexOf(keys, "project"), List.of(
                FieldKey.fromString("Id/demographics/species"),
                FieldKey.fromString("Id/demographics/gender"),
                FieldKey.fromString("Id/demographics/birth"),
                FieldKey.fromString("Id/demographics/dam"),
                FieldKey.fromString("Id/demographics/sire")));

        keys.add(indexOf(keys, "project") + 1, FieldKey.fromString("Id/demographics/geographic_origin"));

        // the social code sits beside Initial Location
        keys.add(indexOf(keys, "cage") + 1, FieldKey.fromString("Id/demographics/socialCode"));

        return keys;
    }

    private int indexOf(List<FieldKey> keys, String name)
    {
        int index = keys.indexOf(FieldKey.fromString(name));
        if (index < 0)
            throw new IllegalStateException("Cannot position the arrival form fields: study.arrival has no '" + name + "' field");

        return index;
    }
}

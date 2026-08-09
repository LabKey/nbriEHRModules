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
import org.labkey.api.ehr.dataentry.forms.NewAnimalFormSection;
import org.labkey.api.query.FieldKey;
import org.labkey.api.view.template.ClientDependency;

import java.util.ArrayList;
import java.util.List;

public class NBRIBirthFormSection extends NewAnimalFormSection
{
    // left to right column order of the Births grid; the demographics fields are not on study.birth, so they are added here
    private static final List<FieldKey> COLUMN_ORDER = List.of(
            FieldKey.fromString("Id"),
            FieldKey.fromString("date"),
            FieldKey.fromString("conceptId"),
            FieldKey.fromString("Id/demographics/species"),
            FieldKey.fromString("Id/demographics/gender"),
            FieldKey.fromString("Id/demographics/dam"),
            FieldKey.fromString("Id/demographics/sire"),
            FieldKey.fromString("cage"),
            FieldKey.fromString("type"),
            FieldKey.fromString("breedingType"),
            FieldKey.fromString("remark"),
            FieldKey.fromString("performedby")
    );

    public NBRIBirthFormSection()
    {
        super("study", "birth", "Births", false);
        addClientDependency(ClientDependency.supplierFromPath("ehr/window/FormBulkAddWindow.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/window/FormBulkAddWindow.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/window/StartWithConceptionWindow.js"));
    }

    @Override
    public JSONObject toJSON(DataEntryFormContext ctx, boolean includeFormElements)
    {
        JSONObject json = super.toJSON(ctx, includeFormElements);
        json.put("collapsible", true);
        json.put("initCollapsed", true);
        json.put("dataDependentCollapseHeader", true);
        return json;
    }

    @Override
    protected List<FieldKey> getFieldKeys(TableInfo ti)
    {
        List<FieldKey> ordered = new ArrayList<>(COLUMN_ORDER);

        // anything not explicitly ordered above (hidden and system fields) keeps its default position at the end
        super.getFieldKeys(ti).stream().filter(key -> !COLUMN_ORDER.contains(key)).forEach(ordered::add);

        return ordered;
    }

    @Override
    public List<String> getTbarButtons()
    {
        List<String> defaultButtons = super.getTbarButtons();

        int idx = defaultButtons.indexOf("ADDANIMALS");
        if (idx > -1)
        {
            defaultButtons.remove(idx);
            defaultButtons.add(idx, "NBRI_ADDANIMALS");
        }
        defaultButtons.remove("COPYFROMSECTION");
        defaultButtons.addFirst("NBRI_START_WITH_CONCEPTION");
        return defaultButtons;
    }

    @Override
    public List<String> getTbarMoreActionButtons()
    {
        List<String> defaultButtons = super.getTbarMoreActionButtons();
        defaultButtons.remove("GUESSPROJECT");
        defaultButtons.add("NBRI_FORM_BULK_ADD");
        return defaultButtons;
    }
}
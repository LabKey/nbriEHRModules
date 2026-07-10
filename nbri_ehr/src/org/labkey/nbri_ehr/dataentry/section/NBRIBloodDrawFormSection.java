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
import org.labkey.api.ehr.EHRService;
import org.labkey.api.ehr.dataentry.BloodDrawFormSection;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.view.template.ClientDependency;

import java.util.List;

public class NBRIBloodDrawFormSection extends BloodDrawFormSection
{
    private boolean _collapsible;
    private boolean _initCollapsed;
    private boolean _addCopyFromSection;

    public NBRIBloodDrawFormSection(boolean collapsible, boolean initCollapsed, boolean addCopyFromSection)
    {
        super(false, EHRService.FORM_SECTION_LOCATION.Body);

        if (!collapsible && initCollapsed)
            throw new IllegalArgumentException("Cannot set initCollapsed to true if collapsible is false");

        _collapsible = collapsible;
        _initCollapsed = initCollapsed;
        _addCopyFromSection = addCopyFromSection;
    }

    public NBRIBloodDrawFormSection(boolean isChild, String parentQueryName)
    {
        this(true, true, true);

        if (isChild && null != parentQueryName)
        {
            addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/ParentChild.js"));
            addConfigSource("ParentChild");

            addClientDependency(ClientDependency.supplierFromPath("ehr/data/ChildClientStore.js"));
            setClientStoreClass("EHR.data.ChildClientStore");
            addExtraProperty("parentQueryName", parentQueryName);
        }
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

        idx = defaultButtons.indexOf("COPYFROMSECTION");
        if (idx > -1)
        {
            if (!_addCopyFromSection)
                defaultButtons.remove(idx);
            else
            {
                defaultButtons.remove(idx);
                defaultButtons.add(idx, "NBRICOPYFROMSECTION");
            }
        }

        return defaultButtons;
    }

    @Override
    public JSONObject toJSON(DataEntryFormContext ctx, boolean includeFormElements)
    {
        JSONObject json = super.toJSON(ctx, includeFormElements);
        json.put("collapsible", _collapsible);
        json.put("initCollapsed", _initCollapsed);
        json.put("dataDependentCollapseHeader", true);
        return json;
    }
}

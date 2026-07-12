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

import org.labkey.api.view.template.ClientDependency;

import java.util.List;

public class NBRITissueDispositionFormSection extends BaseFormSection
{
    public NBRITissueDispositionFormSection(boolean isChild)
    {
        super("study", "tissueDisposition", "Tissue Disposition", "ehr-gridpanel", true, true, true);
        if (isChild)
        {
            addExtraProperty("parentQueryName", "deaths");

            addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/ParentChild.js"));
            addConfigSource("ParentChild");

            addClientDependency(ClientDependency.supplierFromPath("ehr/data/ChildClientStore.js"));
            setClientStoreClass("EHR.data.ChildClientStore");
        }
    }

    @Override
    public List<String> getTbarButtons()
    {
        List<String> defaults = super.getTbarButtons();
        defaults.remove("ADDANIMALS");
        defaults.remove("TEMPLATE");
        defaults.add("TEMPLATE_NO_ID_NO_DATE");
        return defaults;
    }

    @Override
    public List<String> getTbarMoreActionButtons()
    {
        List<String> defaultButtons = super.getTbarMoreActionButtons();
        defaultButtons.remove("COPY_IDS");
        return defaultButtons;
    }
}

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

public class NBRITreatmentGivenFormSection extends BaseFormSection
{
    public static final String LABEL = "Medications/Treatments Given";

    public NBRITreatmentGivenFormSection()
    {
        super("study", "drug", LABEL, "ehr-gridpanel", true, true, true);
        setClientStoreClass("NBRI_EHR.data.DrugAdministrationRunsClientStore");
        addClientDependency(ClientDependency.supplierFromPath("ehr/data/DrugAdministrationRunsClientStore.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/data/DrugAdministrationRunsClientStore.js"));
    }

    public NBRITreatmentGivenFormSection(boolean isChild, String parentQueryName)
    {
        this();
        if (isChild)
        {
            addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/ParentChild.js"));
            addConfigSource("ParentChild");

            addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/data/DrugAdministrationRunsChildClientStore.js"));
            setClientStoreClass("NBRI_EHR.data.DrugAdministrationRunsChildClientStore");
            addExtraProperty("parentQueryName", parentQueryName);
        }
    }

    @Override
    public List<String> getTbarButtons()
    {
        List<String> defaultButtons = super.getTbarButtons();
        int idx = defaultButtons.indexOf("SELECTALL");
        if (idx > -1)
            defaultButtons.add(idx + 1, "NBRI_DRUG_AMOUNT_HELPER");
        else
            defaultButtons.add("DRUGAMOUNTHELPER");
        return defaultButtons;
    }
}

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

public class NBRIClinicalObservationsFormSection extends BaseFormSection
{
    public static final String LABEL = "Observations";
    private boolean _autoPopulateDailyObs;

    public NBRIClinicalObservationsFormSection(boolean autoPopulateDailyObs, boolean initCollapsed)
    {
        super("study", "clinical_observations", LABEL, "ehr-clinicalobservationgridpanel", true, initCollapsed, true);

        _autoPopulateDailyObs = autoPopulateDailyObs;
        addClientDependency(ClientDependency.supplierFromPath("ehr/plugin/ClinicalObservationsCellEditing.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/data/ClinicalObservationClientStore.js"));
        addClientDependency(ClientDependency.supplierFromPath("ehr/grid/ClinicalObservationGridPanel.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/buttons/clinicalObsGridButton.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/buttons/addClinicalObsButton.js"));
        setClientStoreClass("NBRI_EHR.data.ClinicalObservationsClientStore");
    }

    public NBRIClinicalObservationsFormSection(boolean isChild, String parentQueryName)
    {
        this(false, true);

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
        List<String> defaults = super.getTbarButtons();

        if (_autoPopulateDailyObs)
        {
            defaults.add("NBRI_AUTO_POPULATE_DAILY_OBS");
        }
        else {
            defaults.add("NBRI_DAILY_CLINICAL_OBS");
        }

        return defaults;

    }
}

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

public class NBRIProtocolCountsFormSection extends BaseFormSection
{
    public NBRIProtocolCountsFormSection(String label, String parentQueryName)
    {
        super("ehr", "protocol_counts", label, "ehr-gridpanel", true, false, false);

        // approved counts are per species, not per animal
        setAllowBulkAdd(false);
        addExtraProperty(BY_PASS_ANIMAL_ID, "true");

        // each row takes its amendment and protocol from the amendment panel above; see ProtocolAmendment.js, where
        // amendmentId is the isParentField and protocol inherits from the parent
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/ParentChild.js"));
        addConfigSource("ParentChild");

        addClientDependency(ClientDependency.supplierFromPath("ehr/data/ChildClientStore.js"));
        setClientStoreClass("EHR.data.ChildClientStore");
        addExtraProperty("parentQueryName", parentQueryName);
    }
}

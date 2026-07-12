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

public class NBRIProjectAssignmentFormSection extends BaseFormSection
{
    public NBRIProjectAssignmentFormSection(boolean allowAnyId, boolean collapsible, boolean initCollapsed)
    {
        super("study", "assignment", "Project Assignment", "ehr-gridpanel", collapsible, initCollapsed, true);
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/data/AssignmentsClientStore.js"));

        if (allowAnyId)
        {
            setClientStoreClass("NBRI_EHR.data.AssignmentsAnyIdClientStore");
            addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/data/AssignmentsAnyIdClientStore.js"));
        }
        else
        {
            setClientStoreClass("NBRI_EHR.data.AssignmentsClientStore");
        }
    }
}
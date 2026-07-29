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
package org.labkey.nbri_ehr.dataentry.form;

import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.FormSection;
import org.labkey.api.ehr.dataentry.forms.LockAnimalsFormSection;
import org.labkey.api.module.Module;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.nbri_ehr.dataentry.section.NBRIAnimalDetailsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIArrivalFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIArrivalInstructionsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIProjectAssignmentFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIProtocolAssignmentFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITaskFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIWeightFormSection;

import java.util.Arrays;

public class NBRIArrivalFormType extends NBRIBaseTaskFormType
{
    public static final String NAME = "arrival";

    public NBRIArrivalFormType(DataEntryFormContext ctx, Module owner)
    {
        super(ctx, owner, NAME, "Arrivals", "Colony Management", Arrays.asList(
                new LockAnimalsFormSection(),
                new NBRIArrivalInstructionsFormSection(),
                new NBRITaskFormSection(),
                new NBRIAnimalDetailsFormSection(),
                new NBRIArrivalFormSection(),
                new NBRIProtocolAssignmentFormSection(true, true, true),
                new NBRIProjectAssignmentFormSection(true, true, true),
                new NBRIWeightFormSection(true, true)
                ));

        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/Assignment.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/Arrival.js"));

        for (FormSection s : getFormSections())
        {
            s.addConfigSource("Assignment");
            s.addConfigSource("Arrival");
        }

    }
}

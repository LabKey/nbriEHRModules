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
import org.labkey.api.module.Module;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.nbri_ehr.dataentry.section.NBRIAnimalDetailsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIProjectAssignmentFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIProtocolAssignmentFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITaskFormSection;

import java.util.List;

public class NBRIAssignmentFormType extends NBRIBaseTaskFormType
{
    public static final String NAME = "Assignment";
    public static final String LABEL = "Assignment";

    public NBRIAssignmentFormType(DataEntryFormContext ctx, Module owner)
    {
        super(ctx, owner, NAME, LABEL, "Colony Management", List.of(
                new NBRITaskFormSection(),
                new NBRIAnimalDetailsFormSection(),
                new NBRIProtocolAssignmentFormSection(true, true, true),
                new NBRIProjectAssignmentFormSection(true, true, true)
        ));

        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/Assignment.js"));

        for (FormSection s : getFormSections())
        {
            s.addConfigSource("Assignment");
        }
    }
}

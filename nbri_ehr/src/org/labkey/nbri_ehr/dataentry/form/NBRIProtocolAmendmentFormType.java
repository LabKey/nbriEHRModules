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

import org.labkey.api.ehr.dataentry.AbstractFormSection;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.FormSection;
import org.labkey.api.module.Module;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.nbri_ehr.dataentry.section.NBRIProtocolAmendmentFormPanelSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIProtocolCountsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITaskFormSection;

import java.util.List;

public class NBRIProtocolAmendmentFormType extends NBRIBaseTaskFormType
{
    public static final String NAME = "ProtocolAmendment";
    public static final String LABEL = "Protocol Amendment";

    private static final String PARENT_QUERY_NAME = "protocolAmendment";

    public NBRIProtocolAmendmentFormType(DataEntryFormContext ctx, Module owner)
    {
        // one amendment per form, with its per-species counts as child rows; no animal details, since an amendment
        // names a protocol and never an animal
        super(ctx, owner, NAME, LABEL, "Colony Management", List.of(
                new NBRITaskFormSection(),
                new NBRIProtocolAmendmentFormPanelSection(LABEL),
                new NBRIProtocolCountsFormSection("Animals Allowed Per Species", PARENT_QUERY_NAME)
        ));

        setTemplateMode(AbstractFormSection.TEMPLATE_MODE.NO_ID);

        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/ProtocolAmendment.js"));

        for (FormSection s : getFormSections())
        {
            s.addConfigSource("ProtocolAmendment");
        }
    }
}

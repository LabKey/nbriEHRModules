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
import org.labkey.api.ehr.security.EHRProtocolEditPermission;
import org.labkey.api.module.Module;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.nbri_ehr.dataentry.section.NBRIInitialAmendmentFormPanelSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIProtocolCountsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIProtocolFormPanelSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITaskFormSection;

import java.util.ArrayList;
import java.util.List;

public class NBRINewProtocolFormType extends NBRIBaseTaskFormType
{
    public static final String NAME = "NewProtocol";
    public static final String LABEL = "New Protocol";

    public NBRINewProtocolFormType(DataEntryFormContext ctx, Module owner)
    {
        // the protocol, its amendment 0 and that amendment's counts save together, so no protocol exists without them
        super(ctx, owner, NAME, LABEL, "Admin", List.of(
                new NBRITaskFormSection(),
                new NBRIProtocolFormPanelSection("Protocol"),
                new NBRIInitialAmendmentFormPanelSection(),
                new NBRIProtocolCountsFormSection("Animals Allowed Per Species", NBRIInitialAmendmentFormPanelSection.QUERY_NAME)
        ));

        setTemplateMode(AbstractFormSection.TEMPLATE_MODE.NO_ID);

        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/ProtocolAmendment.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/NewProtocol.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/panel/NewProtocolDataEntryPanel.js"));
        setJavascriptClass("NBRI_EHR.panel.NewProtocolDataEntryPanel");

        for (FormSection s : getFormSections())
        {
            s.addConfigSource("ProtocolAmendment");
            s.addConfigSource("NewProtocol");
        }
    }

    // a draft would put a live protocol into every protocol dropdown, since ehr.protocol has no QC state
    @Override
    protected List<String> getButtonConfigs()
    {
        return new ArrayList<>(List.of("SUBMIT"));
    }

    @Override
    protected List<String> getMoreActionButtonConfigs()
    {
        List<String> configs = super.getMoreActionButtonConfigs();
        configs.remove("SUBMITANDNEXT");
        return configs;
    }

    @Override
    public boolean isAvailable()
    {
        return super.isAvailable() && getCtx().getContainer().hasPermission(getCtx().getUser(), EHRProtocolEditPermission.class);
    }
}

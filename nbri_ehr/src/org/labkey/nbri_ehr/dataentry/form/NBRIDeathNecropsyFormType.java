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
package org.labkey.nbri_ehr.dataentry.form;

import org.json.JSONObject;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.FormSection;
import org.labkey.api.ehr.security.EHRVeterinarianPermission;
import org.labkey.api.module.Module;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.nbri_ehr.dataentry.section.NBRIAnimalDetailsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIDeathFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIGrossPathologyFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRINecropsyFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITaskFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITissueDispositionFormSection;
import org.labkey.nbri_ehr.security.NBRIEHRVetTechPermission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NBRIDeathNecropsyFormType extends NBRIBaseTaskFormType
{
    public static final String NAME = "Necropsy";
    public static final String LABEL = "Death/Necropsy";

    public NBRIDeathNecropsyFormType(DataEntryFormContext ctx, Module owner)
    {
        super(ctx, owner, NAME, LABEL, "Pathology", Arrays.asList(
                new NBRITaskFormSection(),
                new NBRIAnimalDetailsFormSection(),
                new NBRIDeathFormSection(),
                new NBRINecropsyFormSection(true),
                new NBRIGrossPathologyFormSection(true),
                new NBRITissueDispositionFormSection(true)

        ));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/DeathNecropsy.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/buttons/deathNecropsyButtons.js"));

        for (FormSection s : getFormSections())
        {
            s.addConfigSource("DeathNecropsy");
        }
    }

    @Override
    protected List<String> getButtonConfigs()
    {
        List<String> defaultButtons = new ArrayList<>();
        boolean isVetTech = getCtx().getContainer().hasPermission(getCtx().getUser(), NBRIEHRVetTechPermission.class);
        boolean isVet = getCtx().getContainer().hasPermission(getCtx().getUser(), EHRVeterinarianPermission.class);

        defaultButtons.add("NBRISAVEDRAFTBUTTON");
        defaultButtons.add("DEATHSUBMIT");

        if (isVet)
        {
            defaultButtons.add("SUBMIT"); //submit final
            defaultButtons.add("DEATH_NECROPSY_VET_REVIEW"); //submit for review
        }
        else if (isVetTech)
        {
            defaultButtons.add("DEATH_NECROPSY_VET_REVIEW"); //submit for review
        }
        return defaultButtons;
    }

    @Override
    protected List<String> getMoreActionButtonConfigs()
    {
        List<String> configs = super.getMoreActionButtonConfigs();
        configs.remove("DISCARD");
        return configs;
    }

    @Override
    public JSONObject toJSON()
    {
        JSONObject ret = super.toJSON();

        //this form involves extra work on save, so relax warning thresholds to reduce error logging
        ret.put("perRowWarningThreshold", 0.5);
        ret.put("totalTransactionWarningThrehsold", 60);
        ret.put("perRowValidationWarningThrehsold", 6);
        ret.put("totalValidationTransactionWarningThrehsold", 60);

        return ret;
    }
}

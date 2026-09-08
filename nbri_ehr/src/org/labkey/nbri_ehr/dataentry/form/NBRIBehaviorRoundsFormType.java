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
import org.labkey.api.ehr.dataentry.SimpleFormSection;
import org.labkey.api.ehr.security.EHRClinicalEntryPermission;
import org.labkey.api.module.Module;
import org.labkey.api.query.Queryable;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.nbri_ehr.dataentry.section.NBRIAnimalDetailsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRICaseTemplateFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRICasesFormPanelSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIClinicalObservationsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITaskFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITreatmentGivenFormSection;

import java.util.Arrays;
import java.util.List;

public class NBRIBehaviorRoundsFormType extends NBRIBaseTaskFormType
{
    @Queryable
    public static final String NAME = "Behavioral Rounds";
    public static final String LABEL = "Behavioral Rounds";

    public NBRIBehaviorRoundsFormType(DataEntryFormContext ctx, Module owner)
    {
        super(ctx, owner, NAME, LABEL, "Behavior", Arrays.<FormSection>asList(
                new NBRITaskFormSection(),
                new NBRIAnimalDetailsFormSection(),
                new NBRICaseTemplateFormSection("Case Template", "Case Template", "nbri_ehr-casetemplatepanel", Arrays.asList(ClientDependency.supplierFromPath("nbri_ehr/panel/CaseTemplatePanel.js"))),
                new NBRICasesFormPanelSection("Behavior Case", ctx, true),
                new NBRIClinicalObservationsFormSection(null, true, "cases"),
                new NBRITreatmentGivenFormSection(true, "cases")
        ));

        setTemplateMode(AbstractFormSection.TEMPLATE_MODE.NO_ID);
        setDisplayReviewRequired(true);

        for (FormSection s : this.getFormSections())
        {
            s.addConfigSource("BehavioralCase");
            s.addConfigSource("BehaviorDefaults");
            s.addConfigSource("TreatmentSchedule");
            s.addConfigSource("BehaviorRounds");
            s.addConfigSource("MedicationEndDate");

            if (s instanceof SimpleFormSection && !s.getName().equals("tasks"))
                s.setTemplateMode(AbstractFormSection.TEMPLATE_MODE.NO_ID);

            if (s instanceof AbstractFormSection)
            {
                ((AbstractFormSection)s).setAllowBulkAdd(false);
            }
        }
        setStoreCollectionClass("NBRI_EHR.data.CaseStoreCollection");
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/data/CaseStoreCollection.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/TreatmentSchedule.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/field/DrugVolumeField.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/window/DrugAmountWindow.js"));

        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/BehavioralCase.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/BehaviorDefaults.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/BehaviorRounds.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/MedicationEndDate.js"));
        addClientDependency(ClientDependency.supplierFromPath("ehr/panel/ExamDataEntryPanel.js"));
        setJavascriptClass("EHR.panel.ExamDataEntryPanel");

        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/panel/NBRIExamCasesDataEntryPanel.js"));
        setJavascriptClass("NBRI_EHR.panel.ExamCasesDataEntryPanel");

        // Needed for case and scheduled date/time when navigating from treatment or observation schedule
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/buttons/treatmentSubmit.js"));
    }

    @Override
    protected boolean canInsert()
    {
        if (!getCtx().getContainer().hasPermission(getCtx().getUser(), EHRClinicalEntryPermission.class))
            return false;

        return super.canInsert();
    }

    @Override
    protected List<String> getButtonConfigs()
    {
        List<String> ret = super.getButtonConfigs();

        ret.remove("SUBMIT");
        ret.add("NBRI_TREATMENT_SUBMIT");

        return ret;
    }

    @Override
    protected List<String> getMoreActionButtonConfigs()
    {
        List<String> configs = super.getMoreActionButtonConfigs();
        configs.remove("DISCARD");
        return configs;
    }
}
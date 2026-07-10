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

import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.FormSection;
import org.labkey.api.ehr.security.EHRVeterinarianPermission;
import org.labkey.api.module.Module;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.nbri_ehr.dataentry.section.NBRIAnimalDetailsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIBloodDrawFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIClinicalObservationsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIClinicalRemarksFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIObservationOrdersFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIProcedureFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIProcedureOrderFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITaskFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITreatmentGivenFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITreatmentOrderFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIVitalsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIWeightFormSection;
import org.labkey.nbri_ehr.security.NBRIEHRVetTechPermission;

import java.util.List;

public class NBRIBulkClinicalFormType extends NBRIBaseTaskFormType
{
    public static final String NAME = "Bulk Clinical Entry";
    public static final String LABEL = "Bulk Clinical Entry";

    public NBRIBulkClinicalFormType(DataEntryFormContext ctx, Module owner)
    {
        super(ctx, owner, NAME, LABEL, "Clinical", List.of(
                new NBRITaskFormSection(),
                new NBRIAnimalDetailsFormSection(),
                new NBRIClinicalRemarksFormSection(ctx.getContainer().hasPermission(ctx.getUser(), NBRIEHRVetTechPermission.class),
                        ctx.getContainer().hasPermission(ctx.getUser(), EHRVeterinarianPermission.class),
                        ctx.getContainer().hasPermission(ctx.getUser(), AdminPermission.class)),
                new NBRIWeightFormSection(true, true),
                new NBRIClinicalObservationsFormSection(false, null),
                new NBRIObservationOrdersFormSection("NBRI_DAILY_CLINICAL_OBS_ORDERS", false, null),
                new NBRIProcedureFormSection(),
                new NBRIProcedureOrderFormSection(),
                new NBRITreatmentGivenFormSection(),
                new NBRITreatmentOrderFormSection(),
                new NBRIVitalsFormSection(),
                new NBRIBloodDrawFormSection(true, true, true)
        ));

        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/ClinicalDefaults.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/BulkClinical.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/TreatmentSchedule.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/MedicationEndDate.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/field/DrugVolumeField.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/window/DrugAmountWindow.js"));

        // Needed for case and scheduled date/time when navigating from treatment or observation schedule
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/buttons/treatmentSubmit.js"));

        for (FormSection s : getFormSections())
        {
            s.addConfigSource("ClinicalDefaults");
            s.addConfigSource("BulkClinical");
            s.addConfigSource("TreatmentSchedule");
            s.addConfigSource("MedicationEndDate");
        }
    }

    @Override
    protected List<String> getButtonConfigs()
    {
        List<String> ret = super.getButtonConfigs();

        ret.remove("SUBMIT");
        ret.add("NBRI_TREATMENT_SUBMIT");

        return ret;
    }
}

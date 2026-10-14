/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.form;

import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.FormSection;
import org.labkey.api.module.Module;
import org.labkey.nbri_ehr.dataentry.section.NBRIAnimalDetailsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIDiagnosesFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIGrossPathologyFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIHistologyFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRINecropsyFinalFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRINecropsyFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRINecropsyRequestFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRINecropsyTaskFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIProcedureStaffFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITaskFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITissueDispositionFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITissueMeasurementsFormSection;

import java.util.List;

public class NBRINecropsyFinalFormType extends NBRINecropsyBaseFormType
{
    public NBRINecropsyFinalFormType(DataEntryFormContext ctx, Module owner)
    {
        super(ctx, owner, FINAL, List.of(
                new NBRITaskFormSection(),
                new NBRIAnimalDetailsFormSection(),
                new NBRINecropsyTaskFormSection(true),
                new NBRINecropsyRequestFormSection(true),
                new NBRINecropsyFormSection(true),
                new NBRIGrossPathologyFormSection(true),
                NBRIDiagnosesFormSection.forPreliminary(true),
                new NBRIProcedureStaffFormSection(true),
                new NBRITissueMeasurementsFormSection(true),
                new NBRITissueDispositionFormSection(true),
                new NBRIHistologyFormSection(false),
                NBRIDiagnosesFormSection.forFinal(false),
                new NBRINecropsyFinalFormSection(false)
        ), "NECROPSY_FINAL_REVIEW", "NECROPSY_FINAL_SUBMIT", true);

        for (FormSection s : getFormSections())
        {
            s.addConfigSource("NecropsyRequestReadOnly");
        }
    }
}

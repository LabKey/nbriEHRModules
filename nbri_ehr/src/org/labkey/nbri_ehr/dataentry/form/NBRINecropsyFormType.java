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
import org.labkey.nbri_ehr.dataentry.section.NBRINecropsyFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRINecropsyRequestFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRINecropsyTaskFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIProcedureStaffFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITaskFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITissueDispositionFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITissueMeasurementsFormSection;

import java.util.List;

/**
 * The preliminary necropsy stage.
 */
public class NBRINecropsyFormType extends NBRINecropsyBaseFormType
{
    public NBRINecropsyFormType(DataEntryFormContext ctx, Module owner)
    {
        super(ctx, owner, PRELIMINARY, List.of(
                new NBRITaskFormSection(),
                new NBRIAnimalDetailsFormSection(),
                new NBRINecropsyTaskFormSection(true),
                new NBRINecropsyRequestFormSection(true),
                new NBRINecropsyFormSection(false),
                new NBRIGrossPathologyFormSection(false),
                NBRIDiagnosesFormSection.forPreliminary(false),
                new NBRIProcedureStaffFormSection(false),
                new NBRITissueMeasurementsFormSection(false),
                new NBRITissueDispositionFormSection(false)
        ), "NECROPSY_PRELIMINARY_REVIEW", "NECROPSY_PRELIMINARY_SUBMIT", true, REQUEST_QUERIES);

        for (FormSection s : getFormSections())
        {
            s.addConfigSource("NecropsyRequestReadOnly");
        }
    }

    // reached from the necropsy notifications and task links, not started from the Enter Data page
    @Override
    public boolean isVisible()
    {
        return false;
    }
}

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
import org.labkey.nbri_ehr.dataentry.section.NBRINecropsyRequestFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRINecropsyTaskFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITaskFormSection;

import java.util.List;

public class NBRINecropsyRequestFormType extends NBRINecropsyBaseFormType
{
    public NBRINecropsyRequestFormType(DataEntryFormContext ctx, Module owner)
    {
        super(ctx, owner, REQUEST, List.of(
                new NBRITaskFormSection(),
                new NBRIAnimalDetailsFormSection(),
                new NBRINecropsyTaskFormSection(false),
                new NBRINecropsyRequestFormSection(false)
        ), "NECROPSY_REQUEST_REVIEW", "NECROPSY_REQUEST_SUBMIT", false, List.of());

        for (FormSection s : getFormSections())
        {
            s.addConfigSource("NecropsyRequest");
        }
    }
}

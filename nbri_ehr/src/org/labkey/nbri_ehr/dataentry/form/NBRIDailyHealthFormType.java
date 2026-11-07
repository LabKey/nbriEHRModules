/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.form;

import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.FormSection;
import org.labkey.api.module.Module;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.nbri_ehr.dataentry.section.NBRIAnimalDetailsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIDailyHealthFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITaskFormSection;

import java.util.List;

/** Backup entry and correction of one daily health log entry; its task id is the entry's logEntryId. */
public class NBRIDailyHealthFormType extends NBRIBaseTaskFormType
{
    public static final String NAME = "Daily Health";
    public static final String LABEL = "Daily Health";

    public NBRIDailyHealthFormType(DataEntryFormContext ctx, Module owner)
    {
        super(ctx, owner, NAME, LABEL, "Clinical", List.of(
                new NBRITaskFormSection(),
                new NBRIAnimalDetailsFormSection(),
                new NBRIDailyHealthFormSection(LABEL)
        ));

        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/DailyHealth.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/panel/DailyHealthDataEntryPanel.js"));
        setJavascriptClass("NBRI_EHR.panel.DailyHealthDataEntryPanel");

        for (FormSection s : getFormSections())
        {
            s.addConfigSource("DailyHealth");
        }
    }
}

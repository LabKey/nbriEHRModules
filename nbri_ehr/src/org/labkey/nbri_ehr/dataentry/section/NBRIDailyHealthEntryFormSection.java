/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.section;

import org.labkey.api.ehr.dataentry.NonStoreFormSection;
import org.labkey.api.view.template.ClientDependency;

/** The fields every row of a daily health log entry shares, which the panel writes into the entry's grid rows. */
public class NBRIDailyHealthEntryFormSection extends NonStoreFormSection
{
    public NBRIDailyHealthEntryFormSection()
    {
        super("DailyHealthEntry", "Log Entry", "nbri_ehr-dailyhealthentrypanel");
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/panel/DailyHealthEntryPanel.js"));
    }
}

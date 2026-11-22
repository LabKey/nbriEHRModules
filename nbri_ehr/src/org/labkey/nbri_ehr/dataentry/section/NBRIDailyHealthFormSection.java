/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.section;

import org.labkey.api.view.template.ClientDependency;

public class NBRIDailyHealthFormSection extends BaseFormSection
{
    public NBRIDailyHealthFormSection(String label)
    {
        super("study", "dailyHealthLog", label, "nbri_ehr-dailyhealthgridpanel", true, false, false);

        // a form holds one log entry, which is always for a single animal
        setAllowBulkAdd(false);

        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/grid/DailyHealthGridPanel.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/data/DailyHealthClientStore.js"));
        setClientStoreClass("NBRI_EHR.data.DailyHealthClientStore");
    }

    @Override
    protected String getServerSort()
    {
        return "sortOrder";
    }
}

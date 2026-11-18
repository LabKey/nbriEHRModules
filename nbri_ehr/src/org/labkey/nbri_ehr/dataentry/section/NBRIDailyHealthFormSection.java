/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.section;

public class NBRIDailyHealthFormSection extends BaseFormSection
{
    public NBRIDailyHealthFormSection(String label)
    {
        super("study", "dailyHealthLog", label, "ehr-gridpanel", true, false, false);

        // a form holds one log entry, which is always for a single animal
        setAllowBulkAdd(false);
    }
}

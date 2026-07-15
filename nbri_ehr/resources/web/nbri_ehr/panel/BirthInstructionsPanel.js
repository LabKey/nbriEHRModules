/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.panel.BirthInstructionsPanel', {
    extend: 'NBRI_EHR.panel.InstructionsPanel',
    alias: 'widget.nbri_ehr-birthinstructionspanel',

    getItems: function(){
        return {
            html: 'This form allows you to enter new birth.  The birth can be entered, and then saved.  If saved as a draft, none of the extra steps below will occur.  Only once the record has been finalized will these steps take place:<p>' +
                    '<ul>' +
                    '<li>If not already present, a demographics record will be created.  This is the table holding 1 row for all known IDs.</li>' +
                    '<li>If date, species, and gender are entered at the time the birth record is entered, the demographic record will use these.</li>' +
                    '<li>If room is entered, a housing record will be created with the room and enclosure starting on the birth date.</li>' +
                    '</ul>',
            style: 'padding: 5px;'
        };
    }
});
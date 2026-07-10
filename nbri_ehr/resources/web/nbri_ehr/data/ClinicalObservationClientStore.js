/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.data.ClinicalObservationsClientStore', {
    extend: 'EHR.data.DataEntryClientStore',

    constructor: function(){
        this.callParent(arguments);

        this.observationTypesStore = EHR.DataEntryUtils.getObservationTypesStore();
    },

    getExtraContext: function(){
        // Pass through tasks for entering observations from schedule
        const obsTask = LABKEY.ActionURL.getParameter('obsTask');

        return {
            orderTasksInTransaction: [obsTask]

        }
    }
});
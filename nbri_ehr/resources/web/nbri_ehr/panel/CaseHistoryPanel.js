/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * @cfg caseId
 * @cfg maxGridHeight
 * @cfg autoLoadRecords
 */
Ext4.define('NBRI_EHR.panel.CaseHistoryPanel', {
    extend: 'NBRI_EHR.panel.ClinicalHistoryPanel',
    alias: 'widget.nbri_ehr-casehistorypanel',

    getStoreConfig: function(){
        return {
            type: 'ehr-clinicalhistorystore',
            containerPath: this.containerPath,
            actionName: 'getCaseHistory',
            sorters: [{property: 'group'}, {property: 'timeString'}]
        };
    }
});

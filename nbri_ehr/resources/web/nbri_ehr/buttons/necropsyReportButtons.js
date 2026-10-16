/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// the query grid loads without Ext4
window.NBRI_EHR = window.NBRI_EHR || {};

NBRI_EHR.NecropsyReportButtons = {
    openReport: function(dataRegion, redacted){
        // the necropsy task grid's key is the task's objectid
        var objectIds = dataRegion ? dataRegion.getChecked() : [];
        if (!objectIds.length){
            LABKEY.Utils.alert('Error', 'Please select one or more necropsies.');
            return;
        }

        var params = {objectIds: objectIds.join(';'), _print: 1};
        if (redacted)
            params.redacted = true;

        window.open(LABKEY.ActionURL.buildURL('nbri_ehr', 'necropsyReport', null, params), '_blank');
    }
};

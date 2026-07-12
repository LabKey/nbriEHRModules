/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);
var departures = [];

function onInit(event, helper){
    helper.setScriptOptions({
        requiresStatusRecalc: false
    });

}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_INSERT, 'study', 'departure', function(helper, scriptErrors, row, oldRow) {

    if (row.id) {
        departures.push(row.id);
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.COMPLETE, 'study', 'departure', function(event, errors, helper){

    if (!helper.isETL() && helper.isEHRDataEntry()) {
        triggerHelper.updateProcedureOrdersToCompleted(departures);
    }
});


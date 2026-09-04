/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);
EHR.Server.Utils = require("ehr/utils").EHR.Server.Utils;

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// dams whose conception this save claims or frees.  The outcome announces its own Id, which is the dam only when the
// record was entered against her, so resolve the dam from the conception instead of trusting row.Id.
var damsToSync = [];

// resolves the dam of a conception so the outcome can announce her; the conception carries the dam, the outcome row does not
function addConceptionDam(conceptId) {
    if (!conceptId)
        return;

    var dam = triggerHelper.getConceptionDam(conceptId);
    if (dam && damsToSync.indexOf(dam) === -1) {
        damsToSync.push(dam);
    }
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'pregnancy', function(event, helper){
    // the script scope can outlive a single save, so never inherit dams from a prior one
    damsToSync = [];
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'pregnancy', function(helper, scriptErrors, row, oldRow) {
    if (!helper.isETL() && row.conceptId) {
        if (triggerHelper.totalRecords('nbri_ehr', 'Conception', 'ConceptId', row.conceptId) === 0) {
            EHR.Server.Utils.addError(scriptErrors, 'conceptId', 'This conception Id does not match any conception record', 'WARN');
        }

        //when updating a record that already carries this conception id, the existing row accounts for one match
        var conceptIdThreshold = (oldRow && oldRow.conceptId === row.conceptId) ? 1 : 0;
        if (triggerHelper.totalRecords('study', 'pregnancy', 'conceptId', row.conceptId) > conceptIdThreshold) {
            EHR.Server.Utils.addError(scriptErrors, 'conceptId', 'This conception Id is already used by another pregnancy outcome record', 'INFO');
        }

        if (triggerHelper.totalRecords('study', 'birth', 'conceptId', row.conceptId) > 0) {
            EHR.Server.Utils.addError(scriptErrors, 'conceptId', 'This conception Id is already used by a birth record', 'INFO');
        }
    }

    // validation never reaches COMPLETE, so resolving dams during it is a wasted query per row
    if (!helper.isETL() && !helper.isValidateOnly()) {
        addConceptionDam(row.conceptId);

        // a re-pointed or cleared outcome reopens the conception it used to claim
        addConceptionDam(oldRow ? oldRow.conceptId : null);
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_DELETE, 'study', 'pregnancy', function(helper, scriptErrors, row) {
    // deleting the outcome reopens its conception
    addConceptionDam(row.conceptId);
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.COMPLETE, 'study', 'pregnancy', function(event, errors, helper){
    if (damsToSync.length) {
        triggerHelper.reportDataChange('nbri_ehr', 'Conception', damsToSync);
        damsToSync = [];
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.ON_BECOME_PUBLIC, 'study', 'pregnancy', function(scriptErrors, helper, row, oldRow) {
    if (!helper.isETL()) {

        var outcomeRec = {
            Id: row.Id,
            date: row.date,
            result: row.result
        }
        triggerHelper.sendPregnancyOutcomeNotification(row.Id, outcomeRec);
    }
});

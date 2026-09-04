/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// the shared trigger collects modified participants from row.Id, which this table does not have, so announce the dams
// here or their cached activeConceptions keeps a stale Pregnant value
var damsModified = [];

function addDam(dam) {
    if (dam && damsModified.indexOf(dam) === -1) {
        damsModified.push(dam);
    }
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'nbri_ehr', 'Conception', function(event, helper){
    // the script scope can outlive a single save, so never inherit dams from a prior one
    damsModified = [];
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'nbri_ehr', 'Conception', function(helper, scriptErrors, row, oldRow) {
    if (helper.isValidateOnly())
        return;

    addDam(row.Dam);

    // a re-pointed conception frees the dam it used to belong to
    addDam(oldRow ? oldRow.Dam : null);
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_DELETE, 'nbri_ehr', 'Conception', function(helper, scriptErrors, row) {
    // the row LabKey passes for a delete can carry keys only, and the record is still readable at this point
    addDam(row.Dam || triggerHelper.getConceptionDam(row.ConceptId));
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.COMPLETE, 'nbri_ehr', 'Conception', function(event, errors, helper){
    if (damsModified.length) {
        triggerHelper.reportDataChange('nbri_ehr', 'Conception', damsModified);
        damsModified = [];
    }
});

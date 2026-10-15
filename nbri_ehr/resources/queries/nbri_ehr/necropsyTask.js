/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

function isPublic(row) {
    if (!row)
        return false;
    var qc = row.QCStateLabel ? EHR.Server.Security.getQCStateByLabel(row.QCStateLabel)
            : (row.QCState ? EHR.Server.Security.getQCStateByRowId(row.QCState) : null);
    return !!(qc && qc.PublicData);
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'nbri_ehr', 'necropsyTask', function(helper, scriptErrors, row, oldRow) {

    if (!row.Date) {
        EHR.Server.Utils.addError(scriptErrors, 'Date', 'Date is required', 'ERROR');
    }

    if (!row.SubmittingVet) {
        EHR.Server.Utils.addError(scriptErrors, 'SubmittingVet', 'Submitting Vet is required', 'ERROR');
    }

    if (!row.Task) {
        EHR.Server.Utils.addError(scriptErrors, 'Task', 'Task title is required', 'ERROR');
    }

    // notify once per stage: when the status changes or the task first becomes public, not on every later save
    if (!helper.isValidateOnly() && row.status && isPublic(row) && (!isPublic(oldRow) || oldRow.status !== row.status)) {
        triggerHelper.sendNecropsyNotification(row.taskid, row.status);
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_DELETE, 'nbri_ehr', 'necropsyTask', function(helper, scriptErrors, row) {

    if (row.status && row.status !== 'Request Pending') {
        EHR.Server.Utils.addError(scriptErrors, 'status', 'Cannot delete a task that has necropsy data.', 'ERROR');
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_DELETE, 'nbri_ehr', 'necropsyTask', function(helper, scriptErrors, row) {

    triggerHelper.deleteNecropsyData(row.taskid);
});

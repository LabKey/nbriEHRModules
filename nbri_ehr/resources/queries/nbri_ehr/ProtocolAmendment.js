/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// Rhino hands Java an undefined argument as an object ConvertHelper can't read; 0 is a real amendment number
function orNull(value) {
    return value === undefined ? null : value;
}

function onInit(event, helper) {
    // amendments are routinely entered after the fact, from a letter that arrived weeks later, or approved ahead of
    // taking effect; the protocol's end date bounds them instead
    helper.setScriptOptions({
        allowDatesInDistantPast: true,
        allowFutureDates: true,
        requiresStatusRecalc: false
    });
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'nbri_ehr', 'ProtocolAmendment', function (helper, scriptErrors, row, oldRow) {
    // counts reference their amendment by objectid, which only the data entry form generates
    if (!oldRow && !row.objectid) {
        row.objectid = LABKEY.Utils.generateUUID().toUpperCase();
    }

    if (!row.protocol) {
        return;
    }

    if (!oldRow) {
        if (row.amendmentNumber === null || row.amendmentNumber === undefined) {
            row.amendmentNumber = triggerHelper.getNextAmendmentNumber(row.protocol);
        }
        // the latest amendment runs to the protocol's end date; validation of a new protocol's own v0 runs before the
        // protocol is saved, so there may be nothing to read yet
        if (!row.enddate) {
            row.enddate = triggerHelper.getProtocolEnddate(row.protocol);
        }
    }

    // the Java Map arrives as a plain JS object
    var movesPredecessor = !oldRow || !triggerHelper.isSameDay(orNull(row.date), orNull(oldRow.date));
    var problems = triggerHelper.validateAmendment(row.protocol, orNull(row.rowid), orNull(row.amendmentNumber), orNull(row.date), orNull(row.enddate), movesPredecessor);
    for (var field in problems) {
        EHR.Server.Utils.addError(scriptErrors, field, String(problems[field]), 'ERROR');
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_INSERT, 'nbri_ehr', 'ProtocolAmendment', function (helper, errors, row) {
    if (helper.isValidateOnly()) {
        return;
    }

    triggerHelper.closePriorAmendment(row.protocol, orNull(row.amendmentNumber), orNull(row.date), null);
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_UPDATE, 'nbri_ehr', 'ProtocolAmendment', function (helper, errors, row, oldRow) {
    if (helper.isValidateOnly() || triggerHelper.isSameDay(orNull(row.date), orNull(oldRow.date))) {
        return;
    }

    triggerHelper.closePriorAmendment(row.protocol, orNull(row.amendmentNumber), orNull(row.date), orNull(oldRow.date));
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_DELETE, 'nbri_ehr', 'ProtocolAmendment', function (helper, errors, row) {
    var msg = triggerHelper.validateAmendmentDelete(row.protocol, orNull(row.amendmentNumber));
    if (msg) {
        errors._form = String(msg);
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_DELETE, 'nbri_ehr', 'ProtocolAmendment', function (helper, errors, row) {
    if (helper.isValidateOnly()) {
        return;
    }

    triggerHelper.afterAmendmentDelete(row.protocol, orNull(row.objectid));
});

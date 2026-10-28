/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// Rhino hands Java an undefined argument as an object ConvertHelper can't read
function orNull(value) {
    return value === undefined ? null : value;
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'ehr', 'protocol', function (helper, scriptErrors, row, oldRow) {
    var datesChanged = !oldRow || !triggerHelper.isSameDay(orNull(row.approve), orNull(oldRow.approve)) || !triggerHelper.isSameDay(orNull(row.enddate), orNull(oldRow.enddate));
    if (datesChanged && triggerHelper.isBeforeDay(orNull(row.enddate), orNull(row.approve))) {
        EHR.Server.Utils.addError(scriptErrors, 'enddate', 'End date cannot be before the approval date.', 'ERROR');
    }

    if (oldRow && !triggerHelper.isSameDay(orNull(row.enddate), orNull(oldRow.enddate))) {
        var msg = triggerHelper.validateProtocolEnddate(row.protocol, orNull(row.enddate));
        if (msg) {
            EHR.Server.Utils.addError(scriptErrors, 'enddate', String(msg), 'ERROR');
        }
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_UPDATE, 'ehr', 'protocol', function (helper, errors, row, oldRow) {
    if (helper.isValidateOnly() || triggerHelper.isSameDay(orNull(row.enddate), orNull(oldRow.enddate))) {
        return;
    }

    triggerHelper.syncLatestAmendmentEnddate(row.protocol, orNull(row.enddate));
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_DELETE, 'ehr', 'protocol', function (helper, errors, row) {
    if (row.protocol && triggerHelper.hasAmendments(row.protocol)) {
        errors._form = 'Protocol ' + row.protocol + ' has amendments and cannot be deleted.';
    }
});

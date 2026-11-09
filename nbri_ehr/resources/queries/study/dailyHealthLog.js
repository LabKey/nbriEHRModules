/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

var entryIdsInSave = [];
var deletedObjectIds = [];
// logEntryId|sortOrder pairs claimed earlier in this save. The saved-row check runs at commit, which validateOnly never reaches.
var entryOrdersInSave = {};

// A JS array reaches Java as a List whose elements don't bind as SQL string parameters.
function toJavaStrings(values) {
    var list = new java.util.ArrayList();
    for (var i = 0; i < values.length; i++)
        list.add(String(values[i]));
    return list;
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'dailyHealthLog', function(event, helper){
    helper.setScriptOptions({
        skipAssignmentCheck: true
    });

    // the script scope can outlive a single save
    entryIdsInSave = [];
    deletedObjectIds = [];
    entryOrdersInSave = {};
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'dailyHealthLog', function(helper, scriptErrors, row, oldRow) {
    // an update may carry only the changed columns
    var value = function(name) {
        return row[name] !== undefined || !oldRow ? row[name] : oldRow[name];
    };

    if (oldRow) {
        if (row.logEntryId !== undefined && row.logEntryId !== oldRow.logEntryId)
            EHR.Server.Utils.addError(scriptErrors, 'logEntryId', 'The log entry Id cannot be changed', 'ERROR');
        if (row.Id !== undefined && row.Id !== oldRow.Id)
            EHR.Server.Utils.addError(scriptErrors, 'Id', 'The animal cannot be changed on a saved log entry', 'ERROR');
    }
    // checked on insert only, so earlier entries for an animal that has since died stay editable
    else if (row.Id && !triggerHelper.isAnimalAlive(row.Id)) {
        EHR.Server.Utils.addError(scriptErrors, 'Id', row.Id + ' is not a living animal', 'ERROR');
    }

    // a code retired after a row was saved stays valid on that row
    if (row.code && (!oldRow || row.code !== oldRow.code)) {
        var codeError = triggerHelper.validateDailyHealthCode(row.code);
        if (codeError)
            EHR.Server.Utils.addError(scriptErrors, 'code', codeError, 'ERROR');
    }

    // location is never stored; only the API supplies it, through extraContext
    var extraContext = helper.getExtraContext() || {};
    if (extraContext.locationArea || extraContext.locationEnclosure || extraContext.locationCage) {
        var locationError = triggerHelper.validateDailyHealthLocation(value('Id'), value('date'), extraContext.locationArea || null, extraContext.locationEnclosure || null, extraContext.locationCage || null);
        if (locationError)
            EHR.Server.Utils.addError(scriptErrors, 'Id', locationError, 'ERROR');
    }

    var entryOrder = value('logEntryId') + '|' + value('sortOrder');
    if (value('logEntryId') && value('sortOrder') != null) {
        if (entryOrdersInSave[entryOrder])
            EHR.Server.Utils.addError(scriptErrors, 'sortOrder', 'Another row of this log entry has sort order ' + value('sortOrder'), 'ERROR');
        entryOrdersInSave[entryOrder] = true;
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_UPSERT, 'study', 'dailyHealthLog', function(helper, errors, row, oldRow) {
    var logEntryId = row.logEntryId || (oldRow && oldRow.logEntryId);
    if (logEntryId && entryIdsInSave.indexOf(logEntryId) === -1)
        entryIdsInSave.push(logEntryId);
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_DELETE, 'study', 'dailyHealthLog', function(helper, errors, row) {
    if (row.objectid)
        deletedObjectIds.push(row.objectid);
    if (row.logEntryId && entryIdsInSave.indexOf(row.logEntryId) === -1)
        entryIdsInSave.push(row.logEntryId);
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.COMPLETE, 'study', 'dailyHealthLog', function() {
    if (!entryIdsInSave.length)
        return;

    var entryIds = toJavaStrings(entryIdsInSave);
    triggerHelper.syncDailyHealthObservations(entryIds, toJavaStrings(deletedObjectIds));
    triggerHelper.queueDailyHealthEntryChecks(entryIds);
});

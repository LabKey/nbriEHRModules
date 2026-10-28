/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

function onInit(event, helper){

    helper.decodeExtraContextProperty('protocolAssignmentsInTransaction', []);

    // the shared assignmentsInTransaction processor requires row.project, which a protocol assignment never has
    helper.registerRowProcessor(function(helper, row){
        if (!row || !row.Id || !row.protocol){
            return;
        }

        var inTransaction = helper.getProperty('protocolAssignmentsInTransaction') || [];

        var shouldAdd = true;
        if (row.objectid){
            LABKEY.ExtAdapter.each(inTransaction, function(r){
                if (r.objectid === row.objectid){
                    shouldAdd = false;
                    return false;
                }
            }, this);
        }

        if (shouldAdd){
            inTransaction.push({
                Id: row.Id,
                objectid: row.objectid,
                date: row.date,
                enddate: row.enddate,
                protocol: row.protocol
            });
        }

        helper.setProperty('protocolAssignmentsInTransaction', inTransaction);
    });
}

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// Warns when an assignment would put the protocol over the animals approved for that animal's species. The shared
// EHR check reaches the protocol through a project, which cannot see a protocol assignment here.
EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'protocolAssignment', function (helper, scriptErrors, row, oldRow) {
    if (helper.isETL() || helper.isQuickValidation()) {
        return;
    }

    // on an update, only re-check when the animal or protocol actually changed
    if (oldRow && oldRow.Id && oldRow.Id === row.Id && oldRow.protocol === row.protocol) {
        return;
    }

    if (!row.Id || !row.protocol || !row.date) {
        return;
    }

    var inTransaction = helper.getProperty('protocolAssignmentsInTransaction') || [];

    var msgs = triggerHelper.verifyProtocolCountsForProtocol(row.Id, row.protocol, inTransaction);
    if (msgs) {
        msgs = msgs.split("<>");
        for (var i = 0; i < msgs.length; i++) {
            EHR.Server.Utils.addError(scriptErrors, 'protocol', msgs[i], 'WARN');
        }
    }
});

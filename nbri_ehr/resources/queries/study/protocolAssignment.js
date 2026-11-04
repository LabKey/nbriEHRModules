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
                date: EHR.Server.Utils.datetimeToString(row.date),
                enddate: row.enddate,
                protocol: row.protocol
            });
        }

        helper.setProperty('protocolAssignmentsInTransaction', inTransaction);
    });
}

var protocolChecks = require("nbri_ehr/protocolChecks");
var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// the shared EHR check reaches the protocol through a project, which cannot see a protocol assignment here
EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'protocolAssignment', function (helper, scriptErrors, row, oldRow) {
    if (helper.isETL() || helper.isQuickValidation()) {
        return;
    }

    if (protocolChecks.isUnchanged('protocol', row, oldRow) || !row.Id || !row.protocol || !row.date) {
        return;
    }

    var inTransaction = helper.getProperty('protocolAssignmentsInTransaction') || [];
    protocolChecks.verifyProtocol(triggerHelper, scriptErrors, 'protocol', row, oldRow, triggerHelper.getAnimalSpecies(row.Id),
            row.objectid || (oldRow && oldRow.objectid) || null, inTransaction);
});

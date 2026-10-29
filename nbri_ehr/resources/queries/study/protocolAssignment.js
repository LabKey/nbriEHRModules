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

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

function sameDay(a, b) {
    a = EHR.Server.Utils.normalizeDate(a);
    b = EHR.Server.Utils.normalizeDate(b);
    if (!a || !b) {
        return !a && !b;
    }

    return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

// Rejects an unknown or expired protocol, and warns when an assignment would put the protocol over the animals
// approved for that animal's species. The shared EHR check reaches the protocol through a project, which cannot see
// a protocol assignment here.
EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'protocolAssignment', function (helper, scriptErrors, row, oldRow) {
    if (helper.isETL() || helper.isQuickValidation()) {
        return;
    }

    // an edit that leaves the animal, protocol and date alone (e.g. closing the assignment) changes neither result,
    // unless it is Submit Final, the first point at which the row counts against the limit
    if (oldRow && !row._becomingPublicData && oldRow.Id === row.Id && oldRow.protocol === row.protocol && sameDay(oldRow.date, row.date)) {
        return;
    }

    if (!row.Id || !row.protocol || !row.date) {
        return;
    }

    var protocolError = triggerHelper.verifyProtocolActive(row.protocol, row.date);
    if (protocolError) {
        EHR.Server.Utils.addError(scriptErrors, 'protocol', protocolError, 'ERROR');
        return;
    }

    var species = triggerHelper.getAnimalSpecies(row.Id);
    if (!species) {
        // nothing to count the animal against, so this stays a warning even on Submit Final
        EHR.Server.Utils.addError(scriptErrors, 'protocol', 'Unknown species: ' + row.Id, 'WARN');
        return;
    }

    var inTransaction = helper.getProperty('protocolAssignmentsInTransaction') || [];

    var msgs = triggerHelper.verifyProtocolCountsForProtocol(row.Id, row.protocol, row.date, species, row.objectid || null, inTransaction);
    if (msgs) {
        // the server commits WARN rows, so only an ERROR keeps a public assignment off a full protocol
        var movingPublicRow = row._publicData && oldRow && oldRow.protocol !== row.protocol;
        var severity = (row._becomingPublicData || movingPublicRow) ? 'ERROR' : 'WARN';
        msgs = msgs.split("<>");
        for (var i = 0; i < msgs.length; i++) {
            EHR.Server.Utils.addError(scriptErrors, 'protocol', msgs[i], severity);
        }
    }
});

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);
var protocolData = {};
var prevAnimalId;
var prevDate;

var missing = [];

var count = 0;

function getLastAssignment(id){
    var batchLastDate;

    LABKEY.Query.selectRows({
        schemaName: 'study',
        queryName: 'protocolAssignment',
        columns: 'Id,date',
        filterArray: [LABKEY.Filter.create('Id', id)],
        sort: '-date',
        success: function (results) {
            if (results.rows.length) {
                batchLastDate = results.rows[0].date;
            }
        },
        scope: this
    });

    return batchLastDate;
}

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

    if (helper.isETL()) {
        LABKEY.Query.selectRows({
            schemaName: 'ehr',
            queryName: 'protocol',
            columns: 'title,objectid',
            success: function (results) {
                if (results.rows.length) {
                    for (var i = 0; i < results.rows.length; i++) {
                        let rec = results.rows[i];
                        protocolData[rec.objectid] = rec.title;
                    }
                }
            },
            scope: this
        });
    }
}

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// Warns when an assignment would put the protocol over the animals approved for that animal's species. The shared
// EHR check reaches the protocol through a project, which cannot see a protocol assignment here.
EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'protocolAssignment', function (helper, scriptErrors, row, oldRow) {
    if (helper.isETL() || helper.isQuickValidation()) {
        return;
    }

    // on an update, only re-check when the animal actually changed
    if (oldRow && oldRow.Id && oldRow.Id === row.Id) {
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

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_INSERT, 'study', 'protocolAssignment', function (helper, scriptErrors, row, oldRow) {

    if (helper.isETL()) {
        var isTransfer = prevAnimalId === row.Id;

        if (row.remark) {
            var remarkTextArr = row.remark.split(':');
            var toProtocol = remarkTextArr[3].split(' (')[0]; // Get "To Protocol:" value without segment
            var protocolId;

            protocolId = getProtocolIdByName(toProtocol);
            if (!protocolId || protocolId === 'undefined') {
                if (missing.indexOf(toProtocol) === -1)
                    missing.push(toProtocol)
            }
            else {
                row.protocol = protocolId;
            }
            if (isTransfer) {
                if (row.enddate) {
                    // End date will initially have animal death/departure. Override if the transfer is older.
                    var death = new Date(row.enddate);
                    var prev = new Date(prevDate);

                    // Sanity check
                    if (prev < death) {
                        row.enddate = prevDate;
                    }
                }
                else {
                    row.enddate = prevDate;
                }
            }
            else if (count === 0) {
                // This handles batch boundary row for full truncate ETL, which is the only ETL setup for this currently.
                // Gets previous date from db for first row in batch
                var batchLastDate = getLastAssignment(row.Id);
                if (batchLastDate) {
                    row.enddate = batchLastDate;
                }
            }

            if (row.enddate === 'undefined') {
                console.log("end date not found for animal event - " + row.animalEventId);
            }
        }

        prevAnimalId = row.Id;
        prevDate = row.date;
        count++;
    }

});


function getProtocolIdByName(protocolName) {
    var protocols = Object.keys(protocolData);

    // Search protocols for exact case-insensitive match
    for (var i = 0; i< protocols.length; i++) {
        var pName = protocolData[protocols[i]] ;
        if (pName && protocolName.trim().toLowerCase() === pName.toString().toLowerCase()) {
            return protocols[i];
        }
    }

    // If exact match not found, search for partial match at beginning of protocol
    for (i = 0; i< protocols.length; i++) {
        pName = protocolData[protocols[i]] ;
        if (pName && protocolName.trim().toLowerCase().indexOf(pName.toString().toLowerCase()) === 0) {
            return protocols[i];
        }
    }
}

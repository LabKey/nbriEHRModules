/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);
var protocolData = {};
var prevAnimalId;
var prevDate;

var missing = [];

var count = 0;

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

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

function onComplete(event, errors, helper){

    if (!helper.isValidateOnly() && !helper.isETL()) {
        var updateRows = helper.getRows();

        // When closing previous records, we don't need to update the orchard file. Use the same flag as demographics providers.
        var skipAnnounceChangedParticipants = false;
        var extraContext = helper.getProperty('extraContext');
        if (extraContext) {
            skipAnnounceChangedParticipants = extraContext.skipAnnounceChangedParticipants;
        }

        if (updateRows && updateRows.length > 0 &&
                updateRows[0].row.taskid &&
                updateRows[0].row.QCStateLabel &&
                EHR.Server.Security.getQCStateByLabel(updateRows[0].row.QCStateLabel).PublicData &&
                !skipAnnounceChangedParticipants
        ) {
            triggerHelper.generateOrchardFile(updateRows[0].row.taskid);
        }
    }
}

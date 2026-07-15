/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);
var prevAnimalId;
var prevDate;

let triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);
let animalIds = [];

function onInit(event, helper){
    helper.setScriptOptions({
        skipHousingCheck: true
    });

    helper.decodeExtraContextProperty('housingInTransaction');

    helper.registerRowProcessor(function(helper, row){
        if (!row)
            return;

        if (!row.Id || !row.room){
            return;
        }

        var housingInTransaction = helper.getProperty('housingInTransaction');
        housingInTransaction = housingInTransaction || {};
        housingInTransaction[row.Id] = housingInTransaction[row.Id] || [];

        // this is a failsafe in case the client did not provide housing JSON.  it ensures
        // the current row is part of housingInTransaction
        var shouldAdd = true;
        if (row.objectid){
            LABKEY.ExtAdapter.each(housingInTransaction[row.Id], function(r){
                if (r.objectid == row.objectid){
                    shouldAdd = false;
                    return false;
                }
            }, this);
        }

        if (shouldAdd){
            housingInTransaction[row.Id].push({
                objectid: row.objectid,
                date: row.date,
                enddate: row.enddate,
                qcstate: row.QCState,
                room: row.room,
                cage: row.cage,
                divider: row.divider
            });
        }

        helper.setProperty('housingInTransaction', housingInTransaction);
    });
}

function onUpsert(helper, scriptErrors, row, oldRow){
    //verify we dont have 2 opened records for the same ID
    if (!helper.isETL() && !row.enddate && row.Id){
        var map = helper.getProperty('housingInTransaction');
        if (map && map[row.Id]){
            var housingRecords = map[row.Id];
            for (var i=0;i<housingRecords.length;i++){
                if (row.objectid == housingRecords[i].objectid){
                    console.log ('same housing record');
                    continue;
                }

                if (!housingRecords[i].enddate){
                    EHR.Server.Utils.addError(scriptErrors, 'enddate', 'Cannot enter multiple open-ended housing records for the same animal', 'WARN');
                }
            }
        }
    }

    if (!helper.isETL() && row && row.Id && row.date && !row.enddate){
        var objectid = row.objectid || null;
        //if this record is active and public, deactivate any old housing records
        var map = helper.getProperty('housingInTransaction');
        var housingRecords = [];
        if (map && map[row.Id]){
            housingRecords = map[row.Id];
        }

        //NOTE: downstream java code should handle type conversion of housingInTransaction
        var msg = helper.getJavaHelper().validateFutureOpenEndedHousing(row.Id, row.date, objectid, housingRecords);
        if (msg){
            EHR.Server.Utils.addError(scriptErrors, 'Id', msg, 'WARN');
        }
    }
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_INSERT, 'study', 'housing', function (helper, scriptErrors, row, oldRow) {

        if (helper.isETL()) {
            var isBatch = prevAnimalId === row.Id;

            if (isBatch) {
                row.endDate = prevDate;
            }

            prevAnimalId = row.Id;
            prevDate = row.date;
        }
        else {
            if (row.reason === 'Veterinary Treatment' && !row.remark) {
                EHR.Server.Utils.addError(scriptErrors, 'remark', 'Reason For Move - Veterinary Treatment requires a remark.', 'ERROR');
            }

            if (!helper.isValidateOnly() && row.reason === 'Veterinary Treatment' && (!oldRow || oldRow.reason !== 'Veterinary Treatment')) {
                triggerHelper.clinicalMoveNotification(row.Id, row.date);
            }
        }
    });

function onComplete(event, errors, helper){

    // Similar to EHR housing trigger onComplete with minor differences to handle ETL
    if (!helper.isValidateOnly()) {
        var updateRows = helper.getRows();
        if (updateRows && updateRows.length > 0) {
            // Only need boundary rows for batch. Changing batch size on ETL may require change here. Incremental updates
            // should update all incoming rows so don't do this.
            if (helper.isETL() && updateRows.length > 4000) {
                console.log("Batch import: only closing boundary housing record")
                updateRows = [updateRows[0]];
            }
            var idsToClose = [];
            for (var i = 0; i < updateRows.length; i++) {
                if (EHR.Server.Security.getQCStateByLabel(updateRows[i].row.QCStateLabel).PublicData && updateRows[i].row.date) {
                    idsToClose.push({
                        Id: updateRows[i].row.Id,
                        date: EHR.Server.Utils.datetimeToString(updateRows[i].row.date),  //stringify to serialize properly
                        objectid: updateRows[i].row.objectid
                    });
                    if (animalIds.indexOf(updateRows[i].row.Id) === -1) {
                        animalIds.push(updateRows[i].row.Id);
                    }
                }
            }
            if (idsToClose.length){
                helper.getJavaHelper().closeHousingRecords(idsToClose);
            }
        }
    }
}


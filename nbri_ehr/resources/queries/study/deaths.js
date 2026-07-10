/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);
var validIds = [];
var idMap = {};
var deathIdMap = {};

function onInit(event, helper){

    helper.decodeExtraContextProperty('deathsInTransaction');

    // Cache valid Ids for check on each row
    LABKEY.Query.selectRows({
        requiredVersion: 9.1,
        schemaName: 'study',
        queryName: 'demographics',
        columns: ['Id', 'calculated_status', 'QCState/Label'],
        scope: this,
        success: function (results) {
            if (!results || !results.rows || results.rows.length < 1)
                return;

            for(var i=0; i < results.rows.length; i++) {
                validIds.push(results.rows[i]["Id"]["value"])
                idMap[results.rows[i]["Id"]["value"]] = {calculated_status: results.rows[i]["calculated_status"]["value"], QCStateLabel: results.rows[i]["QCState/Label"]["value"]};
                // console.log(idMap[results.rows[i]["Id"]["value"]]);
            }
        },
        failure: function (error) {
            console.log("error getting demographics data in death trigger onInit()\n" + error);
        }
    });

    LABKEY.Query.selectRows({
        requiredVersion: 9.1,
        schemaName: 'study',
        queryName: 'deaths',
        columns: ['Id', 'QCState/Label'],
        scope: this,
        success: function (results) {
            if (!results || !results.rows || results.rows.length < 1)
                return;

            for(var i=0; i < results.rows.length; i++) {
                deathIdMap[results.rows[i]["Id"]["value"]] = {QCStateLabel: results.rows[i]["QCState/Label"]["value"]};
            }
        },
        failure: function (error) {
            console.log("error getting death data in death trigger onInit()\n" + error);
        }
    });
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_DELETE, 'study', 'Deaths', function(helper, errors, row, oldRow) {

    var demographicsUpdates = [];
    demographicsUpdates.push({
        Id: row.Id,
        death: null,
        calculated_status: 'Alive',
        QCState: helper.getJavaHelper().getQCStateForLabel('Completed').getRowId(),
    });

    console.log('removing demographics death date for animal:' + row.Id);
    helper.getJavaHelper().updateDemographicsRecord(demographicsUpdates);
});

function onUpsert(helper, scriptErrors, row, oldRow) {

    var demographicsUpdates = [];

    if (!helper.isETL()) {

        //skip other checks so that the admins can update a death record
        if (helper.getEvent() === 'update' && LABKEY.Security.currentUser.isAdmin) {
            return;
        }

        //only allow death record to be created if the animal is in the demographics table
        if (idMap[row.Id]) {

            // check if a death record already exists for this animal
            if (idMap[row.Id].calculated_status.toUpperCase() === 'DEAD' && deathIdMap[row.Id].QCStateLabel.toUpperCase() === 'COMPLETED') {
                EHR.Server.Utils.addError(scriptErrors, 'Id', 'Death record already exists for this animal.', 'ERROR');
            }
            // check if the animal is at the center
            else if (idMap[row.Id].calculated_status.toUpperCase() === 'SHIPPED') {
                EHR.Server.Utils.addError(scriptErrors, 'Id', 'Animal is not at the center.', 'ERROR');
            }
            // Check if an animal that's being entered is pending any request/review.
            // Note 1: When trying to enter a new record for an animal, the QCState = 'IN PROGRESS'.
            // Note 2: Upon 'Submit Death', the QCState will get set to 'REQUEST: PENDING', and upon 'Submit Necropsy for Review',
            // the QCState will get set to 'Review Required' - this way we can distinguish between the two states in the Death/Necropsy workflow.
            // If a user tries to submit a new Death record (identified by QCState = 'IN PROGRESS') for an animal that
            // already has a pending request/review status in study.deaths, then below error message will be displayed.
            else if (row.QCStateLabel.toUpperCase() === 'IN PROGRESS' &&
                    deathIdMap[row.Id] && deathIdMap[row.Id].QCStateLabel &&
                    (deathIdMap[row.Id].QCStateLabel.toUpperCase() === 'REQUEST: PENDING' ||
                            deathIdMap[row.Id].QCStateLabel.toUpperCase() === 'REVIEW REQUIRED')) {
                EHR.Server.Utils.addError(scriptErrors, 'Id', 'Death record is pending review for this animal', 'ERROR');
            }
            // if 'Save Draft' record already exists, it doesn't allow to 'Save Draft' or 'Submit Death'
            // on the same animal again - throws an error "duplicate key value violates unique constraint"
            // So, added this check to allow 'Save Draft' record to be saved only once.
            else if (oldRow === undefined && row.QCStateLabel.toUpperCase() === 'IN PROGRESS' &&
                    deathIdMap[row.Id] && deathIdMap[row.Id].QCStateLabel &&
                    deathIdMap[row.Id].QCStateLabel.toUpperCase() === 'IN PROGRESS') {
                EHR.Server.Utils.addError(scriptErrors, 'Id', 'Death/Necropsy data entry is in progress for this animal', 'ERROR');
            }
            else if (!helper.isValidateOnly() && row.Id && row.date && row.QCStateLabel.toUpperCase() === 'COMPLETED') {

                if (validIds.indexOf(row.id) !== -1) {

                    // update demographics
                    demographicsUpdates.push({
                        Id: row.Id,
                        death: row.date,
                        calculated_status: 'Dead',
                        QCState: helper.getJavaHelper().getQCStateForLabel(row.QCStateLabel).getRowId()
                    });

                    console.log('updating demographics death date for animal: ' + row.Id);
                    helper.getJavaHelper().updateDemographicsRecord(demographicsUpdates);
                    console.log('updated demographics death date for animal: ' + row.Id);
                }
                else {
                    console.log(row.id + " is not a valid animal id");
                }
            }

            if(row.QCStateLabel && EHR.Server.Security.getQCStateByLabel(row.QCStateLabel).PublicData) {
                var qcstate = helper.getJavaHelper().getQCStateForLabel(row.QCStateLabel).getRowId();

                //add/update weight record
                var weightRecord = {
                    Id: row.Id,
                    date: row.date,
                    weight: row.deathWeight,
                    taskid: row.taskid,
                    qcstate: qcstate,
                    performedby: row.performedby
                };
                triggerHelper.upsertWeightRecord(weightRecord);
            }
        }
    }
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_INSERT, 'study', 'deaths', function(helper, scriptErrors, row, oldRow) {
    helper.registerDeath(row.Id, row.date);
    triggerHelper.reportDataChange("study", "deaths", [row.Id]);
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.COMPLETE, 'study', 'Deaths', function(event, errors, helper){
    var rows = helper.getRows() || [];
    for (var i = 0; i < rows.length; i++) {
        var row = rows[i].row;
        var oldRow = rows[i].oldRow;

        // Notification will get sent when:
        // 1) a brand-new row saved directly as 'Request: Pending' (i.e., when a user clicks 'Submit Death'), or
        // 2) a draft death record moving from 'In Progress' to 'Request: Pending'.
        if (!helper.isETL() &&
                row && row.Id &&
                row.QCStateLabel &&
                row.QCStateLabel.toUpperCase() === 'REQUEST: PENDING' &&
                (!oldRow || !oldRow.QCStateLabel || oldRow.QCStateLabel.toUpperCase() === 'IN PROGRESS')) {
            console.log("Sending NBRI Death Notification")
            triggerHelper.sendDeathNotification(row.Id);

            console.log("Updating Procedure Orders to Completed for Animal: " + row.Id + "")
            triggerHelper.updateProcedureOrdersToCompleted([row.Id]);
        }
    }
});
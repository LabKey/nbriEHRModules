/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);
var idMap = {};
var deathIdMap = {};
var idsToSync = [];

function onInit(event, helper){

    // the script scope can outlive a single save, so never inherit ids from a prior one
    idsToSync = [];

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
                idMap[results.rows[i]["Id"]["value"]] = {calculated_status: results.rows[i]["calculated_status"]["value"], QCStateLabel: results.rows[i]["QCState/Label"]["value"]};
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
        QCState: helper.getJavaHelper().getQCStateForLabel('Completed').getRowId(),
    });

    console.log('removing demographics death date for animal:' + row.Id);
    helper.getJavaHelper().updateDemographicsRecord(demographicsUpdates);
});

function onUpsert(helper, scriptErrors, row, oldRow) {

    if (!helper.isETL()) {

        //skip other checks so that the admins can update a death record
        if (helper.getEvent() === 'update' && LABKEY.Security.currentUser.isAdmin) {
            return;
        }

        //only allow death record to be created if the animal is in the demographics table
        if (idMap[row.Id]) {

            // deathIdMap has no entry for the animal on initial import, and any of these values can be null
            var status = idMap[row.Id].calculated_status ? idMap[row.Id].calculated_status.toUpperCase() : null;
            var priorDeathQCState = deathIdMap[row.Id] && deathIdMap[row.Id].QCStateLabel ? deathIdMap[row.Id].QCStateLabel.toUpperCase() : null;
            var rowQCState = row.QCStateLabel ? row.QCStateLabel.toUpperCase() : null;
            var demographicsQCState = idMap[row.Id].QCStateLabel ? idMap[row.Id].QCStateLabel.toUpperCase() : null;

            // deathIdMap is a snapshot taken before any row was processed, so it cannot see earlier rows of this same
            // save. Track them separately: study.deaths is demographic, so a second row for one animal cannot be saved.
            var deathsInTransaction = helper.getProperty('deathsInTransaction') || {};

            var errorMsg = null;

            // check if a death record already exists for this animal
            if (status === 'DEAD' && priorDeathQCState === 'COMPLETED') {
                errorMsg = 'Death record already exists for this animal.';
            }
            // check if the animal is at the center
            else if (status === 'SHIPPED') {
                errorMsg = 'Animal is not at the center.';
            }
            // An in-progress demographics record is provisional; completing it would publish unreviewed arrival or
            // birth data. Insert only, so an in-flight death is not trapped. Admins override. Null is not evidence.
            else if (oldRow === undefined && demographicsQCState && demographicsQCState !== 'COMPLETED' && !LABKEY.Security.currentUser.isAdmin) {
                errorMsg = 'Demographics record for this animal is not final (' + idMap[row.Id].QCStateLabel + '). Complete the arrival or birth record before submitting a death.';
            }
            else if (deathsInTransaction[row.Id]) {
                errorMsg = 'This animal is entered more than once. Only one death record per animal can be saved.';
            }
            // A new entry starts at 'IN PROGRESS'; 'Submit Death' sets 'REQUEST: PENDING' and 'Submit Necropsy for
            // Review' sets 'Review Required', so either prior state means the animal is already in the workflow.
            else if (rowQCState === 'IN PROGRESS' &&
                    (priorDeathQCState === 'REQUEST: PENDING' || priorDeathQCState === 'REVIEW REQUIRED')) {
                errorMsg = 'Death record is pending review for this animal';
            }
            // A second draft for the same animal would fail on the unique constraint, so catch it here.
            else if (oldRow === undefined && rowQCState === 'IN PROGRESS' && priorDeathQCState === 'IN PROGRESS') {
                errorMsg = 'Death/Necropsy data entry is in progress for this animal';
            }
            // Catch-all for that constraint. Test existence, not QC state: ETL rows can carry a null QCState.
            else if (oldRow === undefined && deathIdMap[row.Id]) {
                errorMsg = 'A death record already exists for this animal (' + (deathIdMap[row.Id].QCStateLabel || 'unknown state') + ').';
            }

            if (errorMsg) {
                EHR.Server.Utils.addError(scriptErrors, 'Id', errorMsg, 'ERROR');
            }
            else {
                if (!helper.isValidateOnly() && row.date && row.QCStateLabel && EHR.Server.Security.getQCStateByLabel(row.QCStateLabel).PublicData) {
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
                    if (triggerHelper.upsertWeightRecord(weightRecord, false)) {
                        helper.addTableModified('study', 'weight');
                    }
                }

                // mark only rows that passed, so a duplicate of a failed row reports that row's underlying error
                deathsInTransaction[row.Id] = true;
                helper.setProperty('deathsInTransaction', deathsInTransaction);
            }
        }
        // insert-only: updates of existing death records keep their prior behavior
        else if (oldRow === undefined) {
            EHR.Server.Utils.addError(scriptErrors, 'Id', 'Id not found in the demographics table.', 'ERROR');
        }
    }
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_INSERT, 'study', 'deaths', function(helper, scriptErrors, row, oldRow) {
    helper.registerDeath(row.Id, row.date);
    triggerHelper.reportDataChange("study", "deaths", [row.Id]);

    if (row.Id && idsToSync.indexOf(row.Id) === -1) {
        idsToSync.push(row.Id);
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_UPDATE, 'study', 'deaths', function(helper, scriptErrors, row, oldRow) {
    if (row.Id && idsToSync.indexOf(row.Id) === -1) {
        idsToSync.push(row.Id);
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.COMPLETE, 'study', 'Deaths', function(event, errors, helper){

    // The shared ehr script writes demographics.death from the incoming row; this runs after and re-derives it from
    // the stored record, so the event record wins. calculated_status is left to the shared status recalc.
    if (!helper.isETL() && idsToSync.length) {
        var demographicsUpdates = triggerHelper.computeDemographicsSync(idsToSync);
        if (demographicsUpdates.size() > 0) {
            helper.getJavaHelper().updateDemographicsRecord(demographicsUpdates);
        }
        idsToSync = [];
    }

    // A delete arrives here as the deleted row with a null oldRow, which otherwise reads as a draft leaving draft.
    if (event === 'delete')
        return;

    var rows = helper.getRows() || [];
    for (var i = 0; i < rows.length; i++) {
        var row = rows[i].row;
        var oldRow = rows[i].oldRow;

        if (helper.isETL() || !row || !row.Id || !row.QCStateLabel)
            continue;

        // Notify once, on the first non-draft save: 'Submit Death' lands on 'Request: Pending', but a death entered alongside its necropsy goes straight to 'Review Required' or 'Completed'.
        var wasDraft = !oldRow || !oldRow.QCStateLabel || oldRow.QCStateLabel.toUpperCase() === 'IN PROGRESS';
        if (wasDraft && row.QCStateLabel.toUpperCase() !== 'IN PROGRESS') {
            console.log("Sending NBRI Death Notification")
            triggerHelper.sendDeathNotification(row.Id);

            console.log("Updating Procedure Orders to Completed for Animal: " + row.Id + "")
            triggerHelper.updateProcedureOrdersToCompleted([row.Id]);
        }
    }
});
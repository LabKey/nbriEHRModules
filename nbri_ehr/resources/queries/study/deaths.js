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
            else if (deathsInTransaction[row.Id]) {
                errorMsg = 'This animal is entered more than once. Only one death record per animal can be saved.';
            }
            // Check if an animal that's being entered is pending any request/review.
            // Note 1: When trying to enter a new record for an animal, the QCState = 'IN PROGRESS'.
            // Note 2: Upon 'Submit Death', the QCState will get set to 'REQUEST: PENDING', and upon 'Submit Necropsy for Review',
            // the QCState will get set to 'Review Required' - this way we can distinguish between the two states in the Death/Necropsy workflow.
            // If a user tries to submit a new Death record (identified by QCState = 'IN PROGRESS') for an animal that
            // already has a pending request/review status in study.deaths, then below error message will be displayed.
            else if (rowQCState === 'IN PROGRESS' &&
                    (priorDeathQCState === 'REQUEST: PENDING' || priorDeathQCState === 'REVIEW REQUIRED')) {
                errorMsg = 'Death record is pending review for this animal';
            }
            // if 'Save Draft' record already exists, it doesn't allow to 'Save Draft' or 'Submit Death'
            // on the same animal again - throws an error "duplicate key value violates unique constraint"
            // So, added this check to allow 'Save Draft' record to be saved only once.
            else if (oldRow === undefined && rowQCState === 'IN PROGRESS' && priorDeathQCState === 'IN PROGRESS') {
                errorMsg = 'Death/Necropsy data entry is in progress for this animal';
            }
            // study.deaths is demographic (one row per animal), so any other new row for an animal with an existing
            // record would fail on the unique constraint; report it as a validation error instead. Test record
            // existence, not QC state: ETL/import-sourced rows can carry a null QCState.
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

    // Single writer for the denormalized demographics death date. Runs once per save, after the death rows are saved,
    // and derives the value from the stored record rather than from the incoming row. calculated_status is left to the
    // shared status recalc, which owns the death/departure/re-arrival precedence.
    if (!helper.isETL() && idsToSync.length) {
        var demographicsUpdates = triggerHelper.computeDemographicsSync(idsToSync);
        if (demographicsUpdates.size() > 0) {
            helper.getJavaHelper().updateDemographicsRecord(demographicsUpdates);
        }
        idsToSync = [];
    }

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
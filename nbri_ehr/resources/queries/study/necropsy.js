/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);
var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

var deathIdMap = {};

function onInit(event, helper){

    if (!helper.isETL() && event != 'truncate') {
        LABKEY.Query.selectRows({
            requiredVersion: 9.1,
            schemaName: 'study',
            queryName: 'deaths',
            columns: ['Id', 'QCState/Label'],
            scope: this,
            success: function (results) {
                if (!results || !results.rows || results.rows.length < 1)
                    return;

                for (var i = 0; i < results.rows.length; i++) {
                    deathIdMap[results.rows[i]["Id"]["value"]] = {QCStateLabel: results.rows[i]["QCState/Label"]["value"]};
                }
            },
            failure: function (error) {
                console.log("error getting death data in death trigger onInit()\n" + error);
            }
        });
    }
}

function onUpsert(helper, scriptErrors, row, oldRow) {

    if (!helper.isETL()) {

        if (deathIdMap[row.Id] && deathIdMap[row.Id].QCStateLabel &&
                (deathIdMap[row.Id].QCStateLabel.toUpperCase() === 'REQUEST: PENDING' ||
                deathIdMap[row.Id].QCStateLabel.toUpperCase() === 'REVIEW REQUIRED')) {

            if (!row.specimenCondition)
                EHR.Server.Utils.addError(scriptErrors, 'specimenCondition', "'Condition of Specimen' is required", 'ERROR');
            if (!row.physicalCondition)
                EHR.Server.Utils.addError(scriptErrors, 'physicalCondition', "'Physical Condition' is required", 'ERROR');
            if (!row.diagnosis)
                EHR.Server.Utils.addError(scriptErrors, 'diagnosis', "'Diagnosis' is required", 'ERROR');
            if (!row.grossAbnormalities)
                EHR.Server.Utils.addError(scriptErrors, 'grossAbnormalities', "'Gross Abnormalities' is required", 'ERROR');
            if (!row.accessionNumber)
                EHR.Server.Utils.addError(scriptErrors, 'accessionNumber', "'Accession Number' is required", 'ERROR');
        }
    }
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_INSERT, 'study', 'necropsy', function(helper, scriptErrors, row, oldRow) {
    triggerHelper.reportDataChange("study", "necropsy", [row.Id]);
});
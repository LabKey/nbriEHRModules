/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

require("ehr/triggers").initScript(this);

let triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);
let animalIds = [];

function onInit(event, helper){
    helper.setScriptOptions({
        allowAnyId: true,
        requiresStatusRecalc: false,
        allowDatesInDistantPast: true
    });
}

function onUpsert(helper, scriptErrors, row, oldRow){
    //NOTE: this should be getting set by the birth, death, arrival & departure tables
    //ALSO: it should be rare to insert directly into this table.  usually this record will be created by inserting into either birth or arrival
    if (!row.calculated_status && !helper.isETL()){
        row.calculated_status = helper.getJavaHelper().getCalculatedStatusValue(row.Id);
    }

    if (helper.isETL() && row.damSire) {
        let damSire = row.damSire.split('DAM:');
        if (damSire.length === 1) {
            row.damSire.split('DAM');
        }

        if (damSire.length > 1) {
            let damAndSire = damSire[1].split("SIRE:")
            if (damAndSire.length === 1) {
                row.damSire.split('SIRE');
            }
            if (damAndSire[0].trim() === row.Id)
            {
                console.log(row.Id + " listed as it's own dam")
            }
            if (damAndSire[0].trim() !== 'UNK' && damAndSire[0].trim() !== row.Id)
                row.dam = damAndSire[0].trim();

            if (damAndSire.length > 1 && damAndSire[1].trim() === row.Id)
            {
                console.log(row.Id + " listed as it's own sire")
            }
            if (damAndSire.length > 1 && damAndSire[1].trim() !== 'UNK' && damAndSire[1].trim() !== row.Id)
                row.sire = damAndSire[1].trim();

            if (row.sire === row.dam) {
                row.sire = '';
                row.dam = '';
            }
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
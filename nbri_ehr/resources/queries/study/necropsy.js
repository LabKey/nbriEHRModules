/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

function onUpsert(helper, scriptErrors, row){
    if (helper.isETL())
        return;

    if (!row.accessionNo) {
        EHR.Server.Utils.addError(scriptErrors, 'accessionNo', 'Accession number is required', 'ERROR');
    }

    if (row.Id && triggerHelper.necropsyExists(row.Id, row.taskid)) {
        EHR.Server.Utils.addError(scriptErrors, 'Id', 'Necropsy already entered for this animal', 'ERROR');
    }
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_UPSERT, 'study', 'necropsy', function(helper, errors, row){
    triggerHelper.reportDataChange("study", "necropsy", [row.Id]);

    // a draft accession number is not yet the animal's alias
    var qc = row.QCStateLabel ? EHR.Server.Security.getQCStateByLabel(row.QCStateLabel) : null;
    if (!helper.isETL() && !helper.isValidateOnly() && row.Id && row.accessionNo && qc && qc.PublicData) {
        triggerHelper.upsertNecropsyAlias(row.Id, row.date, row.accessionNo, row.QCStateLabel, row.performedby ? String(row.performedby) : null);
    }
});

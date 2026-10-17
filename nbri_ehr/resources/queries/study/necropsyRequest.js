/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

function onUpsert(helper, scriptErrors, row){
    if (helper.isETL() || !row.Id)
        return;

    if ('Dead' !== helper.getJavaHelper().getCalculatedStatusValue(row.Id)) {
        EHR.Server.Utils.addError(scriptErrors, 'Id', 'Animal is not marked as Dead', 'ERROR');
    }

    if (triggerHelper.necropsyRequestExists(row.Id, row.taskid)) {
        EHR.Server.Utils.addError(scriptErrors, 'Id', 'A necropsy has already been requested for this animal', 'ERROR');
    }
}

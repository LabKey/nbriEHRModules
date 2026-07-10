/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.ON_BECOME_PUBLIC, 'study', 'pregnancy', function(scriptErrors, helper, row, oldRow) {
    if (!helper.isETL()) {

        var outcomeRec = {
            Id: row.Id,
            date: row.date,
            result: row.result
        }
        triggerHelper.sendPregnancyOutcomeNotification(row.Id, outcomeRec);
    }
});
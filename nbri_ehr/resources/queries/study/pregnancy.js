/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);
EHR.Server.Utils = require("ehr/utils").EHR.Server.Utils;

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'pregnancy', function(helper, scriptErrors, row, oldRow) {
    if (!helper.isETL() && row.conceptId) {
        if (triggerHelper.totalRecords('nbri_ehr', 'Conception', 'ConceptId', row.conceptId) === 0) {
            EHR.Server.Utils.addError(scriptErrors, 'conceptId', 'This conception Id does not match any conception record', 'WARN');
        }

        //when updating a record that already carries this conception id, the existing row accounts for one match
        var conceptIdThreshold = (oldRow && oldRow.conceptId === row.conceptId) ? 1 : 0;
        if (triggerHelper.totalRecords('study', 'pregnancy', 'conceptId', row.conceptId) > conceptIdThreshold) {
            EHR.Server.Utils.addError(scriptErrors, 'conceptId', 'This conception Id is already used by another pregnancy outcome record', 'INFO');
        }

        if (triggerHelper.totalRecords('study', 'birth', 'conceptId', row.conceptId) > 0) {
            EHR.Server.Utils.addError(scriptErrors, 'conceptId', 'This conception Id is already used by a birth record', 'INFO');
        }
    }
});

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
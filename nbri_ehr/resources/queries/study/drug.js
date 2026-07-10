/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

require("ehr/triggers").initScript(this);

let triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_INSERT, 'study', 'drug', function(helper, scriptErrors, row, oldRow) {
    if(row.treatmentid && row.scheduledDate && triggerHelper.isTreatmentOrderEntered(row.treatmentid, row.scheduledDate)) {
        EHR.Server.Utils.addError(scriptErrors, 'scheduledDate', 'A treatment has already been entered for this order for this date and time.', 'ERROR');
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'drug', function(helper, scriptErrors, row, oldRow) {
    // Max reasonable amount warning. Error when the amount is greater than the max reasonable amount
    if (row.code) {
        let drugFormulary = triggerHelper.getFormularyForDrug(row.code);
        if (drugFormulary && drugFormulary.maxAmount && row.amount > drugFormulary.maxAmount) {
            EHR.Server.Utils.addError(scriptErrors, 'amount', 'Amount is greater than the maximum reasonable amount: ' + drugFormulary.maxAmount, 'ERROR');
        }
    }
    if (row.volume && !row.vol_units) {
        EHR.Server.Utils.addError(scriptErrors, 'volume', 'Units required for volume.', 'ERROR');
    }
});
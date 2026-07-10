/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_INSERT, 'study', 'prc', function(helper, scriptErrors, row, oldRow) {
    if(row.orderid && triggerHelper.isProcedureOrderEntered(row.orderid)) {
        EHR.Server.Utils.addError(scriptErrors, 'orderid', 'Procedure order has already been closed.', 'ERROR');
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.COMPLETE, 'study', 'prc', function (event, errors, helper) {

    if (!helper.isValidateOnly()){
        var rows = helper.getRows();
        var orderIds = [];
        for (var i=0;i<rows.length;i++) {
            var row = rows[i].row;
            if (row.orderid && row.qcstateLabel === 'Completed') {
                orderIds.push(row.orderid);
            }
        }

        if (orderIds.length)
            triggerHelper.markProcedureOrderComplete(orderIds);
    }
});
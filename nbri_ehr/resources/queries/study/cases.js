/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

const console = require("console");
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

function onInit(event, helper) {
    helper.decodeExtraContextProperty('ordersInTransaction');
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'cases', function(helper, errors, row, oldRow){
    if (!helper.isETL()) {
        var error = false;
        if (row.enddate) {
            if (!row.closeRemark) {
                EHR.Server.Utils.addError(errors, 'closeRemark', 'Close remark required when closing a case.', 'ERROR');
                error = true;
            }

            if(!triggerHelper.canCloseCase()) {
                EHR.Server.Utils.addError(errors, 'enddate', 'Veterinarian permission required to close a case.', 'ERROR');
                error = true;
            }
        }

        if (!row.performedby) {
            EHR.Server.Utils.addError(errors, 'performedby', 'Opened By is a required field.', 'ERROR');
            error = true;
        }

        if (!helper.isValidateOnly() && row.caseid && row.enddate && oldRow && (row.enddate != oldRow.enddate)) {
            triggerHelper.closeDailyClinicalObs(row.caseid, row.enddate);
        }

        if (!helper.isValidateOnly() && !error) {
            var qc;
            if (row.QCStateLabel) {
                qc = EHR.Server.Security.getQCStateByLabel(row.QCStateLabel);
            }
            else if (row.QCState) {
                qc = EHR.Server.Security.getQCStateByRowId(row.QCState);
            }

            if (!qc) {
                console.error('Unable to find QCState: ' + row.QCState + '/' + row.QCStateLabel);
            }
            else {

                // Don't allow taking an existing non-draft case back to draft
                if (oldRow && qc.Label == 'In Progress') {
                    var oldQc = EHR.Server.Security.getQCStateByLabel(oldRow.QCStateLabel);
                    if (oldQc.Label != 'In Progress') {
                        EHR.Server.Utils.addError(errors, null, 'Cannot save a draft copy of a case already opened or in review.', 'ERROR');
                    }
                } else if (row.category == 'Clinical' && (qc.Label == 'Completed' || qc.Label == 'Review Required') && row.caseid && row.Id && row.performedby && row.taskid && !row.enddate) {
                    var ordersInTransaction = helper.getProperty('ordersInTransaction');
                    var oit = [];
                    if (ordersInTransaction && ordersInTransaction.length) {
                        oit = ordersInTransaction;
                    }
                    triggerHelper.ensureDailyClinicalObservationOrders(row.Id, row.caseid, row.date, row.performedby, qc.RowId, row.taskid, oit);
                }
            }
        }
    }
});

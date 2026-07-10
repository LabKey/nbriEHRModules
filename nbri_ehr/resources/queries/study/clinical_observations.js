/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

const console = require("console");
require("ehr/triggers").initScript(this);

var animalIdCasesMap = {};
var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

function onInit(event, helper) {
    helper.decodeExtraContextProperty('orderTasksInTransaction');
    triggerHelper.clearScheduledObsTaskMap();
}

function onUpsert(helper, scriptErrors, row, oldRow) {

    if (!helper.isETL() && !helper.isGeneratedByServer()) {

        if (!row.observation && !row.remark) {
            EHR.Server.Utils.addError(scriptErrors, 'observation', 'Must enter an observation or remark', 'WARN');
            EHR.Server.Utils.addError(scriptErrors, 'remark', 'Must enter an observation or remark', 'WARN');
        }

        if (row.category === "Verified Id?" && row.observation === "No" && !row.remark) {
            EHR.Server.Utils.addError(scriptErrors, 'remark', "You selected 'No' for 'Verified Id?', please enter Remark", "WARN");
        }

        if (row.category === "Daily Enrichment" && row.observation === "Other" && !row.remark) {
            EHR.Server.Utils.addError(scriptErrors, 'remark', "You selected 'Other' for 'Daily Enrichment', please enter Remark", "WARN");
        }

        if (row.category === "Alopecia Score" && !row.observation) {
            EHR.Server.Utils.addError(scriptErrors, 'observation', "Score required for 'Alopecia Score'.", "WARN");
        }

        if (row.category === "Alopecia Score" && !row.remark) {
            EHR.Server.Utils.addError(scriptErrors, 'remark', "Remark required for 'Alopecia Score'.", "WARN");
        }

        var yesRemarkRequired = (row.category === "Self Biting Observed" || row.category === "New Injury Observed" || row.category === "Other Stereotopy" || row.category === "Environmental Change" || row.category === "Special Enrichment");
        if (yesRemarkRequired && row.observation === "Yes" && !row.remark) {
            EHR.Server.Utils.addError(scriptErrors, 'remark', "You selected 'Yes' for " + row.category + ", please explain in the Remark", "WARN");
        }

        // Handle scheduled observations
        if (!helper.isValidateOnly() && row.scheduledDate) {
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
                var orderTasks = helper.getProperty('orderTasksInTransaction');
                if (orderTasks && orderTasks.length > 0) {
                    var orderData = triggerHelper.handleScheduledObservations(row, qc.RowId, orderTasks[0]);

                    if (orderData) {
                        row.caseId = orderData.caseId;
                        row.orderid = orderData.orderId;
                        row.area = orderData.area;
                        row.type = orderData.type;
                        row.taskid = orderData.taskId;
                    }
                }
            }
        }
    }
}
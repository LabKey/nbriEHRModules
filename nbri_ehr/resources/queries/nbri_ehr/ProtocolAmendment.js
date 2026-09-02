/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

var RENEWAL_YEARS = 3;

// value -> title for the two seeded lookups, so the workflow reads by name rather than by lookup id
var statusTitles = {};
var typeTitles = {};

function loadLookup(queryName, target) {
    LABKEY.Query.selectRows({
        schemaName: 'ehr_lookups',
        queryName: queryName,
        columns: 'value,title',
        scope: this,
        success: function (results) {
            for (var i = 0; i < results.rows.length; i++) {
                target[results.rows[i].value] = results.rows[i].title;
            }
        }
    });
}

function onInit(event, helper) {
    // amendments are routinely entered after the fact, from a letter that arrived weeks later
    helper.setScriptOptions({
        allowDatesInDistantPast: true,
        requiresStatusRecalc: false
    });

    loadLookup('protocol_state', statusTitles);
    loadLookup('protocol_type', typeTitles);
}

function statusOf(row) {
    return row && row.status ? statusTitles[row.status] : null;
}

function typeOf(row) {
    return row && row.amendmentType ? typeTitles[row.amendmentType] : null;
}

function isRenewal(type) {
    return type === 'Renewal' || type === 'Renewal/Amendment';
}

function changesCounts(type) {
    return type === 'Amendment' || type === 'Renewal/Amendment' || type === 'Original';
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'nbri_ehr', 'ProtocolAmendment', function (helper, scriptErrors, row, oldRow) {
    var status = statusOf(row);
    var type = typeOf(row);
    var priorStatus = statusOf(oldRow);

    if (!status) {
        EHR.Server.Utils.addError(scriptErrors, 'status', 'Amendment status is required.', 'ERROR');
        return;
    }
    if (!type) {
        EHR.Server.Utils.addError(scriptErrors, 'amendmentType', 'Amendment type is required.', 'ERROR');
        return;
    }

    // recording a decision is a separate privilege from entering and submitting the amendment
    var decided = (status === 'Approved' || status === 'Not Approved');
    if (decided && status !== priorStatus && !triggerHelper.canApproveProtocolAmendment()) {
        EHR.Server.Utils.addError(scriptErrors, 'status', 'Protocol amendment approval permission is required to record an IACUC decision.', 'ERROR');
        return;
    }

    if (status === 'Submitted' && !row.submittedDate) {
        row.submittedDate = row.date || new Date();
    }

    if (status === 'Approved') {
        if (!row.approvedDate) {
            EHR.Server.Utils.addError(scriptErrors, 'approvedDate', 'A decision date is required to approve an amendment.', 'ERROR');
        }
        // the letter's date and the date the change takes effect are not always the same
        if (!row.effectiveDate && row.approvedDate) {
            row.effectiveDate = row.approvedDate;
        }
        if (isRenewal(type) && !row.newExpirationDate && row.effectiveDate) {
            row.newExpirationDate = triggerHelper.plusYears(row.effectiveDate, RENEWAL_YEARS);
        }
    }

    if (isRenewal(type) && status === 'Approved' && !row.effectiveDate) {
        EHR.Server.Utils.addError(scriptErrors, 'effectiveDate', 'A renewal needs an effective date to move the protocol approval span.', 'ERROR');
    }

    if (!changesCounts(type) && !isRenewal(type)) {
        EHR.Server.Utils.addError(scriptErrors, 'amendmentType', 'Unrecognized amendment type: ' + type, 'ERROR');
    }
});

// The IACUC decision is carried by the status field alone. QCState governs the data entry lifecycle (draft vs
// submitted vs completed) and deliberately has no part in this: an amendment becoming public data is not an
// approval, and an approval is not a QC state transition.
EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.AFTER_UPSERT, 'nbri_ehr', 'ProtocolAmendment', function (helper, errors, row, oldRow) {
    if (helper.isValidateOnly()) {
        return;
    }

    var status = statusOf(row);
    var priorStatus = statusOf(oldRow);

    // fire once, on the transition into Approved
    if (status !== 'Approved' || priorStatus === 'Approved') {
        return;
    }

    triggerHelper.applyApprovedAmendment(
            row.protocol,
            row.objectid,
            row.effectiveDate || null,
            row.newExpirationDate || null,
            isRenewal(typeOf(row)));
});

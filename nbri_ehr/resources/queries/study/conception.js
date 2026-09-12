/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// conception ids claimed by the rows of this save that have already been validated.  Rows entered together are not in
// study.conception yet when each one is checked, so this is the only way the one-record-per-conception rule can see them.
var conceptIdsInSave = [];

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'conception', function(event, helper){
    helper.setScriptOptions({
        // gestation puts the conception date months behind the entry date, so a distant past date is the normal case
        allowDatesInDistantPast: true,
        allowDeadIds: true,
        skipHousingCheck: true,
        skipAssignmentCheck: true
    });

    // the script scope can outlive a single save, so never inherit ids from a prior one
    conceptIdsInSave = [];
});

// conceptId was a unique constraint before this became a dataset, and the birth and pregnancy triggers still resolve a
// conception by that id alone, so the rule is enforced here now.  Unlike the sibling checks in birth.js and
// pregnancy.js this one does not exempt ETL: getConceptionDam() reads the dam with getObject(), which throws on a
// second match, so a duplicate from any write path breaks later birth and pregnancy saves.
EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'conception', function(helper, scriptErrors, row, oldRow) {
    if (!row.conceptId)
        return;

    //when updating a record that already carries this conception id, the existing row accounts for one match
    var conceptIdThreshold = (oldRow && oldRow.conceptId === row.conceptId) ? 1 : 0;
    var claimedBySavedRow = triggerHelper.totalRecords('study', 'conception', 'conceptId', row.conceptId) > conceptIdThreshold;

    // rows are validated one at a time and collected below, so this list holds the earlier rows of this save only
    var claimedByEarlierRow = conceptIdsInSave.indexOf(row.conceptId) > -1;

    if (claimedBySavedRow || claimedByEarlierRow) {
        EHR.Server.Utils.addError(scriptErrors, 'conceptId', 'This conception Id is already used by another conception record', 'ERROR');
    }

    conceptIdsInSave.push(row.conceptId);
});

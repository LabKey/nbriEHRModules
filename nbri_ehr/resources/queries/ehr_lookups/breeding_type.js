/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var LABKEY = require("labkey");

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// The demographics cache holds group titles (ActiveGroupsDemographicsProvider) and only refreshes per animal, so a
// renamed group would show its old title until the cache expired.
var titleChanged = false;

function init(event, errors){
    titleChanged = false;
}

function afterUpdate(row, oldRow, errors){
    if (row.title !== oldRow.title)
        titleChanged = true;
}

function complete(event, errors){
    if (titleChanged)
        triggerHelper.recalcCachedDemographicsAfterCommit('ehr_lookups', 'breeding_type');
}

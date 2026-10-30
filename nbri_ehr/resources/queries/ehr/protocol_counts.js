/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// Rhino hands Java an undefined argument as an object ConvertHelper can't read
function orNull(value) {
    return value === undefined ? null : value;
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'ehr', 'protocol_counts', function (helper, scriptErrors, row) {
    if (!row.amendmentId || !row.species) {
        return;
    }

    // rows saved together are not in the table yet, so the database check alone misses a duplicate within one form
    var seen = helper.getProperty('protocolCountSpecies') || {};
    var key = row.amendmentId + '|' + row.species;

    if (seen[key] || triggerHelper.hasProtocolCount(row.amendmentId, row.species, orNull(row.rowid))) {
        EHR.Server.Utils.addError(scriptErrors, 'species', 'Species ' + triggerHelper.getSpeciesCommonName(row.species) + ' is entered more than once on this amendment.', 'ERROR');
    }

    seen[key] = true;
    helper.setProperty('protocolCountSpecies', seen);
});

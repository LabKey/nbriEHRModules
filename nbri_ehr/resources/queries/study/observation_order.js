/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPDATE, 'study', 'observation_order', function(helper, scriptErrors, row, oldRow){
    // Closing or reopening a case sets its orders' end dates mid-save, then the form's stale copy of each order saves with its old end date; other callers may change it
    var caseid = row.caseid || oldRow.caseid;
    var oldEnd = EHR.Server.Utils.normalizeDate(oldRow.enddate, true);
    var newEnd = EHR.Server.Utils.normalizeDate(row.enddate, true);
    var objectid = row.objectid || oldRow.objectid;
    var setInThisSave = objectid && triggerHelper.isOrderEndSetByCase(objectid);
    // A later save from the same page can still carry the end date from before a close
    var staleAfterClose = oldEnd && (!newEnd || newEnd.getTime() > oldEnd.getTime()) && caseid && triggerHelper.isCaseClosed(caseid);
    if (helper.getImportPathway() === 'ehr-ext4DataEntry' && (setInThisSave || staleAfterClose)){
        row.enddate = oldRow.enddate;
    }
});

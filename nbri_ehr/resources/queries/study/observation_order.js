/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPDATE, 'study', 'observation_order', function(helper, scriptErrors, row, oldRow){
    // Closing a case ends its orders mid-save, then the form's stale copy of each order saves with a blank end date
    var caseid = row.caseid || oldRow.caseid;
    if (!helper.isETL() && !row.enddate && oldRow.enddate && caseid && triggerHelper.isCaseClosed(caseid)){
        row.enddate = oldRow.enddate;
    }
});

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

function onUpsert(helper, scriptErrors, row){
    if (helper.isETL() || row.causeCategory)
        return;

    // a draft may leave it blank, but WARN still blocks Submit Final; only a public save must reject it
    var qc = row.QCStateLabel ? EHR.Server.Security.getQCStateByLabel(row.QCStateLabel) : null;
    EHR.Server.Utils.addError(scriptErrors, 'causeCategory', 'Cause of death category is required', qc && qc.PublicData ? 'ERROR' : 'WARN');
}
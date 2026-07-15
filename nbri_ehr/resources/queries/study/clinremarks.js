/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */


const console = require("console");
require("ehr/triggers").initScript(this);

function onInit(event, helper) {
    helper.decodeExtraContextProperty('casesInTransaction');
}

function onUpsert(helper, scriptErrors, row, oldRow) {
    if (!helper.isETL()) {

        var caseMap = helper.getProperty('casesInTransaction');
        var remarkCase = [];
        if (caseMap && caseMap[row.Id]){
            remarkCase = caseMap[row.Id];
        }

        var hasCaseEnddate = remarkCase.length > 0 && remarkCase[0].enddate

        if (!row.s && !row.o && !row.a && !row.p && !row.remark && !hasCaseEnddate) {
            EHR.Server.Utils.addError(scriptErrors, 'remark', 'Must enter at least one comment', 'WARN');
            EHR.Server.Utils.addError(scriptErrors, 's', 'Must enter at least one comment', 'WARN');
        }
    }
}
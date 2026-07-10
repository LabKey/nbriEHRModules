/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var prevRow;

function getEnddate(row, enddate){
    var closeDate = new Date(enddate);

    if (row.enddate) {
        var deathOrDep = new Date(row.enddate);

        if (deathOrDep < closeDate)
            return deathOrDep;
    }
    return EHR.Server.Utils.datetimeToString(closeDate);
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_INSERT, 'nbri_ehr', 'casesTemp', function (helper, scriptErrors, row, oldRow) {

    // Align resolved cases with case openings
    if (helper.isETL()) {
        if (prevRow && prevRow.Id == row.Id) {
            if (row.category == "Presenting Diagnosis" && prevRow.category == "Presenting Diagnosis") {
                row.openRemark = "Error: Consecutive Presenting Diagnosis"
            }
            else if (row.category == "Clinical Resolution" && prevRow.category == "Clinical Resolution") {
                row.openRemark = "Error: Consecutive Clinical Resolution"
            }
            else if (row.category == "Presenting Diagnosis") {
                row.closeRemark = prevRow.closeRemark || null;
                row.closeDiagnosis = prevRow.closeDiagnosis || null;
                row.enddate = getEnddate(row, prevRow.date);
            }
        }

        prevRow = row;
    }
});
/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var EHR_UTILS = require("ehr/utils").EHR.Server.Utils;

function startOfDay(date) {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}

// Rejects a project or protocol assignment dated on or after the animal's death day
function verifyBeforeDeath(helper, scriptErrors, row, label) {
    if (helper.isETL() || !row.Id || !row.date) {
        return;
    }

    EHR_UTILS.findDemographics({
        participant: row.Id,
        helper: helper,
        scope: this,
        callback: function (data) {
            var death = data && data.death ? EHR_UTILS.normalizeDate(data.death) : null;
            var date = EHR_UTILS.normalizeDate(row.date);
            // Rejects the whole death day; compare full timestamps instead to allow assignments earlier that day
            if (death && date && startOfDay(date) >= startOfDay(death)) {
                EHR_UTILS.addError(scriptErrors, 'date', label + ' cannot be assigned on or after the animal\'s death date: ' + EHR_UTILS.datetimeToString(death), 'ERROR');
            }
        }
    });
}

exports.verifyBeforeDeath = verifyBeforeDeath;

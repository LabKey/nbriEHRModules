/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var EHR_UTILS = require("ehr/utils").EHR.Server.Utils;

function toJsDate(javaDate) {
    return javaDate ? new Date(javaDate.getTime()) : null;
}

// Rejects a housing or assignment record that starts on or after the animal's death or departure
function verifyAnimalPresent(helper, scriptErrors, row, oldRow, label) {
    if (helper.isETL() || !row.Id || !row.date) {
        return;
    }

    var date = EHR_UTILS.normalizeDate(row.date);
    if (!date) {
        return;
    }

    // an edit that keeps the start date is not rechecked, so legacy date-only records stay editable
    var oldDate = oldRow && oldRow.date ? EHR_UTILS.normalizeDate(oldRow.date) : null;
    if (oldDate && oldDate.getTime() === date.getTime()) {
        return;
    }

    var ar = helper.getJavaHelper().getDemographicRecord(row.Id);
    if (!ar) {
        return;
    }

    var death = toJsDate(ar.getDeath());
    if (death && date.getTime() >= death.getTime()) {
        EHR_UTILS.addError(scriptErrors, 'date', label + ' cannot start on or after the animal\'s death date: ' + EHR_UTILS.datetimeToString(death), 'ERROR');
        return;
    }

    // a rearrival marks the animal Alive before its arrival is on record, so status decides whether the departure still applies
    var departure = toJsDate(ar.getMostRecentDeparture());
    var arrival = toJsDate(ar.getMostRecentArrival());
    var status = String(ar.getCalculatedStatus() || '');
    var departed = departure && (!arrival || arrival.getTime() < departure.getTime()) && status.indexOf('Alive') !== 0;
    if (departed && date.getTime() >= departure.getTime()) {
        EHR_UTILS.addError(scriptErrors, 'date', label + ' cannot start on or after the animal\'s departure date: ' + EHR_UTILS.datetimeToString(departure), 'ERROR');
    }
}

exports.verifyAnimalPresent = verifyAnimalPresent;

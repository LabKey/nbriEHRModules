/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var EHR_UTILS = require("ehr/utils").EHR.Server.Utils;

function toJsDate(javaDate) {
    return javaDate ? new Date(javaDate.getTime()) : null;
}

// Rejects the whole day of the death or departure; compare full timestamps instead to allow records earlier that day
function isOnOrAfter(date, event) {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime() >=
            new Date(event.getFullYear(), event.getMonth(), event.getDate()).getTime();
}

// Rejects a housing or assignment record that starts on or after the animal's death or departure
function verifyAnimalPresent(helper, scriptErrors, row, label) {
    if (helper.isETL() || !row.Id || !row.date) {
        return;
    }

    var ar = helper.getJavaHelper().getDemographicRecord(row.Id);
    var date = EHR_UTILS.normalizeDate(row.date);
    if (!ar || !date) {
        return;
    }

    var death = toJsDate(ar.getDeath());
    if (death && isOnOrAfter(date, death)) {
        EHR_UTILS.addError(scriptErrors, 'date', label + ' cannot start on or after the animal\'s death date: ' + EHR_UTILS.datetimeToString(death), 'ERROR');
        return;
    }

    // a rearrival sets the status to Alive before it opens its housing and assignments, ahead of its arrival being on record
    var departure = toJsDate(ar.getMostRecentDeparture());
    var arrival = toJsDate(ar.getMostRecentArrival());
    var status = String(ar.getCalculatedStatus() || '');
    var departed = departure && (!arrival || arrival.getTime() < departure.getTime()) && status.indexOf('Alive') !== 0;
    if (departed && isOnOrAfter(date, departure)) {
        EHR_UTILS.addError(scriptErrors, 'date', label + ' cannot start on or after the animal\'s departure date: ' + EHR_UTILS.datetimeToString(departure), 'ERROR');
    }
}

exports.verifyAnimalPresent = verifyAnimalPresent;

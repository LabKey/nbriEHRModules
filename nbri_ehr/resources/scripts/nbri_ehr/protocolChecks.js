/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var EHR_UTILS = require("ehr/utils").EHR.Server.Utils;

function sameDay(a, b) {
    a = EHR_UTILS.normalizeDate(a);
    b = EHR_UTILS.normalizeDate(b);
    if (!a || !b) {
        return !a && !b;
    }

    return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
}

// an edit that leaves the animal, protocol and date alone (e.g. closing the assignment) changes neither result,
// unless it is Submit Final, the first point at which the row counts against the limit
function isUnchanged(fieldName, row, oldRow) {
    return !!oldRow && !row._becomingPublicData && oldRow.Id === row.Id && oldRow[fieldName] === row[fieldName] && sameDay(oldRow.date, row.date);
}

// Rejects an unknown or expired protocol, and reports an assignment of a species the protocol is not approved for or
// that would put the protocol over the animals approved for that species. The shared EHR check reaches the protocol
// through a project, which cannot see a protocol assignment here.
function verifyProtocol(triggerHelper, scriptErrors, fieldName, row, oldRow, species, objectId, inTransaction) {
    var protocol = row[fieldName];
    var protocolError = triggerHelper.verifyProtocolActive(protocol, row.date);
    if (protocolError) {
        EHR_UTILS.addError(scriptErrors, fieldName, protocolError, 'ERROR');
        return;
    }

    // the server commits WARN rows, so only an ERROR keeps a public assignment off a protocol; a public row that gets
    // this far has a new animal, protocol or date
    var severity = (row._becomingPublicData || (row._publicData && oldRow)) ? 'ERROR' : 'WARN';

    if (!species) {
        // approval is per species, so an animal of unknown species cannot be shown to be approved
        EHR_UTILS.addError(scriptErrors, fieldName, 'Unknown species: ' + row.Id, severity);
        return;
    }

    var msgs = triggerHelper.verifyProtocolCountsForProtocol(row.Id, protocol, row.date, species, objectId, inTransaction);
    if (msgs) {
        msgs = msgs.split("<>");
        for (var i = 0; i < msgs.length; i++) {
            EHR_UTILS.addError(scriptErrors, fieldName, msgs[i], severity);
        }
    }
}

// the protocol assignment an arrival or birth creates skips the protocol checks, so they run on the parent row against
// the entered protocol; inSave collects this save's rows, since a validate-only request writes no protocol assignment
function verifyEnteredProtocol(triggerHelper, helper, scriptErrors, fieldName, row, oldRow, species, inSave) {
    var protocol = row[fieldName];
    if (helper.isETL() || helper.isQuickValidation() || !protocol || !row.Id || !row.date)
        return;

    inSave.push({Id: row.Id, date: EHR_UTILS.datetimeToString(row.date), protocol: protocol, species: species});

    if (isUnchanged(fieldName, row, oldRow))
        return;

    verifyProtocol(triggerHelper, scriptErrors, fieldName, row, oldRow, species, null, inSave);
}

exports.isUnchanged = isUnchanged;
exports.verifyProtocol = verifyProtocol;
exports.verifyEnteredProtocol = verifyEnteredProtocol;

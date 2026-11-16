/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

function roundWeight(row, fieldName) {
    var value = row[fieldName];
    // Number() turns blank strings and booleans into 0 or 1; leave them for type conversion to reject
    if (value === null || value === undefined || typeof value === 'boolean' || (typeof value === 'string' && value.trim() === '')) {
        return;
    }

    var number = Number(value);
    if (!isFinite(number)) {
        return;
    }

    row[fieldName] = Math.round(number * 1000) / 1000;
}

exports.roundWeight = roundWeight;

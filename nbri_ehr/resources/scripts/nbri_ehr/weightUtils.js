/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

function roundWeight(row, fieldName) {
    var value = row[fieldName];
    if (value === null || value === undefined || value === '' || isNaN(value)) {
        return;
    }

    row[fieldName] = Math.round(Number(value) * 1000) / 1000;
}

exports.roundWeight = roundWeight;

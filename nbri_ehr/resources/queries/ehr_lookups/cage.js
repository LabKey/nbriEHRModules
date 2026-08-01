/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var LABKEY = require("labkey");

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// 'location' is not user editable, so it is absent from the incoming row map and the value this script
// derives has nowhere to land. Declaring it managed reserves a slot so the derived key is persisted.
function managedColumns() {
    return {
        insert: ["location"],
        update: ["location"],
    };
}

function onUpsert(row, oldRow, errors){
    if (extraContext.dataSource != "etl") {
        if (!row.location) {
            if (oldRow && oldRow.location && oldRow.location[0]) {
                row.location = oldRow.location[0];
                return;
            }

            if (!row.room) {
                errors['room'] = 'Room is required.';
                return;
            }

            row.location = row.room;
            if (row.cage)
                row.location += '-' + row.cage;
        }
    }
}

function beforeInsert(row, errors){
    onUpsert(row, undefined, errors);
}

function beforeUpdate(row, oldRow, errors){
    onUpsert(row, oldRow, errors);
}

function beforeDelete(row, errors) {
    if (extraContext.dataSource != "etl") {
        if (!row.location) {
            errors[null] = 'Location is required.';
        }
        else {
            let currentlyHoused = triggerHelper.totalHousingRecords(row.location);
            if (currentlyHoused > 0) {
                errors[null] = 'Cannot delete. There are ' + currentlyHoused + ' animals currently housed in this cage.';
            }
        }
    }
}
/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var LABKEY = require("labkey");
var console = require("console");

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// Width of ehr_lookups.rooms.room. The derived key is built from values the user supplies, so it can overrun the
// column; reject it here rather than letting the database raise an unreadable error.
var MAX_ROOM_LENGTH = 100;

// 'room' is not user editable, so it is absent from the incoming row map and the value this script derives has
// nowhere to land. Declaring it managed reserves a slot so the derived key is persisted.
function managedColumns() {
    return {
        insert: ["room"],
        update: ["room"],
    };
}

function onUpsert(row, oldRow, errors){
    if (extraContext.dataSource != "etl") {
        if (!row.name) {
            errors['name'] = 'Room name is required.';
            return;
        }

        if (!row.building) {
            errors['building'] = 'Building is required.';
            return;
        }

        if (!row.room) {
            if (oldRow && oldRow.room && oldRow.room[0]) {
                row.room = oldRow.room[0];
                return;
            }

            let room = row.building + '-' + row.name;
            if (room.length > MAX_ROOM_LENGTH) {
                errors['name'] = 'Building and room name are too long: they combine to a ' + room.length + ' character room key, which cannot exceed ' + MAX_ROOM_LENGTH + '.';
                return;
            }

            row.room = room;
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
        if (!row.room) {
            errors[null] = 'Room is required.';
        }
        else {
            let totalRecords = triggerHelper.totalRecords("ehr_lookups", "cage", "room", row.room);
            if (totalRecords > 0) {
                errors[null] = 'Cannot delete. There are ' + totalRecords + ' cages currently registered in this room. Delete cages before deleting this room.';
            }
        }
    }
}
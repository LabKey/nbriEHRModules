/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var LABKEY = require("labkey");
var console = require("console");

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

function onUpsert(row, oldRow, errors){
    if (extraContext.dataSource != "etl") {
        if (!row.name) {
            errors['name'] = 'Room name is required.';
            return;
        }

        if (!row.floor) {
            errors['floor'] = 'Floor is required.';
            return;
        }

        if (!row.room) {
            if (oldRow && oldRow.room && oldRow.room[0]) {
                row.room = oldRow.room[0];
                return;
            }

            row.room = row.name + '-' + row.floor;
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
/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var LABKEY = require("labkey");

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

function onUpsert(row, oldRow, errors){
    if (extraContext.dataSource != "etl") {
        if (!row.name) {
            errors['name'] = 'Floor name is required.';
            return;
        }

        if (!row.building) {
            errors['building'] = 'Building is required.';
            return;
        }

        if (!row.floor) {
            if (oldRow && oldRow.floor && oldRow.floor[0]) {
                row.floor = oldRow.floor[0];
                return;
            }

            row.floor = row.name + '-' + row.building;
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
        if (!row.floor) {
            errors[null] = 'Floor is required.';
        }
        else {
            let totalRecords = triggerHelper.totalRecords("ehr_lookups", "rooms", "floor", row.floor);
            if (totalRecords > 0) {
                errors[null] = 'Cannot delete. There are ' + totalRecords + ' rooms currently registered on this floor. Delete rooms before deleting this floor.';
            }
        }
    }
}
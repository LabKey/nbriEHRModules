/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var LABKEY = require("labkey");

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

// Width of ehr_lookups.buildings.name. The description it is derived from is a wider column, so it can overrun the
// key; reject it here rather than letting the database raise an unreadable error.
var MAX_NAME_LENGTH = 100;

// 'name' is not user editable, so it is absent from the incoming row map and the value this script derives has
// nowhere to land. Declaring it managed reserves a slot so the derived key is persisted.
function managedColumns() {
    return {
        insert: ["name"],
        update: ["name"],
    };
}

function onUpsert(row, oldRow, errors){
    if (extraContext.dataSource != "etl") {
        if (!row.description) {
            errors['description'] = 'Building description is required.';
            return;
        }

        if (!row.area) {
            errors['area'] = 'Area is required.';
            return;
        }

        if (!row.name) {
            if (oldRow && oldRow.name && oldRow.name[0]) {
                row.name = oldRow.name[0];
                return;
            }

            if (row.description.length > MAX_NAME_LENGTH) {
                errors['description'] = 'Description is too long: it becomes the building key, which cannot exceed ' + MAX_NAME_LENGTH + ' characters.';
                return;
            }

            // The description alone identifies the building now that the area is no longer folded in, so a duplicate
            // would collide on the key. Say so here instead of surfacing a constraint violation on a hidden column.
            if (triggerHelper.totalRecords("ehr_lookups", "buildings", "name", row.description) > 0) {
                errors['description'] = 'A building described as ' + row.description + ' already exists. Building descriptions must be unique.';
                return;
            }

            row.name = row.description;
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
        if (!row.name) {
            errors[null] = 'Building name is required.';
        }
        else {
            let totalRecords = triggerHelper.totalRecords("ehr_lookups", "floors", "building", row.name);
            if (totalRecords > 0) {
                errors[null] = 'Cannot delete. There are ' + totalRecords + ' floors currently registered in this building. Delete floors before deleting this building.';
            }
        }
    }
}
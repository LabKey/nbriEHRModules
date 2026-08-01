/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
var LABKEY = require("labkey");

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);

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
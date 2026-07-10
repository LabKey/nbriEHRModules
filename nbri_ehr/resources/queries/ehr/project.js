/*
 * Copyright (c) 2024 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

require("ehr/triggers").initScript(this);
var nextProjectNum = -1;

function onInit(event, helper){

    //ETL'd data has integer values in the 'project' column that is meaningless to the client,
    //but since 'project' is a required field, find the highest num in the existing data and add 1 for inserting new Projects.
    LABKEY.Query.executeSql({
        schemaName: 'ehr',
        sql: 'SELECT MAX(project) AS latestProjectNum FROM ehr.project',
        containerFilter: LABKEY.Query.containerFilter.allFolders,
        success: function(data) {
            nextProjectNum = data.rows[0].latestProjectNum + 1;
        },
        failure: function (error) {
            console.log("Error getting the latest 'Project Id' in project trigger onInit()\n" + error);
        }
    });
}
function onUpsert(helper, scriptErrors, row, oldRow) {
    if (!helper.isETL()) {
        if (!row.project) {
            row.project = nextProjectNum;
        }
    }
}
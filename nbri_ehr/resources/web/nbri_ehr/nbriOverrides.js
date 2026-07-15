/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.Utils.rowEditorPlugin = 'NBRI_EHR.plugin.RowEditor';

EHR.Utils.editUIButtonHandler = function(schemaName, queryName, dataRegionName, paramMap, copyFilters){
    var params = {
        schemaName: schemaName,
        'query.queryName': queryName,
        showImport: false
    };

    this.editUIButtonCore(schemaName, queryName, dataRegionName, paramMap, undefined, params);
};
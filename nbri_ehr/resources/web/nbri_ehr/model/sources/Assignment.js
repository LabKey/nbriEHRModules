/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

EHR.model.DataModelManager.registerMetadata('Assignment', {
    byQuery: {
        'study.assignment': {
            // a new project assignment ends the open one automatically, so the end date is never entered here
            'enddate': {
                hidden: true
            },
            'project': {
                xtype: 'combo',
                nullable: false,
                lookup: {
                    schemaName: 'ehr',
                    queryName: 'project',
                    keyColumn: 'project',
                    columns: 'project,name',
                    filterArray: [
                        LABKEY.Filter.create('isActive', true, LABKEY.Filter.Types.EQUAL),
                    ]
                }
            }
        },
        'study.protocolAssignment': {
            'enddate': {
                hidden: true
            },
            'project': {
              hidden: true
            },
            'protocol': {
                xtype: 'combo',
                nullable: false,
                columnConfig: {
                    fixed: true,
                    width: 150
                },
                // set displayColumn: ehr.protocol's title column (displayName) is not returned by this query
                lookup: {
                    schemaName: 'ehr',
                    queryName: 'activeProtocols',
                    keyColumn: 'protocol',
                    displayColumn: 'protocol',
                    columns: 'protocol,title'
                }
            }
        }
    }
});
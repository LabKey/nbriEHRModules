/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

EHR.model.DataModelManager.registerMetadata('Rearrival', {

    byQuery: {
        'study.arrival': {
            rearrival: {
                getInitialValue: function (v, rec) {
                    return true
                },
                editable: false,
                hidden: true,
                columnConfig: {
                    editable: false
                }
            },
            performedby: {
                hidden: true,
                showInGrid: false
            },
            sourceFacility: {
                allowBlank: false,
                columnConfig: {
                    fixed: true,
                    width: 150
                },
            },
            // a rearrival is a return, not a fresh acquisition, so neither field applies to it
            acquisitionType: {
                allowBlank: true,
                hidden: true,
                showInGrid: false
            },
            CITES: {
                allowBlank: true,
                hidden: true,
                showInGrid: false
            },
            arrivalType: {
                allowBlank: false,
                columnConfig: {
                    width: 200
                }
            },
            'cage': {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    fixed: true,
                    width: 200
                },
            },
            // the animal's departure closed its project, protocol and group assignments, so each is opened again here
            project: {
                xtype: 'combo',
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    fixed: true,
                    width: 150
                },
                lookup: {
                    schemaName: 'ehr',
                    queryName: 'project',
                    keyColumn: 'project',
                    columns: 'project,name',
                    filterArray: [
                        LABKEY.Filter.create('isActive', true, LABKEY.Filter.Types.EQUAL),
                    ]
                }
            },
            arrivalProtocol: {
                xtype: 'combo',
                allowBlank: false,
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
            },
            groupId: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 200
                },
                lookup: {
                    // the shared default filters on a date column that the breeding type lookup does not have
                    filterArray: []
                }
            },
        }
    }
});

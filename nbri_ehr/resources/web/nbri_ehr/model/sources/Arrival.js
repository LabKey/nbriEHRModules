/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.onReady(function() {
    // this is to skip Id not found warning during weights entry in Arrival data entry form
    if (EHR.data.DataEntryClientStore) {
        Ext4.override(EHR.data.DataEntryClientStore, {
            getExtraContext: function(){
                return {
                    skipIdNotFoundError: {'form': 'arrival'}
                }
            }
        });
    }
});

EHR.model.DataModelManager.registerMetadata('Arrival', {
    byQuery: {
        'study.arrival': {
            'cage': {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    fixed: true,
                    width: 200
                },
            },
            'Id/demographics/species': {
                columnConfig: {
                    fixed: true,
                    width: 200
                },
                allowBlank: false
            },
            'Id/demographics/birth': {
                // allowBlank: false
            },
            'Id/demographics/gender': {
                allowBlank: false
            },
            'Id/demographics/geographic_origin': {
                // allowBlank: false,
                columnConfig: {
                    fixed: true,
                    width: 200
                }
            },
            // the social code is recorded once per animal, at birth or arrival, and lives on demographics
            'Id/demographics/socialCode': {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    fixed: true,
                    width: 200
                }
            },
            // an arriving animal establishes its own lineage, so it starts at generation 0
            'Id/demographics/generation': {
                allowBlank: false,
                nullable: false,
                getInitialValue: function(v) {
                    return Ext4.isEmpty(v) ? 0 : v;
                },
                editorConfig: {
                    minValue: 0
                },
                columnConfig: {
                    fixed: true,
                    width: 120
                }
            },
            // an animal joins the colony already assigned to a project, a protocol and a group; the trigger script
            // opens the matching assignment record for each one
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
            acquisitionType: {
                // allowBlank: false,
                columnConfig: {
                    fixed: true,
                    width: 150
                },
            },
            arrivalType: {
                // allowBlank: false,
                columnConfig: {
                    width: 200
                }
            },
            rearrival: {
                allowBlank: true,
                hidden: true,
                showInGrid: false
            }
        }
    }
});
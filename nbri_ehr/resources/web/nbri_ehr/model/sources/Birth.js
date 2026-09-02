/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.onReady(function() {
    // this is to skip Id not found warning during weights entry in Birth data entry form
    if (EHR.data.DataEntryClientStore) {
        Ext4.override(EHR.data.DataEntryClientStore, {
            getExtraContext: function(){
                return {
                    skipIdNotFoundError: {'form': 'birth'}
                }
            }
        });
    }
});

EHR.model.DataModelManager.registerMetadata('Birth', {
    byQuery: {
        'study.birth': {
            Id: {
                allowBlank: false,
                nullable: false
            },
            date: {
                allowBlank: false,
                nullable: false
            },
            // conception Id, species, dam and sire all come from the conception picked in the Start with Conception
            // window, so they are shown but not entered by hand.  That window writes to the store directly, which is
            // unaffected by these read-only editor settings.
            'Id/demographics/species': {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    fixed: true,
                    width: 250,
                    editable: false
                },
                formEditorConfig: {
                    readOnly: true
                }
            },
            'Id/demographics/dam': {
                columnConfig: {
                    editable: false
                },
                formEditorConfig: {
                    readOnly: true
                }
            },
            'Id/demographics/sire': {
                columnConfig: {
                    editable: false
                },
                formEditorConfig: {
                    readOnly: true
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
            type: {
                columnConfig: {
                    width: 200
                },
            },
            // an animal joins the colony already assigned to a project, a protocol and a group; the trigger script
            // opens the matching assignment record for each one
            project: {
                xtype: 'combo',
                allowBlank: false,
                nullable: false,
                columnConfig: {
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
            birthProtocol: {
                xtype: 'combo',
                allowBlank: false,
                nullable: false,
                columnConfig: {
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
            'Id/demographics/birth': {
                allowBlank: false
            },
            'Id/demographics/gender': {
                allowBlank: false,
                nullable: false
            },
            // the conception is picked rather than typed: clicking the field, in the grid or in the row editor, opens
            // the same window the Start with Conception button uses and copies that conception onto the row
            conceptId: {
                xtype: 'nbri_ehr-conceptionField',
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 150
                }
            },
            breedingType: {
                columnConfig: {
                    width: 200
                }
            },
            // the social code is recorded once per animal, at birth or arrival, and lives on demographics
            'Id/demographics/socialCode': {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 200
                }
            }
        }
    }
});
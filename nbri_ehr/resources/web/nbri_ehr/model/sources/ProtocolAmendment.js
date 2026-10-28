/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('ProtocolAmendment', {
    allQueries: {

    },
    byQuery: {
        'nbri_ehr.ProtocolAmendment': {
            rowid: {
                allowBlank: true,
                nullable: true,
                hidden: true
            },
            protocol: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 220
                },
            },
            // assigned by the server in sequence
            amendmentNumber: {
                allowBlank: true,
                nullable: true,
                editable: false,
                columnConfig: {
                    width: 110,
                    editable: false
                },
            },
            date: {
                xtype: 'datefield',
                extFormat: LABKEY.extDefaultDateFormat,
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 160
                },
            },
            // written by the server: the protocol's end date, then the day before the next amendment takes effect
            enddate: {
                allowBlank: true,
                nullable: true,
                hidden: true,
                editable: false
            }
        },

        'ehr.protocol_counts': {
            rowid: {
                allowBlank: true,
                nullable: true,
                hidden: true
            },
            // ParentClientStore.getParentFieldName looks for this flag; without it the parent id would go to 'parentid'
            amendmentId: {
                isParentField: true,
                allowBlank: true,
                nullable: true,
                hidden: true,
                editable: false
            },
            // the amendment states the protocol once; each count row takes it from there
            protocol: {
                inheritFromParent: true,
                allowBlank: true,
                nullable: true,
                hidden: true,
                editable: false,
                columnConfig: {
                    editable: false
                }
            },
            taskid: {
                allowBlank: true,
                nullable: true,
                hidden: true
            },
            species: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 200
                },
            },
            allowed: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 140
                },
            },
            // unused: a count's window is its amendment's, and a second one here could contradict it
            start: {
                allowBlank: true,
                nullable: true,
                hidden: true,
                editable: false
            },
            enddate: {
                allowBlank: true,
                nullable: true,
                hidden: true,
                editable: false
            },
            gender: {
                hidden: true
            },
            project: {
                hidden: true
            },
            description: {
                height: 75,
                editorConfig: {
                    resizeDirections: 's'
                },
                columnConfig: {
                    width: 300
                },
            }
        }

    }
});

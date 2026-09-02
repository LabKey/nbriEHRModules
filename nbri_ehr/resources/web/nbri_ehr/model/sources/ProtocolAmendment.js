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
            amendmentType: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 160
                },
            },
            status: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 140
                },
            },
            cycle: {
                columnConfig: {
                    width: 110
                },
            },
            revision: {
                columnConfig: {
                    width: 110
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
            submittedDate: {
                xtype: 'datefield',
                extFormat: LABKEY.extDefaultDateFormat,
                columnConfig: {
                    width: 160
                },
            },
            approvedDate: {
                xtype: 'datefield',
                extFormat: LABKEY.extDefaultDateFormat,
                columnConfig: {
                    width: 160
                },
            },
            effectiveDate: {
                xtype: 'datefield',
                extFormat: LABKEY.extDefaultDateFormat,
                columnConfig: {
                    width: 160
                },
            },
            // written by the approval of the next amendment on the protocol
            enddate: {
                allowBlank: true,
                nullable: true,
                hidden: true,
                editable: false
            },
            newExpirationDate: {
                xtype: 'datefield',
                extFormat: LABKEY.extDefaultDateFormat,
                columnConfig: {
                    width: 180
                },
            },
            amendmentReason: {
                height: 75,
                editorConfig: {
                    resizeDirections: 's'
                },
                columnConfig: {
                    width: 300
                },
            },
            remark: {
                height: 75,
                editorConfig: {
                    resizeDirections: 's'
                },
                columnConfig: {
                    width: 300
                },
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
            // derived from the amendment: start is its effective date, enddate is written when a later amendment
            // supersedes this row. Never entered, or the count's window could contradict its own amendment.
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

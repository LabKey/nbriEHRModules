/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('Default', {
    allQueries: {
        Id: {
            xtype: 'ehr-animalIdUpperField',
        },
        'Id/demographics/dam': {
            xtype: 'ehr-animalIdUpperField',
        },
        'Id/demographics/sire': {
            xtype: 'ehr-animalIdUpperField',
        },
        performedby: {
            hidden: false,
            allowBlank: true,
            defaultValue: LABKEY.Security.currentUser.id,
            getInitialValue: function (v, rec) {
                if (Number.isInteger(Number(v))){
                    return v;
                }

                return LABKEY.Security.currentUser.id;
            },
            editorConfig: {
                store: {
                    type: 'labkey-store',
                    schemaName: 'core',

                    // 'performedby' is a text field in the dataset and its lookup to the userid is an int field - this mismatch causes it to disappear
                    // from the display when a value is selected from the dropdown even though the 'userid' value gets saved as a text (this behavior was only seen
                    // in the form panel but not in the grid panel).
                    // casting it as a varchar when loading the store fixes this issue.
                    sql: "SELECT CAST (UserId AS VARCHAR) AS UserId,DisplayName,FirstName,LastName FROM core.PrincipalsWithoutAdmin WHERE active = TRUE AND Type = 'u'",
                    autoLoad: true
                }
            }
        },
        orderedby: {
            hidden: false,
            allowBlank: false,
            defaultValue: null,
            columnConfig: {
                width: 160
            },
            editorConfig: {
                store: {
                    type: 'labkey-store',
                    schemaName: 'ehr_lookups',

                    // 'orderedby' is a text field in the dataset and its lookup to the userid is an int field - this mismatch causes it to disappear
                    // from the display when a value is selected from the dropdown even though the 'userid' value gets saved as a text.
                    // Casting it as a varchar when loading the store fixes this issue.
                    sql: 'SELECT CAST (UserId AS VARCHAR) AS UserId,DisplayName FROM ehr_lookups.veterinarians',
                    autoLoad: true
                }
            }
        },
        scheduleddate: {
            header: 'Scheduled Date/Time',
            label: 'Scheduled Date/Time',
            hidden: false,
            userEditable: false,
            nullable: true,
            columnConfig: {
                width: 180
            }
        },
        date: {
            columnConfig: {
                fixed: true,
                width: 150
            }
        },
        enddate: {
            columnConfig: {
                fixed: true,
                width: 150
            }
        },
        'cage': {
            columnConfig: {
                fixed: true,
                width: 150
            },
        },
        QCState: {
            hidden: true,
            editable: false,
            editorConfig: {
                hidden: true,
                editable: false
            },
            getInitialValue: function (v) {
                var qc;
                if (!v && EHR.Security.getQCStateByLabel('In Progress'))
                    qc = EHR.Security.getQCStateByLabel('In Progress').RowId;
                return v || qc || 'In Progress';
            }
        },
        'qcstate/label': {
            hidden: true,
            editable: false,
            editorConfig: {
                hidden: true,
                editable: false
            }
        }
    },
    byQuery: {
        'study.housing': {
            room: {
                allowBlank: true,
                hidden: true
            },
            'cage': {
                allowBlank: false
            },
            'enddate': {
                hidden: true
            },
            reason: {
                defaultValue: null,
                allowBlank: false,
                columnConfig: {
                    width: 180
                },
                lookup: {
                    filterArray: [LABKEY.Filter.create('date_disabled', null, LABKEY.Filter.Types.ISBLANK)]
                }
            },
            performedby: {
                hidden: false,
                allowBlank: true,
                defaultValue: LABKEY.Security.currentUser.id,
                lookup: {
                    schemaName: 'core',
                    queryName: 'users',
                    keyColumn: 'UserId',
                    displayColumn: 'DisplayName',
                    columns: 'UserId,DisplayName,FirstName,LastName',
                    sort: 'Type,DisplayName'
                },
                getInitialValue: function (v, rec) {
                    if (Number.isInteger(Number(v))){
                        return v;
                    }

                    return LABKEY.Security.currentUser.id;
                },
                editorConfig: {
                    store: {
                        type: 'labkey-store',
                        schemaName: 'core',

                        // 'performedby' is a text field in the dataset and its lookup to the userid is an int field - this mismatch causes it to disappear
                        // from the display when a value is selected from the dropdown even though the 'userid' value gets saved as a text (this behavior was only seen
                        // in the form panel but not in the grid panel).
                        // casting it as a varchar when loading the store fixes this issue.
                        sql: "SELECT CAST (UserId AS VARCHAR) AS UserId,DisplayName,FirstName,LastName FROM core.PrincipalsWithoutAdmin WHERE active = TRUE AND Type = 'u'",
                        autoLoad: true
                    }
                }
            },
        },
        'study.arrival': {
            initialRoom: {
                allowBlank: true,
                hidden: true
            }
        },
        'study.birth': {
            room: {
                allowBlank: true,
                hidden: true
            }
        },
        'study.exemptions': {
            category: {
                columnConfig: {
                    width: 300
                }
            }
        },
        'study.notes': {
            remark: {
                hidden: false,
                columnConfig: {
                    width: 400
                }
            }
        },
        'study.treatment_order': {
            endTreatmentOrderedBy: {
                columnConfig: {
                    width: 200
                },
                editorConfig: {
                    store: {
                        type: 'labkey-store',
                        schemaName: 'ehr_lookups',

                        // see 'orderedby' comment above.
                        sql: 'SELECT CAST (UserId AS VARCHAR) AS UserId,DisplayName FROM ehr_lookups.veterinarians',
                        autoLoad: true
                    }
                }
            },
            frequency: {
                columnConfig: {
                    width: 180
                },
                nullable: false,
                allowBlank: true,
                lookup: {
                    filterArray: [LABKEY.Filter.create('active', true, LABKEY.Filter.Types.EQUAL)]
                },
            }
        },
        'study.drug': {
            treatmentid: {
                hidden: true,
                nullable: true
            },
            performedby: {
                allowBlank: true,
            }
        },
        'study.observation_order': {
            category: {
                allowBlank: false,
                editorConfig: {
                    plugins: [Ext4.create('LDK.plugin.UserEditableCombo', {
                        allowChooseOther: false
                    })]
                },
                lookup: {
                    columns: 'value,description'
                },
                columnConfig: {
                    width: 200
                }
            },
            area: {
                defaultValue: 'N/A',
                columnConfig: {
                    width: 200
                }
            },
            observation: {
                columnConfig: {
                    width: 200
                }
            },
            remark: {
                columnConfig: {
                    width: 300
                }
            },
            frequency: {
                columnConfig: {
                    width: 180
                },
                nullable: false,
                allowBlank: true,
                lookup: {
                    filterArray: [LABKEY.Filter.create('active', true, LABKEY.Filter.Types.EQUAL)]
                },
            }
        },
        'study.pairings': {
            date: {
                header: 'Start Date',
                getInitialValue: function(v, rec){
                    if (v)
                        return v;

                    return new Date();
                }
            },
            type: {
                hidden: true
            },
            pairid: {
                hidden: false,
                nullable: false,
                columnConfig: {
                    width: 150
                },
            },
            formationType: {
                columnConfig: {
                    width: 150
                }
            },
            reason: {
                columnConfig: {
                    width: 150
                },
            },
            goal: {
                columnConfig: {
                    width: 150
                },
            },
            endState: {
                columnConfig: {
                    width: 150
                },
            },
            remark: {
                title: 'Remark on Formation',
                columnConfig: {
                    width: 200
                }
            },
            separationRemark: {
                columnConfig: {
                    width: 200
                }
            }
        },
        'study.flags': {
            flag: {
                allowBlank: false,
                lookup: {
                    columns: 'objectid,value,category,code',
                    sort: 'category,code,value',
                    filterArray: [LABKEY.Filter.create('datedisabled', null, LABKEY.Filter.Types.ISBLANK)]
                },
                columnConfig: {
                    width: 300
                },
                editorConfig: {
                    caseSensitive: false,
                    anyMatch: true,
                    plugins: [Ext4.create('LDK.plugin.UserEditableCombo', {
                        allowChooseOther: false
                    })],
                    listConfig: {
                        innerTpl: '{[(values.category ? ("<b>" + LABKEY.Utils.encodeHtml(values.category) + ":</b> ") : "") + LABKEY.Utils.encodeHtml(values.value)]}',
                        getInnerTpl: function () {
                            return this.innerTpl;
                        }
                    }
                }
            }
        }
    }
});
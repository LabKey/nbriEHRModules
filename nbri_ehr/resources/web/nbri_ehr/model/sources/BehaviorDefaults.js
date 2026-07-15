/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('BehaviorDefaults', {
    byQuery: {
        'study.clinremarks': {
            hx: {
                formEditorConfig: {
                    xtype: 'ehr-hxtextarea'
                },
            },
            p: {
                hidden: true,
            },
            s: {
                hidden: true,
            },
            o: {
                hidden: true,
            },
            a: {
                hidden: true,
            },
            remark: {
                label: 'Remark',
                height: 120
            },
            vetreview: {
                hidden: true,
            },
            category: {
                getInitialValue: function (v, rec) {
                    return 'Behavior'
                },
                editable: false,
                hidden: true,
                columnConfig: {
                    editable: false
                }
            },
            enddate: {
                hidden: true
            },
            dateFinalized: {
                hidden: true
            },
            qcstate: {
                hidden: true
            },
            performedby: {
                hidden: false,
                defaultValue: LABKEY.Security.currentUser.id.toString(),
                editorConfig: {
                    store: {
                        type: 'labkey-store',
                        schemaName: 'core',
                        sql: "SELECT CAST (UserId AS VARCHAR) AS UserId,DisplayName,FirstName,LastName FROM core.PrincipalsWithoutAdmin WHERE active = TRUE AND Type = 'u'",
                        autoLoad: true
                    }
                }
            },
        },
        'study.clinical_observations': {
            type: {
                hidden: true,
                defaultValue: 'Behavior'
            },
            category: {
                lookup: {
                    filterArray: [
                        LABKEY.Filter.create('category', 'Behavior')
                    ]
                }
            }
        },
        'study.observation_order': {
            type: {
                hidden: true,
                defaultValue: 'Behavior'
            },
            category: {
                lookup: {
                    filterArray: [
                        LABKEY.Filter.create('category', 'Behavior')
                    ]
                }
            }
        },
        'study.drug': {
            category: {
                defaultValue: 'Behavior',
                hidden: true
            }
        },
        'study.treatment_order': {
            category: {
                defaultValue: 'Behavior',
                hidden: true
            },
            performedby: {
                hidden: true
            }
        },
    }
});
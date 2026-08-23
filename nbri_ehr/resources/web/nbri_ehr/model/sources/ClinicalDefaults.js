/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('ClinicalDefaults', {
    byQuery: {
        'study.treatment_order': {
            category: {
                defaultValue: 'Clinical',
                hidden: true
            },
            performedby: {
                hidden: true
            },
            enddate: {
                allowBlank: true,
            }
        },
        'study.prc_order': {
            category: {
                defaultValue: 'Clinical',
                hidden: true
            },
            performedby: {
                hidden: true
            },
            windowStart: {
                columnConfig: {
                    fixed: true,
                    width: 150
                },
                getInitialValue: function(v, rec){
                    if (v)
                        return v;

                    return new Date();
                }
            },
            windowEnd: {
                columnConfig: {
                    fixed: true,
                    width: 150
                },
                getInitialValue: function(v, rec){
                    if (v)
                        return v;

                    let ret = Ext4.Date.clearTime(new Date());
                    ret = Ext4.Date.add(ret, Ext4.Date.DAY, 1);
                    ret.setHours(8);
                    return ret;
                }
            },
            procedure: {
                lookup: {
                    filterArray: [LABKEY.Filter.create('active', true, LABKEY.Filter.Types.EQUAL)]
                },
                columnConfig: {
                    width: 250
                }
            }
        },
        'study.drug': {
            category: {
                defaultValue: 'Clinical',
                hidden: true
            },
            date: {
                header: 'Date/Time',
            }
        },
        'study.prc': {
            category: {
                defaultValue: 'Clinical',
                hidden: true
            },
            // procedure data is not categorized, so not using procedure_category based selection
            procedure: {
                lookup: {
                    filterArray: [LABKEY.Filter.create('active', true, LABKEY.Filter.Types.EQUAL)]
                },
                columnConfig: {
                    width: 250
                }
            },
            orderid: {
                hidden: true
            }
        },
        'study.clinremarks': {
            category: {
                defaultValue: 'Clinical',
                hidden: true,
                allowBlank: false
            }
        },
        'study.blood': {
            reason: {
                defaultValue: 'Clinical',
                hidden: true
            },
            category: {
                defaultValue: 'Clinical',
                hidden: true,
                allowBlank: false
            },
            units: {
                hidden: true
            }
        },
        'study.cases': {
            openRemark: {
                height: 120
            },
            plan: {
                height: 120
            },
            closeRemark: {
                height: 120
            },
            QCState: {
                hidden: true
            },
            attachmentFile: {
                hidden: true
            },
            category: {
                hidden: true
            },
            caseCategory: {
                hidden: true
            },
            formCasesLink: {
                xtype: 'nbri_ehr-editCases'
            }
        },
        'study.clinremarks': {
            hx: {
                formEditorConfig: {
                    xtype: 'ehr-hxtextarea'
                },
            },
            p: {
                height: 120
            },
            remark: {
                label: 'Remark',
                height: 120
            },
            vetreview: {
                height: 120
            },
            category: {
                getInitialValue: function (v, rec) {
                    return 'Clinical'
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
            QCState: {
                hidden: true
            },
        },

        'study.clinical_observations': {
            type: {
                hidden: true,
                defaultValue: 'Clinical'
            },
            category: {
                lookup: {
                    columns: 'value,description',
                    filterArray: [
                        LABKEY.Filter.create('category', null, LABKEY.Filter.Types.ISBLANK)
                    ],
                }
            },
        },
        'study.vitals': {
            category: {
                defaultValue: 'Clinical',
                hidden: true
            },
            units: {
                hidden: true
            }
        },
        'study.observation_order': {
            type: {
                hidden: true,
                defaultValue: 'Clinical'
            },
            category: {
                lookup: {
                    columns: 'value,description',
                    filterArray: [
                        LABKEY.Filter.create('category', null, LABKEY.Filter.Types.ISBLANK)
                    ],
                }
            }
        }
    }
});
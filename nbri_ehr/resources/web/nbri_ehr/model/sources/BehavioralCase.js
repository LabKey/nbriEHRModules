/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

EHR.model.DataModelManager.registerMetadata('BehavioralCase', {
    allQueries: {
        Id: {
            inheritFromParent: true,
            editable: false,
            hidden: true,
            columnConfig: {
                editable: false
            }
        },
        date: {
            editable: true,
            hidden: false,
            columnConfig: {
                editable: true
            }
        },
        caseid: {
            inheritFromParent: true,
            editable: false,
            hidden: true,
            columnConfig: {
                editable: false
            }
        }
    },
    byQuery: {
        'study.cases': {
            Id: {
                xtype: 'nbri_ehr-animalIdCases',
                inheritFromParent: false,
                editable: true,
                hidden: false,
                columnConfig: {
                    editable: true
                }
            },
            date: {
                formEditorConfig: {
                    id: 'openDate',
                }
            },
            enddate: {
                formEditorConfig: {
                    id: 'closeDate'
                }
            },
            category: {
                getInitialValue: function (v, rec){
                    return 'Behavior'
                },
                editable: false,
                hidden: true,
                columnConfig: {
                    editable: false
                }
            },
            problemCategory: {
                nullable: false,
                editorConfig: {
                    listeners: {
                        select: function (field, recs) {
                            if (!recs || recs.length !== 1)
                                return;

                            var record = EHR.DataEntryUtils.getBoundRecord(field);
                            if (record) {
                                var rec = recs[0];
                                var meta = record.store.model.prototype.fields.get('problemSubcategory');
                                var storeId = LABKEY.ext4.Util.getLookupStoreId(meta);
                                var store = Ext4.StoreMgr.get(storeId);
                                if (store) {
                                    store.filterArray = [LABKEY.Filter.create('category', rec.get('value'))];
                                    store.load();
                                }
                            }
                        }
                    }
                },
            },
            openRemark: {
                height: 120
            },
            plan: {
                height: 120
            },
            closeRemark: {
                height: 120
            },
            qcstate: {
                hidden: true
            },
            attachmentFile: {
                hidden: true
            },
            caseCategory: {
                hidden: true
            },
            formCasesLink: {
                xtype: 'nbri_ehr-editCases'
            }
        }
    }
});
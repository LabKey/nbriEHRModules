/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('DeathNecropsy', {
    allQueries: {
    },
    byQuery: {
        'study.deaths': {
            qcstate: {
                hidden: true
            },
            date: {
                xtype: 'xdatetime',
                editorConfig: {
                    dateFormat: 'Y-m-d',
                    timeFormat: 'H:i'
                },
            },
            deathWeight: {
                label: 'Weight (kg)'
            },
            type: {
                allowBlank: false,
                nullable: false
            }
        },
        'study.necropsy': {
            date: {
                label: 'Exam Date',
                xtype: 'xdatetime',
                editorConfig: {
                    dateFormat: 'Y-m-d',
                    timeFormat: 'H:i',
                    listeners: {
                        change: function (combo, rec) {
                            const panel = combo.up('ehr-dataentrypanel');
                            if (panel) {
                                const date = panel.down('datefield')?.value;
                                const store = panel.storeCollection.getClientStoreByName('grossPathology');
                                if (store && date) {
                                    store.data.each(function (dateRec) {
                                        dateRec.set('date', rec);
                                    }, this);
                                }

                                const store2 = panel.storeCollection.getClientStoreByName('tissueDisposition');
                                if (store2 && date) {
                                    store2.data.each(function (dateRec) {
                                        dateRec.set('date', rec);
                                    }, this);
                                }
                            }
                        }
                    }
                }
            },
            grossAbnormalities: {
                xtype: 'ehr-remarkfield',
                height: 100,
                editorConfig: {
                    resizeDirections: 's'
                },
                columnConfig: {
                    width: 200
                }
            },
            diagnosis: {
                xtype: 'ehr-remarkfield',
                label: 'Diagnosis',
                resizeDirections: 's',
                height: 100,
                columnConfig: {
                    width: 200
                },
            },
            relevantHistory: {
                xtype: 'ehr-remarkfield',
                label: 'Relevant History',
                resizeDirections: 's',
                height: 100,
                columnConfig: {
                    width: 200,
                }
            },
            attachmentFile: {
                hidden: true
            },
            category: {
                hidden: true
            },
            qcstate: {
                hidden: true
            }
        },
        'study.grossPathology': {
            date: {
                hidden: true,
                getInitialValue: function(v, rec){
                    const necropsyStore = rec.storeCollection.getClientStoreByName('necropsy');
                    if (necropsyStore?.data?.items.length > 0) {
                        return necropsyStore.data.items[0].data.date;
                    }
                }
            },
            remark: {
                hidden: true,
                showInGrid: false
            },
            project: {
                hidden: true,
                showInGrid: false
            },
            systemExamined: {
                columnConfig: {
                    width: 200
                }
            },
            performedby: {
                hidden: true,
                showInGrid: false
            },
            appearance: {
                defaultValue: 'Normal',
                lookup: {
                    schemaName: 'ehr_lookups',
                    queryName: 'necropsy_organ_appearance',
                    keyColumn: 'title',
                    displayColumn: 'title',
                    defaultValue: 'Normal'
                }
            }
        },
        'study.tissueDisposition': {
            date: {
                hidden: true,
                getInitialValue: function(v, rec){
                    const necropsyStore = rec.storeCollection.getClientStoreByName('necropsy');
                    if (necropsyStore) {
                        return necropsyStore.data.items[0].data.date;
                    }
                }
            },
            project: {
                hidden: true,
                showInGrid: false
            },
            performedby: {
                hidden: true,
                showInGrid: false
            },
            necropsyDispositionCode: {
                columnConfig: {
                    fixed: true,
                    width: 200
                },
            }
        }
    }
});
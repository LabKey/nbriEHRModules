/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Metadata for the Necropsy Request, Necropsy and Necropsy Final forms. 'NecropsyRequest' is the request stage;
 * 'NecropsyRequestReadOnly' is used by the later stages, where the request is locked and the findings are editable.
 */
(function(){

    // child rows record the necropsy date rather than their own
    var NECROPSY_DATED_SECTIONS = ['procedureStaff', 'grossPathology', 'tissueMeasurements', 'tissueDisposition', 'preliminaryDiagnoses'];

    function getNecropsyDate(rec){
        var store = rec.storeCollection ? rec.storeCollection.getClientStoreByName('necropsy') : null;
        return store && store.getCount() > 0 ? store.getAt(0).get('date') : null;
    }

    var necropsyDatedField = {
        hidden: true,
        getInitialValue: function(v, rec){
            return v || getNecropsyDate(rec) || new Date();
        }
    };

    var ownDateField = {
        hidden: true,
        getInitialValue: function(v){
            return v || new Date();
        }
    };

    function remarkField(label, height){
        return {
            label: label,
            formEditorConfig: {
                xtype: 'textarea',
                height: height || 200,
                width: 1200
            },
            columnConfig: {
                width: 300
            }
        };
    }

    var wideTextField = {
        width: 400,
        height: 22,
        formEditorConfig: {
            xtype: 'textfield'
        }
    };

    function diagnosesFields(dateField){
        return {
            date: dateField,
            tissueSample: {
                nullable: false,
                columnConfig: {
                    width: 250
                }
            },
            diagnosis: {
                nullable: false,
                columnConfig: {
                    width: 250
                }
            },
            severity: {
                lookup: {
                    sort: 'sort_order'
                },
                columnConfig: {
                    width: 150
                }
            },
            distribution: {
                lookup: {
                    sort: 'sort_order'
                },
                columnConfig: {
                    width: 150
                }
            },
            remark: {
                columnConfig: {
                    width: 400
                }
            },
            performedby: {
                hidden: true
            }
        };
    }

    var requestFields = {
        Id: {},
        'Id/Demographics/death': {
            label: 'Death Date/Time',
            editable: false,
            hidden: false,
            columnConfig: {
                editable: false
            },
            formEditorConfig: {
                xtype: 'datefield',
                readOnly: true,
                format: 'Y-m-d H:i'
            }
        },
        date: {
            hidden: true
        },
        breed: {
            label: 'Breed/Strain'
        },
        wastingDisease: {},
        geneticModified: {},
        geneticModifiedBackground: {
            label: 'Genetic Background'
        },
        geneticModifiedGenotype: {
            label: 'Genotype'
        },
        clinicalDiagnosis: remarkField('Clinical Differential Diagnoses'),
        testsRequested: remarkField('Requested Samples/Tests'),
        history: remarkField('History'),
        performedby: {
            hidden: true
        },
        remark: {
            hidden: true
        }
    };

    var taskFields = {
        objectid: {
            hidden: true
        },
        Task: Ext4.apply({
            label: 'Title',
            nullable: false,
            getInitialValue: function(v, rec){
                return v || rec.dataEntryPanel.formConfig.name;
            }
        }, wideTextField),
        Date: {
            label: 'Date of Request',
            nullable: false,
            getInitialValue: function(v){
                return v || new Date();
            },
            editorConfig: {
                listeners: {
                    change: function(field, date){
                        var panel = field.up('ehr-dataentrypanel');
                        var store = panel ? panel.storeCollection.getClientStoreByName('necropsyRequest') : null;
                        if (store && Ext4.isDate(date)) {
                            store.each(function(rec){
                                rec.set('date', date);
                            });
                        }
                    }
                }
            }
        },
        SubmittingPI: wideTextField,
        SubmittingVet: {
            nullable: false
        },
        SubmittingVetCell: {},
        PriorityCode: {
            lookup: {
                sort: 'sort_order'
            },
            getInitialValue: function(v){
                return v || 'Normal';
            }
        },
        Sensitive: {
            editorConfig: {
                listeners: {
                    change: function(field, val){
                        var reason = field.up('panel').down('field[name=SensitiveReason]');
                        if (reason)
                            reason.setDisabled(val !== true);
                    }
                }
            }
        },
        SensitiveReason: wideTextField,
        AssignedTo: {
            hidden: true
        },
        status: {
            hidden: true
        }
    };

    function readOnly(fields){
        var ret = {};
        Ext4.Object.each(fields, function(name, cfg){
            ret[name] = Ext4.apply({}, cfg);
            if (!cfg.hidden)
                ret[name].formEditorConfig = Ext4.apply({}, {readOnly: true}, cfg.formEditorConfig);
        });
        return ret;
    }

    var findingsQueries = {
        'study.necropsy': {
            date: {
                label: 'Necropsy Date/Time',
                xtype: 'xdatetime',
                editorConfig: {
                    dateFormat: 'Y-m-d',
                    timeFormat: 'H:i',
                    listeners: {
                        change: function(field, val){
                            if (!Ext4.isDate(val))
                                return;

                            var panel = field.up('ehr-dataentrypanel');
                            if (!panel)
                                return;

                            Ext4.Array.forEach(NECROPSY_DATED_SECTIONS, function(name){
                                var store = panel.storeCollection.getClientStoreByName(name);
                                if (store) {
                                    store.each(function(rec){
                                        rec.set('date', val);
                                    });
                                }
                            });
                        }
                    }
                }
            },
            accessionNo: {
                label: 'Accession Number',
                nullable: false
            },
            weight: {
                label: 'Body Weight (kg)'
            },
            preliminaryCauseOfDeath: remarkField('Preliminary Cause of Death', 35),
            preliminaryComments: remarkField('Preliminary Comments'),
            remark: {
                hidden: true
            }
        },
        'study.procedureStaff': {
            date: necropsyDatedField,
            category: {
                hidden: true,
                getInitialValue: function(v){
                    return v || 'Necropsy';
                }
            },
            staff: {
                nullable: false,
                columnConfig: {
                    width: 200
                }
            },
            role: {
                nullable: false,
                columnConfig: {
                    width: 200
                }
            },
            performedby: {
                hidden: true
            }
        },
        'study.grossPathology': {
            date: necropsyDatedField,
            remark: remarkField('Gross Description'),
            performedby: {
                hidden: true
            }
        },
        'study.tissueMeasurements': {
            date: necropsyDatedField,
            tissueSample: {
                nullable: false,
                columnConfig: {
                    width: 200
                }
            },
            qualifier: {
                columnConfig: {
                    width: 200
                }
            },
            preparation: {
                columnConfig: {
                    width: 250
                }
            },
            measurement1: {
                columnConfig: {
                    width: 150
                }
            },
            measurement2: {
                columnConfig: {
                    width: 150
                }
            },
            measurement3: {
                columnConfig: {
                    width: 150
                }
            },
            performedby: {
                hidden: true
            }
        },
        'study.tissueDisposition': {
            date: necropsyDatedField,
            project: {
                hidden: true
            },
            performedby: {
                hidden: true
            },
            necropsyDispositionCode: {
                columnConfig: {
                    fixed: true,
                    width: 200
                }
            }
        },
        'study.histology': {
            date: ownDateField,
            tissueSample: {
                columnConfig: {
                    width: 300
                }
            },
            // the shared EHR defaults hide these and default the stain to H&E
            qualifier: {
                hidden: false,
                columnConfig: {
                    width: 200
                }
            },
            stain: {
                hidden: false,
                defaultValue: null,
                columnConfig: {
                    width: 200
                }
            },
            remark: {
                header: 'Histology Description',
                columnConfig: {
                    width: 500
                }
            }
        },
        'study.preliminaryDiagnoses': diagnosesFields(necropsyDatedField),
        'study.finalDiagnoses': diagnosesFields(ownDateField),
        'study.necropsyFinal': {
            date: ownDateField,
            causeCategory: {
                lookup: {
                    sort: 'sort_order'
                }
            },
            causeOfDeath: remarkField('Cause of Death', 35),
            comments: remarkField('Comments'),
            performedby: {
                hidden: true
            },
            remark: {
                hidden: true
            }
        }
    };

    EHR.model.DataModelManager.registerMetadata('NecropsyRequest', {
        allQueries: {
            QCState: {
                hidden: true
            },
            necropsyStatus: {
                hidden: true
            }
        },
        byQuery: {
            'ehr.tasks': {
                title: {
                    getInitialValue: function(v){
                        return v || 'Necropsy';
                    }
                }
            },
            'nbri_ehr.necropsyTask': taskFields,
            'study.necropsyRequest': requestFields
        }
    });

    EHR.model.DataModelManager.registerMetadata('NecropsyRequestReadOnly', {
        allQueries: {
            QCState: {
                hidden: true
            },
            necropsyStatus: {
                hidden: true
            }
        },
        byQuery: Ext4.apply({
            'nbri_ehr.necropsyTask': readOnly(taskFields),
            'study.necropsyRequest': readOnly(requestFields)
        }, findingsQueries)
    });
})();

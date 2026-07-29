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
    allQueries: {
        'endDate': {
            hidden: true
        }
    },
    byQuery: {
        'study.birth': {
            'Id/demographics/species': {
                allowBlank: false,
                columnConfig: {
                    fixed: true,
                    width: 250
                }
            },
            'cage': {
                // allowBlank: false,
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
            cond: {
                columnConfig: {
                    width: 200
                },
            },
            // project and protocol are entered through the Project Assignment and Protocol Assignment sections
            project: {
                allowBlank: true,
                hidden: true,
                showInGrid: false
            },
            birthProtocol: {
                allowBlank: true,
                hidden: true,
                showInGrid: false
            },
            'Id/demographics/birth': {
                allowBlank: false
            },
            'Id/demographics/gender': {
                allowBlank: false
            },
            conceptId: {
                columnConfig: {
                    width: 150
                }
            },
            breedingType: {
                columnConfig: {
                    width: 200
                }
            }
        }
    }
});
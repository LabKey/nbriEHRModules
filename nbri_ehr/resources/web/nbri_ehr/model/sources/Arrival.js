/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.onReady(function() {
    // this is to skip Id not found warning during weights entry in Arrival data entry form
    if (EHR.data.DataEntryClientStore) {
        Ext4.override(EHR.data.DataEntryClientStore, {
            getExtraContext: function(){
                return {
                    skipIdNotFoundError: {'form': 'arrival'}
                }
            }
        });
    }
});

EHR.model.DataModelManager.registerMetadata('Arrival', {
    allQueries: {
        'endDate': {
            hidden: true
        }
    },
    byQuery: {
        'study.arrival': {
            'cage': {
                // allowBlank: false,
                columnConfig: {
                    fixed: true,
                    width: 200
                },
            },
            'Id/demographics/species': {
                columnConfig: {
                    fixed: true,
                    width: 200
                },
                allowBlank: false
            },
            'Id/demographics/birth': {
                // allowBlank: false
            },
            'Id/demographics/gender': {
                allowBlank: false
            },
            'Id/demographics/geographic_origin': {
                // allowBlank: false,
                columnConfig: {
                    fixed: true,
                    width: 200
                }
            },
            // project and protocol are entered through the Project Assignment and Protocol Assignment sections
            project: {
                allowBlank: true,
                hidden: true,
                showInGrid: false
            },
            arrivalProtocol: {
                allowBlank: true,
                hidden: true,
                showInGrid: false
            },
            performedby: {
                hidden: true,
                showInGrid: false
            },
            sourceFacility: {
                allowBlank: false,
                columnConfig: {
                    fixed: true,
                    width: 150
                },
            },
            acquisitionType: {
                // allowBlank: false,
                columnConfig: {
                    fixed: true,
                    width: 150
                },
            },
            arrivalType: {
                // allowBlank: false,
                columnConfig: {
                    width: 200
                }
            },
            rearrival: {
                allowBlank: true,
                hidden: true,
                showInGrid: false
            }
        }
    }
});
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
        // lowercase to match the key Default.js and Assignment.js use; a differently-cased key shadows theirs entirely
        // rather than merging with it
        'enddate': {
            hidden: true
        }
    },
    byQuery: {
        'study.birth': {
            Id: {
                allowBlank: false,
                nullable: false
            },
            date: {
                allowBlank: false,
                nullable: false
            },
            // conception Id, species, dam and sire all come from the conception picked in the Start with Conception
            // window, so they are shown but not entered by hand.  That window writes to the store directly, which is
            // unaffected by these read-only editor settings.
            'Id/demographics/species': {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    fixed: true,
                    width: 250,
                    editable: false
                },
                formEditorConfig: {
                    readOnly: true
                }
            },
            'Id/demographics/dam': {
                columnConfig: {
                    editable: false
                },
                formEditorConfig: {
                    readOnly: true
                }
            },
            'Id/demographics/sire': {
                columnConfig: {
                    editable: false
                },
                formEditorConfig: {
                    readOnly: true
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
                allowBlank: false,
                nullable: false
            },
            // see the note above on the fields the Start with Conception window populates
            conceptId: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 150,
                    editable: false
                },
                formEditorConfig: {
                    readOnly: true
                }
            },
            breedingType: {
                columnConfig: {
                    width: 200
                }
            },
            // the social code is recorded once per animal, at birth or arrival, and lives on demographics
            'Id/demographics/socialCode': {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 200
                }
            }
        }
    }
});
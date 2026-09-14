/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('Conception', {
    allQueries: {

    },
    byQuery: {
        'study.conception': {
            Id: {
                label: 'Dam',
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 200
                }
            },
            date: {
                label: 'Conception Date',
                xtype: 'datefield',
                extFormat: LABKEY.extDefaultDateFormat,
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 200
                }
            },
            conceptId: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 200
                }
            },
            estimated: {
                xtype: 'checkbox',
                defaultValue: false,
                columnConfig: {
                    width: 100
                }
            },
            sire: {
                xtype: 'ehr-animalfield',
                lookups: false,
                columnConfig: {
                    width: 200
                }
            },
            remark: {
                height: 75,
                editorConfig: {
                    resizeDirections: 's'
                },
                columnConfig: {
                    width: 300
                }
            }
        },

    }
});

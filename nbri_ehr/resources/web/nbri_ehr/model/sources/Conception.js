/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('Conception', {
    allQueries: {

    },
    byQuery: {
        'nbri_ehr.Conception': {
            RowId: {
                allowBlank: true,
                nullable: true,
                hidden: true
            },
            ConceptId: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 200
                },
            },
            ConceptDate: {
                xtype: 'datefield',
                extFormat: LABKEY.extDefaultDateFormat,
                columnConfig: {
                    width: 200
                },
            },
            ConceptTermDate: {
                xtype: 'datefield',
                extFormat: LABKEY.extDefaultDateFormat,
                columnConfig: {
                    width: 200
                },
            },
            Estimated: {
                xtype: 'checkbox',
                defaultValue: false,
                columnConfig: {
                    width: 100
                },
            },
            Dam: {
                xtype: 'ehr-animalfield',
                lookups: false,
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 200
                },
            },
            Sire: {
                xtype: 'ehr-animalfield',
                lookups: false,
                columnConfig: {
                    width: 200
                },
            },
            Remark: {
                height: 75,
                editorConfig: {
                    resizeDirections: 's'
                },
                columnConfig: {
                    width: 300
                },
            }
        },

    }
});

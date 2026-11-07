/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('DailyHealth', {
    allQueries: {

    },
    byQuery: {
        'study.dailyHealthLog': {
            Id: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 150
                }
            },
            date: {
                label: 'Observation Date',
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 180
                }
            },
            sortOrder: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 90
                }
            },
            code: {
                xtype: 'combo',
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 150
                },
                lookup: {
                    schemaName: 'nbri_ehr',
                    queryName: 'dailyHealthCode',
                    keyColumn: 'code',
                    displayColumn: 'code',
                    columns: 'code,name',
                    filterArray: [
                        LABKEY.Filter.create('enabled', true, LABKEY.Filter.Types.EQUAL)
                    ]
                }
            },
            // the task id doubles as the log entry id, so every row of the form shares it
            logEntryId: {
                hidden: true,
                getInitialValue: function(v, rec){
                    return v || (rec.dataEntryPanel ? rec.dataEntryPanel.taskId : null);
                }
            },
            performedby: {
                label: 'Technician',
                allowBlank: false
            },
            remark: {
                columnConfig: {
                    width: 250
                }
            }
        }
    }
});

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('DailyHealth', {
    allQueries: {

    },
    byQuery: {
        // Id, date and performedby are entered once in the Log Entry section, which writes them into every row
        'study.dailyHealthLog': {
            Id: {
                allowBlank: false,
                nullable: false,
                shownInGrid: false
            },
            date: {
                label: 'Observation Date',
                allowBlank: false,
                nullable: false,
                shownInGrid: false
            },
            sortOrder: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 90
                }
            },
            code: {
                // the shared EHR customizer turns faceting off on every column named code, which leaves the lookup combo without a store
                facetingBehaviorType: 'AUTOMATIC',
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
                allowBlank: false,
                shownInGrid: false
            },
            remark: {
                columnConfig: {
                    width: 250
                }
            }
        }
    }
});

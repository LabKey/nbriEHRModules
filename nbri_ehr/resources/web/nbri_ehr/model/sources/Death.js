/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Metadata for the grid-based Deaths form. The columnConfig widths only take effect in the grid; they are ignored
 * when the same fields render in a form panel.
 */
EHR.model.DataModelManager.registerMetadata('Death', {
    allQueries: {
    },
    byQuery: {
        'study.deaths': {
            QCState: {
                hidden: true
            },
            date: {
                xtype: 'xdatetime',
                editorConfig: {
                    dateFormat: 'Y-m-d',
                    timeFormat: 'H:i'
                },
                columnConfig: {
                    width: 160
                }
            },
            deathWeight: {
                label: 'Weight (kg)',
                columnConfig: {
                    width: 150
                }
            },
            type: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 160
                }
            },
            reason: {
                columnConfig: {
                    width: 160
                }
            },
            remark: {
                xtype: 'ehr-remarkfield',
                columnConfig: {
                    width: 200
                }
            }
        }
    }
});

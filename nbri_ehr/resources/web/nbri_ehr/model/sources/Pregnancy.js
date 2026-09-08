/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('Pregnancy', {
    allQueries: {

    },
    byQuery: {
        'study.pregnancy': {
            project: {
                hidden: true,
            },
            diagnosis: {
                hidden: true,
            },
            attachmentFile: {
                hidden: true,
            },
            result: {
                allowBlank: false,
                nullable: false,
            },
            conceptId: {
                allowBlank: false,
                nullable: false,
                columnConfig: {
                    width: 150
                }
            },
            // shares the delivery_mode lookup with study.birth, but is optional here: an outcome can be recorded
            // before the delivery mode is known
            type: {
                columnConfig: {
                    width: 200
                }
            }
        },

    }
});
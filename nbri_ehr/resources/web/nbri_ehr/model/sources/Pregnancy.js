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
            type: {
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
                columnConfig: {
                    width: 150
                }
            }
        },

    }
});
/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('Alias', {
    allQueries: {

    },
    byQuery: {
        'study.alias': {
            project: {
                hidden: true,
            },
            alias: {
                allowBlank: false,
                nullable: false,
            },
            category: {
                allowBlank: false,
                nullable: false,
            }
        },

    }
});
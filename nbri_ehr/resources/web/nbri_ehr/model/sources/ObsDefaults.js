/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('ObsDefaults', {
    byQuery: {
        'study.clinical_observations': {
            category: {
                lookup: {
                    columns: 'value,description',
                    filterArray: []
                }
            },
        },
        'study.observation_order': {
            category: {
                lookup: {
                    columns: 'value,description',
                    filterArray: []
                }
            }
        }
    }
});
/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('ObsDefaults', {
    byQuery: {
        'study.clinical_observations': {
            // This form offers every observation type, so it can't know the observation's type up front.
            // Clearing the default inherited from ClinicalDefaults lets the trigger script derive it
            // from the selected type's category.
            type: {
                hidden: true,
                defaultValue: null
            },
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
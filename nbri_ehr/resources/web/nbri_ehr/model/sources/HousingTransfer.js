/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

EHR.model.DataModelManager.registerMetadata('HousingTransfer', {

    byQuery: {
        'study.housing': {
            // display-only; NBRI_EHR.data.HousingTransferClientStore fills it from the demographics cache
            'Id/curLocation/location': {
                header: 'Current Location',
                label: 'Current Location',
                editable: false,
                columnConfig: {
                    editable: false,
                    width: 200
                }
            },
            'cage': {
                header: 'New Location',
                label: 'New Location',
                columnConfig: {
                    width: 200
                }
            }
        }
    }
});

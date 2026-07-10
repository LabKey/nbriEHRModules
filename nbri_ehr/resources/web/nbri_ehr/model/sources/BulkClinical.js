/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('BulkClinical', {
    byQuery: {
        'study.clinremarks': {
            p: {
                columnConfig: {
                    width: 250
                }
            },
            vetreview: {
                columnConfig: {
                    width: 250
                }
            },
            vetreviewdate: {
                columnConfig: {
                    width: 200
                }
            },
            dateFinalized: {
                columnConfig: {
                    width: 200
                }
            },
            hx: {
                hidden: true
            },
            remark: {
                allowBlank: true
            },
            category: {
                getInitialValue: function (v, rec) {
                    return 'Clinical'
                },
                editable: false,
                hidden: true,
                columnConfig: {
                    editable: false
                }
            }
        }
    }
});
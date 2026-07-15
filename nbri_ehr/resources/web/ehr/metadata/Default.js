/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */


EHR.model.DataModelManager.registerMetadata('Default', {
    byQuery: {
        'study.MHC': {
            QCState: {
                getInitialValue: function(v){
                    var qc;
                    if (!v && EHR.Security.getQCStateByLabel('Completed'))
                        qc = EHR.Security.getQCStateByLabel('Completed').RowId;
                    return v || qc;
                },
            }
        }
    }
});
/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.DataEntryUtils.registerDataEntryFormButton('NBRISAVEDRAFTBUTTON', {
    text: 'Save Draft',
    name: 'nbriSaveDraft',
    errorThreshold: 'ERROR',
    successURL: LABKEY.ActionURL.buildURL('ehr', 'enterData.view'),
    itemId: 'nbriSaveDraftBtn',
    disabled: false,
    requiredQC: 'In Progress',
    targetQC: 'In Progress',
    handler: function(btn){
        var panel = btn.up('ehr-dataentrypanel');
        panel.onSubmit(btn, false, true);
    },
    disableOn: 'ERROR'
});
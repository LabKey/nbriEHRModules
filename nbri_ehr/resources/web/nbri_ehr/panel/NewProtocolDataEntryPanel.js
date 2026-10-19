/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.panel.NewProtocolDataEntryPanel', {
    extend: 'EHR.panel.TaskDataEntryPanel',

    applyConfigToServerStore: function(cfg){
        cfg = this.callParent(arguments);

        // ehr.protocol has no taskid, so the server drops the task filter and returns every protocol; this form only
        // creates, so load none
        if (LABKEY.Utils.caseInsensitiveEquals(cfg.schemaName, 'ehr') && LABKEY.Utils.caseInsensitiveEquals(cfg.queryName, 'protocol')){
            cfg.filterArray.push(LABKEY.Filter.create('objectid', null, LABKEY.Filter.Types.ISBLANK));
            // matches NewProtocolClientStore, so the client and server rows pair up on a key that doesn't change
            cfg.keyFieldName = 'objectid';
        }

        return cfg;
    }
});

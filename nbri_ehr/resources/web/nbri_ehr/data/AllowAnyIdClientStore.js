/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.data.AllowAnyIdClientStore', {
    extend: 'EHR.data.DataEntryClientStore',

    getExtraContext: function () {
        var ret = this.callParent(arguments) || {};

        // Tell the trigger scripts to allow any Id. Requires handling in trigger script to fully enable.
        ret['allowAnyId'] = true;
        return ret;
    }
});

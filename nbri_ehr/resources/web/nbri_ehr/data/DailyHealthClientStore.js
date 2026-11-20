/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// sortOrder is each row's position in the grid, so saved rows must load sorted by it or reopening reorders them.
Ext4.define('NBRI_EHR.data.DailyHealthClientStore', {
    extend: 'EHR.data.DataEntryClientStore',

    constructor: function(){
        this.callParent(arguments);
        this.on('datachanged', this.updateSortOrder, this);
    },

    updateSortOrder: function(){
        this.each(function(r, idx){
            if (Number(r.get('sortOrder')) !== idx + 1){
                r.beginEdit();
                r.set('sortOrder', idx + 1);
                r.endEdit(true);
            }
        }, this);
    }
});

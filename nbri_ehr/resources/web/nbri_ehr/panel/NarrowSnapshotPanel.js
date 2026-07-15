/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.define('NBRI_EHR.panel.NarrowSnapshotPanel', {
    extend: 'NBRI_EHR.panel.SnapshotPanel',
    alias: 'widget.nbri_ehr-narrowsnapshotpanel',

    showLocationDuration: false,
    showExtendedInformation: true,

    minWidth: 800,

    initComponent: function(){

        this.defaultLabelWidth = 120;
        this.callParent();
    },

    getItems: function() {
        var items = this.getBaseItems();

        //combine the first and second columns
        var secondCol = items[0].items[1].items[1];
        var extended = this.getExtendedItems();

        var index = items[0].items[1].items.indexOf(secondCol);
        if (index !== -1) {
            items[0].items[1].items = items[0].items[1].items.splice(index, 1);
        }

        items[0].items[1].items = items[0].items[1].items.concat(extended[0].items[1].items[0]);
        items[0].items[1].items[1].items = items[0].items[1].items[1].items.concat(extended[0].items[1].items[1].items[0]);

        items[0].items[1].items[0].columnWidth = 0.45;
        items[0].items[1].items[1].columnWidth = 0.55;

        return items;
    }
});
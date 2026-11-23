/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// Rows are dragged into order; NBRI_EHR.data.DailyHealthClientStore turns that order into sortOrder.
Ext4.define('NBRI_EHR.grid.DailyHealthGridPanel', {
    extend: 'EHR.grid.Panel',
    alias: 'widget.nbri_ehr-dailyhealthgridpanel',

    initComponent: function(){
        this.allowDragDropReorder = true;
        this.callParent();

        // EHR counts fields hidden from the grid when it picks the column Add starts editing, which lands past the last column
        var codeIdx = Ext4.Array.indexOf(Ext4.Array.pluck(this.headerCt.getVisibleGridColumns(), 'dataIndex'), 'code');
        if (codeIdx > -1)
            this.firstEditableColumn = codeIdx;
    }
});

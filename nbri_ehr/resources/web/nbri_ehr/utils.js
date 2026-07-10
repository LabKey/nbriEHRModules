/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
NBRI_EHR.Utils = new function () {
    return {

        showFlagPopup: function (id, el) {
            var ctx = EHR.Utils.getEHRContext() || {};
            Ext4.create('Ext.window.Window', {
                title: 'Flag Details: ' + id,
                width: 880,
                modal: true,
                bodyStyle: 'padding: 5px;',
                items: [{
                    xtype: 'grid',
                    cls: 'ldk-grid', //variable row height
                    border: false,
                    store: {
                        type: 'labkey-store',
                        containerPath: ctx['EHRStudyContainer'],
                        schemaName: 'study',
                        queryName: 'flags',
                        columns: 'Id,date,enddate,flag/category,flag/value,remark,performedby/displayName',
                        filterArray: [LABKEY.Filter.create('Id', id), LABKEY.Filter.create('isActive', true)],
                        autoLoad: true
                    },
                    viewConfig: {
                        //loadMask: !(Ext4.isIE && Ext4.ieVersion <= 8)
                    },
                    columns: [{
                        header: 'Category',
                        dataIndex: 'flag/category',
                        width: 160
                    }, {
                        header: 'Meaning',
                        dataIndex: 'flag/value',
                        tdCls: 'ldk-wrap-text',
                        width: 160
                    }, {
                        header: 'Date Added',
                        dataIndex: 'date',
                        xtype: 'datecolumn',
                        format: LABKEY.extDefaultDateFormat,
                        width: 110
                    }, {
                        header: 'Date Removed',
                        dataIndex: 'enddate',
                        xtype: 'datecolumn',
                        format: LABKEY.extDefaultDateFormat,
                        width: 110
                    }, {
                        header: 'Remark',
                        dataIndex: 'remark',
                        tdCls: 'ldk-wrap-text',
                        width: 210
                    }, {
                        header: 'Performed By',
                        dataIndex: 'performedby/displayName',
                        width: 110
                    }],
                    border: false
                }],
                buttons: [{
                    text: 'Close',
                    handler: function (btn) {
                        btn.up('window').close();
                    }
                }]
            }).show(el);
        },
    }
}
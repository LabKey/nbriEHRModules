/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.window.RecentRemarksWindow', {
    extend: 'Ext.window.Window',

    initComponent: function(){
        Ext4.apply(this, {
            modal: true,
            width: 1000,
            closeAction: 'destroy',
            minHeight: 400,
            bodyStyle: 'padding: 5px;',
            title: 'Recent Remarks',
            items: [{
                html: 'The grid below will show any vet and behavior remarks and observations entered in the previous 5 days for this animal',
                style: 'padding-bottom: 10px;',
                border: false
            }, this.getGridConfig()],
            buttons: [{
                text: 'Close',
                handler: function(btn){
                    btn.up('window').close();
                }
            }]
        });

        this.callParent(arguments);
    },

    getStore: function(){
        if (this.store)
            return this.store;

        this.store = Ext4.create('LABKEY.ext4.data.Store', {
            schemaName: 'study',
            queryName: 'clinremarks',
            columns: 'Id,date,category,s,o,a,p,remark,performedBy',
            sort: '-date',
            filterArray: [
                LABKEY.Filter.create('Id', this.animalId, LABKEY.Filter.Types.EQUAL),
                LABKEY.Filter.create('date', (Ext4.Date.add(new Date(), Ext4.Date.DAY, -5)), LABKEY.Filter.Types.DATE_GREATER_THAN_OR_EQUAL)
            ],
            autoLoad: true
        });

        return this.store;
    },

    getGridConfig: function(){
        return {
            xtype: 'grid',
            maxHeight: 400,
            border: true,
            store: this.getStore(),
            viewConfig: {
                loadMask: !(Ext4.isIE && Ext4.ieVersion <= 8)
            },
            columns: [{
                header: 'Date',
                xtype: 'datecolumn',
                width: 160,
                format: LABKEY.extDefaultDateTimeFormat,
                dataIndex: 'date'
            },{
                header: 'Subjective',
                width: 200,
                dataIndex: 's',
                tdCls: 'ldk-wrap-text',
                noWrap: false,
                renderer: function(v){
                    if (v){
                        return v.replace(/\n/g, '<br>');
                    }
                }
            },{
                header: 'Objective',
                width: 200,
                dataIndex: 'o',
                tdCls: 'ldk-wrap-text',
                noWrap: false,
                renderer: function(v){
                    if (v){
                        return v.replace(/\n/g, '<br>');
                    }
                }
            },{
                header: 'Assessment',
                width: 200,
                dataIndex: 'a',
                tdCls: 'ldk-wrap-text',
                noWrap: false,
                renderer: function(v){
                    if (v){
                        return v.replace(/\n/g, '<br>');
                    }
                }
            },{
                header: 'Plan',
                width: 200,
                dataIndex: 'p',
                tdCls: 'ldk-wrap-text',
                noWrap: false,
                renderer: function(v){
                    if (v){
                        return v.replace(/\n/g, '<br>');
                    }
                }
            }]
        }
    },

    statics: {
        showRecentRemarks: function(animalId){
            Ext4.create('NBRI_EHR.window.RecentRemarksWindow', {
                animalId: animalId
            }).show();
        }
    }
});
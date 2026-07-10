/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.window.RecentCasesWindow', {
    extend: 'Ext.window.Window',

    caseCount: 20,

    initComponent: function(){
        Ext4.apply(this, {
            modal: true,
            width: 1250,
            closeAction: 'destroy',
            minHeight: 400,
            bodyStyle: 'padding: 5px;',
            title: 'Recent Cases',
            items: [{
                html: 'The grid below will show the previous ' + this.caseCount + ' cases for this animal',
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
        const formType = LABKEY.ActionURL.getParameter('formType');
        const caseCategory = NBRI_EHR.panel.SelectCasePanel?.getCaseCategory()[formType] ?? 'Clinical';

        this.store = Ext4.create('LABKEY.ext4.data.Store', {
            schemaName: 'study',
            queryName: 'recentCases',
            columns: 'Id,date,reviewdate,enddate,problemCategory,problemSubcategory,remark,plan,caseHistory,category',
            sort: '-date',
            parameters: {'SubjectId': this.animalId, 'CaseCategory': caseCategory},
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
                loadMask: true
            },
            columns: [{
                header: 'OpenDate',
                xtype: 'datecolumn',
                width: 150,
                format: LABKEY.extDefaultDateTimeFormat,
                dataIndex: 'date'
            },{
                header: 'Close Date',
                xtype: 'datecolumn',
                width: 150,
                format: LABKEY.extDefaultDateTimeFormat,
                dataIndex: 'enddate'
            },{
                header: 'Category',
                width: 150,
                dataIndex: 'category'
            },{
                header: 'Problem Area',
                width: 150,
                dataIndex: 'problemCategory'
            },{
                header: 'Problem Subcategory',
                width: 150,
                dataIndex: 'problemSubcategory'
            },{
                header: 'Description/Notes',
                width: 500,
                dataIndex: 'remark',
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
        showRecentCases: function(animalId){
            Ext4.create('NBRI_EHR.window.RecentCasesWindow', {
                animalId: animalId
            }).show();
        }
    }
});
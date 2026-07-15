/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * @cfg {String} animalId
 * @cfg {Object} casepanel
 */
Ext4.define('NBRI_EHR.panel.SelectCasePanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.nbri_ehr-selectcasepanel',

    statics: {

        getButtonConfig: function(){
            return [{
                xtype: 'button',
                text: 'Use Selected Case',
                handler: function(btn){
                    var window = btn.up('window');
                    var grid = window.down('grid');
                    var rec = grid.getSelectionModel().getSelection()[0];
                    EHR.DemographicsCache.reportCaseSelected(rec);
                    window.close();
                }
            }, {
                xtype: 'button',
                text: 'Show Closed Cases',
                handler: function(btn){
                    var owner = btn.up('window');
                    if (owner)
                        owner = owner.down('panel');

                    var store = owner.getStore();
                    const formType = LABKEY.ActionURL.getParameter('formType');
                    const caseCategory = NBRI_EHR.panel.SelectCasePanel.getCaseCategory()[formType];

                    LDK.Assert.assertNotEmpty('Unable to find animalId in ManageCasesPanel', owner.animalId);
                    var filterArray = [
                        LABKEY.Filter.create('Id', owner.animalId, LABKEY.Filter.Types.EQUAL),
                        LABKEY.Filter.create('category', caseCategory, LABKEY.Filter.Types.EQUAL),
                        LABKEY.Filter.create('QCState/Label', "Completed", LABKEY.Filter.Types.EQUAL)
                    ];

                    if (btn.text == 'Show Closed Cases'){
                        btn.setText('Hide Closed Cases');
                    }
                    else {
                        btn.setText('Show Closed Cases');
                        filterArray.push(LABKEY.Filter.create('isOpen', true, LABKEY.Filter.Types.EQUAL));
                    }

                    store.filterArray = filterArray;
                    store.load();
                }
            }]
        },

        getCaseCategory: function(){
            return {
                'Clinical Cases': 'Clinical',
                'Clinical Rounds': 'Clinical',
                'Behavioral Cases': 'Behavior',
                'Behavioral Rounds': 'Behavior',
            };
        }
    },

    initComponent: function(){
        Ext4.apply(this, {
            border: false,
            items: [this.getGridConfig()],
            buttons: this.hideButtons ? null : this.getButtonConfig()
        });

        this.callParent();

        this.addEvents('storeloaded');
    },

    getStore: function(){
        if (this.store)
            return this.store;

        const formType = LABKEY.ActionURL.getParameter('formType');
        const caseCategory = NBRI_EHR.panel.SelectCasePanel.getCaseCategory()[formType];

        this.store = Ext4.create('LABKEY.ext4.data.Store', {
            schemaName: 'study',
            queryName: 'cases',
            columns: 'lsid,objectid,caseId,taskId,Id,date,enddate,reviewdate,category,openRemark,closeRemark,performedby,problemCategory,problemSubcategory,plan',
            filterArray: [
                LABKEY.Filter.create('Id', this.animalId, LABKEY.Filter.Types.EQUAL),
                LABKEY.Filter.create('isOpen', true, LABKEY.Filter.Types.EQUAL),
                LABKEY.Filter.create('category', caseCategory, LABKEY.Filter.Types.EQUAL),
                LABKEY.Filter.create('QCState/Label', "Completed", LABKEY.Filter.Types.EQUAL),

            ],
            autoLoad: true,
            listeners: {
                scope: this,
                synccomplete: function(store){
                    var grid = this.down('grid');
                    if (grid){
                        grid.getView().refresh();
                    }

                    EHR.DemographicsCache.clearCache(this.animalId);
                },
                load: function(store){
                    //NOTE: consumed by SnapshotPanel
                    this.fireEvent('storeloaded', this);
                },
                exception: function(store){
                    //NOTE: refresh the store in order to avoid invalid data on the client
                    store.load();
                }
            }
        });

        return this.store;
    },

    getGridConfig: function(){
        var me = this;
        return {
            xtype: 'grid',
            cls: 'ldk-grid', //variable row height
            border: false,
            store: this.getStore(),
            viewConfig: {
                loadMask: true
            },
            columns: [{
                xtype: 'actioncolumn',
                width: 40,
                icon: LABKEY.ActionURL.getContextPath() + '/_images/editprops.png',
                tooltip: 'Select',
                handler: function(view, rowIndex, colIndex, item, e, rec){
                    if (me.casepanel){
                        EHR.DemographicsCache.reportCaseSelected(rec);
                        this.up("window").close();
                    }
                }

            },{
                header: 'Open Date',
                dataIndex: 'date',
                xtype: 'datecolumn',
                format: LABKEY.extDefaultDateFormat,
                width: 110
            },{
                header: 'Close Date',
                dataIndex: 'enddate',
                xtype: 'datecolumn',
                format: LABKEY.extDefaultDateFormat,
                width: 110
            },{
                header: 'Problem',
                dataIndex: 'problemCategory',
                tdCls: 'ldk-wrap-text',
                width: 150
            },{
                header: 'Subcategory',
                dataIndex: 'problemSubcategory',
                tdCls: 'ldk-wrap-text',
                width: 150
            },{
                header: 'Open Remark',
                dataIndex: 'openRemark',
                tdCls: 'ldk-wrap-text',
                width: 350
            },{
                header: 'Close Remark',
                dataIndex: 'closeRemark',
                tdCls: 'ldk-wrap-text',
                width: 350
            }]
        }
    }
});






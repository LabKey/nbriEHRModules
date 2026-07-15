/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.form.field.SelectCaseButton', {
    extend: 'Ext.button.Button',
    alias: 'widget.nbri_ehr-selectcasebutton',

    text: 'Use Existing Case',
    margin: '0 0 0 20',
    animalId: null,

    handleCaseSelect: function(caseid, hideEdit){
        this.enableBubble('animalchange');

        const store = this.getStore(caseid);
        this.on('storeloaded', function(panel){

            const rec = store.getAt(0);
            EHR.DemographicsCache.reportCaseSelected(rec);
            panel.fireEvent('animalchange', rec.data.Id);

            let editCases = this.up('#upperPanel')?.query('nbri_ehr-editCases')?.[0];
            editCases.fieldEnableChange(true);
            if (hideEdit)
                editCases.editBtn.hide();
        });
    },

    onRender: function(){
        this.callParent(arguments);

        const caseid = LABKEY.ActionURL.getParameter('caseid');
        const edit = LABKEY.ActionURL.getParameter('edit');
        if (caseid){
            this.handleCaseSelect(caseid, !edit);
        }
        else {
            const taskid = LABKEY.ActionURL.getParameter('taskId') || LABKEY.ActionURL.getParameter('taskid');
            if (taskid) {
                let record = EHR.DataEntryUtils.getBoundRecord(this.up('panel'));
                if (!record || !record.get('Id')) {
                    let tasksPanel = this.up('#upperPanel')?.query('ehr-formpanel')?.find(panel => panel.formConfig.name === 'tasks');
                    let caseid = tasksPanel?.store?.data?.get(0)?.get('caseid');
                    if (caseid) {
                        this.handleCaseSelect(caseid, false);
                    }
                }
            }
        }
    },

    onAnimalChange: function(id){
        this.animalId = id;
    },

    handler: function(btn){
        if (this.animalId){
            var casepanel = this.up('panel').up('panel');
            Ext4.create('NBRI_EHR.window.SelectCaseWindow', {animalId: this.animalId, casepanel: casepanel}).show();
        }
        else {
            console.log('no id');
        }
    },

    getStore: function(caseid){
        if (this.store)
            return this.store;

        this.store = Ext4.create('LABKEY.ext4.data.Store', {
            schemaName: 'study',
            queryName: 'Cases',
            columns: 'lsid,objectid,caseid,taskid,Id,date,enddate,reviewdate,category,openRemark,closeRemark,performedby,plan,problemCategory,problemSubcategory',
            filterArray: [
                LABKEY.Filter.create('objectid', caseid, LABKEY.Filter.Types.EQUAL),
            ],
            autoLoad: true,
            listeners: {
                scope: this,
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
});

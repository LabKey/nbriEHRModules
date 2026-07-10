/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Buttons specific to the Death-Necropsy form
 */

EHR.DataEntryUtils.registerDataEntryFormButton('DEATHSUBMIT', {
    text: 'Submit Death',
    name: 'submitDeath',
    errorThreshold: 'ERROR',
    successURL: LABKEY.ActionURL.buildURL('ehr', 'enterData.view'),
    itemId: 'submitDeathBtn',
    disabled: true,
    requiredQC: 'Request: Pending',
    targetQC: 'Request: Pending',
    handler: function(submitDeathBtn){
        Ext4.Msg.confirm('Confirm', 'You are about to submit a death record (any necropsy info entered will be discarded during this step). This will send an email notification to the appropriate parties to enter Necropsy. Submit death?', function(val){
            if (val == 'yes') {
                var panel = submitDeathBtn.up('ehr-dataentrypanel');
                panel.storeCollection.clientStores.each((store) => {
                    if (store.storeId && (store.storeId.indexOf('necropsy') > -1) || store.storeId.indexOf('grossPathology') > -1 ||  store.storeId.indexOf('tissueDisposition') > -1) {
                        store.removeAll();
                    }
                });

                // get and set 'Veterinarians' as the assignedTo value
                var assignedToCombo = panel.down('ehr-usersandgroupscombo');
                if (assignedToCombo) {
                    var assignedToStore = assignedToCombo.getStore();
                    if (assignedToStore.count() > 0) {
                        var assignedToRec = assignedToStore.findRecord('DisplayName', 'Veterinarians');
                        if (assignedToRec) {
                            var taskStore = panel.storeCollection.getServerStoreForQuery('ehr', 'tasks');
                            taskStore.getAt(0).set('assignedto', assignedToRec.get('UserId'));
                            panel.storeCollection.transformServerToClient();
                        }
                    }
                }

                panel.onSubmit(submitDeathBtn);
            }
        }, this);
    },
    disableOn: 'ERROR'
});

EHR.DataEntryUtils.registerDataEntryFormButton('DEATH_NECROPSY_VET_REVIEW', {
    text: 'Submit Necropsy for Review',
    name: 'review',
    requiredQC: 'Review Required',
    targetQC: 'Review Required',
    errorThreshold: 'WARN',
    successURL: LABKEY.ActionURL.getParameter('returnUrl') || LABKEY.ActionURL.buildURL('ehr', 'enterData.view'),
    disabled: true,
    itemId: 'reviewBtn',
    disableOn: 'ERROR',
    handler: function(btn){
        var panel = btn.up('ehr-dataentrypanel');

        Ext4.create('NBRI_EHR.window.DeathNecropsySubmitForReviewWindow', {
            dataEntryPanel: panel,
            dataEntryBtn: btn,
            reviewRequiredRecipient: panel.formConfig.defaultReviewRequiredPrincipal
        }).show();

    }
});

Ext4.define('NBRI_EHR.window.DeathNecropsySubmitForReviewWindow', {
    extend: 'Ext.window.Window',

    initComponent: function(){
        Ext4.apply(this, {
            closeAction: 'destroy',
            modal: true,
            title: 'Submit For Review',
            width: 430,
            buttons: [{
                text:'Submit',
                disabled:false,
                itemId: 'submit',
                scope: this,
                handler: function(btn){
                    var win = btn.up('window');
                    var assignedTo = win.down('#assignedTo').getValue();
                    if(!assignedTo){
                        alert('Must assign this task to someone');
                        return;
                    }

                    var taskStore = this.dataEntryPanel.storeCollection.getServerStoreForQuery('ehr', 'tasks');
                    taskStore.getAt(0).set('assignedto', assignedTo);
                    this.dataEntryPanel.storeCollection.transformServerToClient();
                    this.dataEntryPanel.onSubmit(this.dataEntryBtn);
                    win.close();
                }
            },{
                text: 'Cancel',
                scope: this,
                handler: function(btn){
                    btn.up('window').hide();
                }
            }],
            items: [{
                bodyStyle: 'padding:5px;',
                items: [{
                    html: this.reviewRecipientMsg || '',
                    border: false,
                    style: 'padding-bottom: 10px;',
                    hidden: !this.reviewRecipientMsg
                },{
                    xtype: 'labkey-combo',
                    forceSelection: true,
                    fieldLabel: 'Assign To',
                    width: 400,
                    queryMode: 'local',
                    store: {
                        type: 'labkey-store',
                        schemaName: 'ehr_lookups',
                        queryName: 'veterinarians',
                        columns: 'UserId, DisplayName',
                        autoLoad: true
                    },
                    value: this.getDefaultRecipient(),
                    displayField: 'DisplayName',
                    valueField: 'UserId',
                    itemId: 'assignedTo',
                    id: 'assignedTo',
                    anyMatch: true,
                    caseSensitive: false,
                }]
            }]
        });

        this.callParent(arguments);
    },

    getDefaultRecipient: function(){
        return this.reviewRequiredRecipient;
    }
});
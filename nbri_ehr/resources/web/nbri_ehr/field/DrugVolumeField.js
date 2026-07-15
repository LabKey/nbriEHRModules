/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.define('NBRI_EHR.field.DrugVolumeField', {
    extend: 'EHR.form.field.DrugVolumeField',
    alias: 'widget.nbri_ehr-drugvolumefield',

    triggerCls: 'x4-form-search-trigger',
    triggerToolTip: 'Click to calculate amount based on concentration and volume',

    initComponent: function(){
        this.callParent(arguments);
        this.getSnomedStore();
    },

    getSnomedStore: function(){
        if (NBRI_EHR._snomedStore)
            return NBRI_EHR._snomedStore;

        var storeId = ['ehr_lookups', 'snomed', 'code', 'meaning'].join('||');

        NBRI_EHR._snomedStore = Ext4.create('LABKEY.ext4.data.Store', {
            type: 'labkey-store',
            schemaName: 'ehr_lookups',
            queryName: 'snomed',
            columns: 'code,meaning',
            sort: 'meaning',
            storeId: storeId,
            autoLoad: true,
            getRecordForCode: function(code){
                var recIdx = this.findExact('code', code);
                if (recIdx != -1){
                    return this.getAt(recIdx);
                }
            }
        });

        return NBRI_EHR._snomedStore;
    },

    onTriggerClick: function(){
        var record = EHR.DataEntryUtils.getBoundRecord(this);
        if (!record){
            return;
        }

        if (!record.get('code') || !record.get('Id')){
            Ext4.Msg.alert('Error', 'Must enter the Animal Id and treatment');
            return;
        }

        Ext4.create('NBRI_EHR.window.DrugAmountWindow', {
            targetStore: record.store,
            formConfig: record.sectionCfg,
            boundRecord: record
        }).show();
    }
});
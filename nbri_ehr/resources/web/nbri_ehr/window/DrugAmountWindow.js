/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
(function getSnomedStore(){
    if (NBRI_EHR._snomedStore)
        return NBRI_EHR._snomedStore;

    let storeId = ['ehr_lookups', 'snomed', 'code', 'meaning'].join('||');

    NBRI_EHR._snomedStore = Ext4.create('LABKEY.ext4.data.Store', {
        type: 'labkey-store',
        schemaName: 'ehr_lookups',
        queryName: 'snomed',
        columns: 'code,meaning',
        sort: 'meaning',
        storeId: storeId,
        autoLoad: true,
        getRecordForCode: function(code){
            let recIdx = this.findExact('code', code);
            if (recIdx != -1){
                return this.getAt(recIdx);
            }
        }
    });

    return NBRI_EHR._snomedStore;
})();

Ext4.define('NBRI_EHR.window.DrugAmountWindow', {
    extend: 'EHR.window.DrugAmountWindow',

    initComponent: function(){
        this.callParent(arguments);
    },

    getDrugItems: function(){

        var numCols = 13;
        var items = [{
            html: '<b>Animal</b>'
        },{
            html: '<b>Drug</b>'
        },{
            html: '<b>Weight (kg)</b>',
            width: 100
        },{
            html: '<b>Conc</b>'
        },{
            html: '<b>Units</b>'
        },{
            html: '<b>Dosage</b>'
        },{
            html: '<b>Units</b>'
        },{
            html: '<b>Vol</b>'
        },{
            html: '<b>Units</b>'
        },{
            html: '<b>Amount</b>'
        },{
            html: '<b>Units</b>'
        },{
            html: '<b>Auto Calc?</b>'
        },{
            html: '' //placeholder for messages
        }];

        var fields = [{
            name: 'concentration',
            width: 70
        },{
            name: 'conc_units',
            width: 80
        },{
            name: 'dosage',
            width: 70
        },{
            name: 'dosage_units',
            width: 80
        },{
            name: 'volume',
            width: 70
        },{
            name: 'vol_units',
            width: 80
        },{
            name: 'amount',
            width: 70
        },{
            name: 'amount_units',
            width: 80
        }];

        this.snomedStore = NBRI_EHR._snomedStore;
        LDK.Assert.assertTrue('SNOMED store is not done loading', !this.snomedStore.isLoading());

        Ext4.Array.forEach(this.getTargetRecords(), function(record, recordIdx){
            if (!record.get('Id') || !record.get('code')){
                return;
            }

            items.push({
                xtype: 'displayfield',
                value: record.get('Id'),
                record: record,
                recordIdx: recordIdx,
                fieldName: 'Id'
            });

            var snomedRec = this.snomedStore.getRecordForCode(record.get('code'));
            items.push({
                xtype: 'displayfield',
                fieldName: 'code',
                snomedCode: record.get('code'),
                recordIdx: recordIdx,
                value: snomedRec ? snomedRec.get('meaning') : record.get('code')
            });

            items.push({
                xtype: 'ldk-numberfield',
                hideTrigger: true,
                decimalPrecision: 3,
                keyNavEnabled: false,
                width: 80,
                fieldName: 'weight',
                recordIdx: recordIdx,
                animalId: record.get('Id'),
                value: this.weights[record.get('Id')] ? this.weights[record.get('Id')].weight : null,
                listeners: {
                    scope: this,
                    change: this.onFieldChange
                }
            });

            Ext4.Array.forEach(fields, function(fieldObj){
                var fieldName = fieldObj.name;
                var editor, found = false;
                Ext4.each(this.formConfig.fieldConfigs, function(field, idx){
                    if (fieldName == field.name){
                        var cfg = Ext4.apply({}, field);
                        cfg = EHR.model.DefaultClientModel.getFieldConfig(cfg, this.formConfig.configSources);

                        editor = LABKEY.ext4.Util.getGridEditorConfig(cfg);
                        if (cfg.jsonType != 'string'){
                            editor.xtype = 'ldk-numberfield';
                            editor.hideTrigger = true;
                        }

                        found = true;

                        return false;
                    }
                }, this);

                LDK.Assert.assertTrue('Unable to find target field in DrugAmountWindow: ' + fieldName, found);

                if (editor){
                    editor.width = fieldObj.width;
                    editor.value = record.get(fieldName);
                    editor.fieldName = fieldName;
                    editor.recordIdx = recordIdx;
                    editor.animalId = record.get('Id');
                    if (editor.xtype == 'ldk-numberfield'){
                        editor.hideTrigger = true;
                    }
                    editor.listeners = editor.listeners || {};
                    editor.listeners.scope = this;
                    editor.listeners.change = this.onFieldChange;

                    items.push(editor);
                }
                else {
                    items.push({
                        html: ''
                    });
                }
            }, this);

            items.push({
                xtype: 'checkbox',
                recordIdx: recordIdx,
                fieldName: 'include',
                checked: true
            });

            items.push({
                xtype: 'displayfield',
                width: 80,
                recordIdx: recordIdx,
                fieldName: 'messages'
            });
        }, this);

        return [{
            html: 'This tab shows one row per drug, allowing you to review and re-calculate amount/volume for weight-based drugs.  It will pre-populate doses based on the formulary.  Any drug using kg in the dosage will have the option to auto-calculate dose.  To exclude a given drug from auto-calculation, check the box to the right.  Use the \'Recalculate\' button in the bottom-right to recalculate values.',
            border: false,
            style: 'padding-bottom: 10px;'
        },{
            border: false,
            itemId: 'theTable',
            layout: {
                type: 'table',
                columns: numCols
            },
            defaults: {
                border: false,
                style: 'margin-left: 5px;margin-right: 5px;'
            },
            items: items
        },{
            layout: {
                type: 'vbox',
                align: 'right'
            },
            border: false,
            style: 'margin-bottom: 5px;margin-top: 5px;margin-right: 110px;',
            items: [{
                xtype: 'button',
                text: 'Recalculate All',
                border: true,
                itemId: 'recalculate',
                menu: [{
                    text: 'Recalculate Both Amount/Volume',
                    scope: this,
                    handler: function(btn){
                        var panel  = this.down('#drugTab');
                        var cbs = panel.query('checkbox');
                        for (var i=0;i<cbs.length;i++){
                            if (cbs[i].getValue()){
                                this.recalculateRow(cbs[i].recordIdx, '*');
                            }
                        }
                    }
                },{
                    text: 'Recalculate Amount Based On Volume',
                    scope: this,
                    handler: function(btn){
                        var panel  = this.down('#drugTab');
                        var cbs = panel.query('checkbox');
                        for (var i=0;i<cbs.length;i++){
                            if (cbs[i].getValue()){
                                this.recalculateRow(cbs[i].recordIdx, 'volume');
                            }
                        }
                    }
                },{
                    text: 'Recalculate Volume Based On Amount',
                    scope: this,
                    handler: function(btn){
                        var panel  = this.down('#drugTab');
                        var cbs = panel.query('checkbox');
                        for (var i=0;i<cbs.length;i++){
                            if (cbs[i].getValue()){
                                this.recalculateRow(cbs[i].recordIdx, 'amount');
                            }
                        }
                    }
                }]
            }]
        }];
    },

    getDistinctCodes: function() {
        var codes = [];

        Ext4.Array.forEach(this.getTargetRecords(), function(record, recordIdx){
            if (!record.get('Id') || !record.get('code')){
                return;
            }

            codes.push(record.get('code'));
        }, this);

        codes = Ext4.unique(codes);

        this.formularyStore = EHR.DataEntryUtils.getFormularyStore();
        this.snomedStore = NBRI_EHR._snomedStore;
        LDK.Assert.assertTrue('Formulary store is not done loading', !this.formularyStore.isLoading());
        LDK.Assert.assertTrue('SNOMED store is not done loading', !this.snomedStore.isLoading());

        var codeMap = [];
        Ext4.Array.forEach(codes, function(code){
            var snomedRec = this.snomedStore.getRecordForCode(code);
            var formularyMap = this.formularyStore.getFormularyValues(code);
            var valMap = {
                code: code,
                meaning: snomedRec ? snomedRec.get('meaning') : null
            };

            if (formularyMap && !Ext4.Object.isEmpty(formularyMap)){
                if (snomedRec)
                    Ext4.apply(valMap, formularyMap);
            }
            else {
                console.log('unknown or malformed code: ' + code);
                console.log(formularyMap);
            }

            codeMap.push(valMap);
        }, this);

        codeMap = LDK.Utils.sortByProperty(codeMap, 'meaning');

        return codeMap;
    },

});

EHR.DataEntryUtils.registerGridButton('NBRI_DRUG_AMOUNT_HELPER', function(config){
    return Ext4.Object.merge({
        text: 'Review Drug Amount(s)',
        xtype: 'button',
        tooltip: 'Click to set the drug amounts',
        handler: function(btn){
            var grid = btn.up('gridpanel');

            Ext4.create('NBRI_EHR.window.DrugAmountWindow', {
                targetGrid: grid,
                targetStore: grid.store,
                formConfig: grid.formConfig
            }).show();
        }
    });
});
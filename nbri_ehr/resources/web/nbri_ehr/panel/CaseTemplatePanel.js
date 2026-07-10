/*
 * Copyright (c) 2024 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.panel.CaseTemplatePanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.nbri_ehr-casetemplatepanel',
    plugins: ['ehr-collapsibleDataEntryPanel'],

    initComponent: function(){
        var buttons = [];
        LDK.Assert.assertNotEmpty('No data entry panel', this.dataEntryPanel);
        var btnCfg = EHR.DataEntryUtils.getDataEntryFormButton('APPLYFORMTEMPLATE_NO_ID');
        if (btnCfg){
            btnCfg = this.dataEntryPanel.configureButton(btnCfg);
            if (btnCfg){
                btnCfg.defaultDate = new Date();
                btnCfg.text = 'Apply Template To Form';
                buttons.push(btnCfg);
            }
        }

        Ext4.apply(this, {
            defaults: {

            },
            bodyStyle: 'padding: 5px;',
            title: this.getTitle(),
            items: [{
                html: this.getHtml(),
                maxWidth: Ext4.getBody().getWidth() * 0.8,
                style: 'padding-top: 10px;padding-bottom: 10px;',
                border: false
            },{
                layout: 'hbox',
                border: false,
                defaults: {
                    style: 'margin-right: 5px;'
                },
                items: buttons
            }]
        });

        this.formConfig.initCollapsed = true;
        this.formConfig.dataDependentCollapseHeader = false;

        this.callParent(arguments);
    },

    getTitle: function(){
        return 'Case Template';
    },

    getHtml: function(){
        return 'Start with a case template by clicking \'Apply Template To Form\' below or fill out the sections directly.';
    }
});
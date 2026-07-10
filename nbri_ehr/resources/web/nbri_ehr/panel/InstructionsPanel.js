/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.panel.InstructionsPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.nbri_ehr-instructionspanel',
    plugins: ['ehr-collapsibleDataEntryPanel'],

    initComponent: function(){
        Ext4.apply(this, {
            defaults: {
                border: false
            },
            items: [this.getItems()]
        });

        this.formConfig.initCollapsed = true;
        this.formConfig.dataDependentCollapseHeader = false;

        this.callParent(arguments);
    },

    getItems: function(){
        return {};
    }
});
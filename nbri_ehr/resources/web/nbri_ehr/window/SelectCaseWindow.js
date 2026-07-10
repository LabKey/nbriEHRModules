/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * @cfg {String} animalId
 * @cfg {Object} panel
 */
Ext4.define('NBRI_EHR.window.SelectCaseWindow', {
    extend: 'Ext.window.Window',

    width: 1200,
    minHeight: 50,

    initComponent: function(){
        Ext4.apply(this, {
            title: 'Select Case: ' + this.animalId,
            modal: true,
            closeAction: 'destroy',
            items: [{
                xtype: 'nbri_ehr-selectcasepanel',
                animalId: this.animalId,
                hideButtons: true,
                casepanel: this.casepanel
            }],
            buttons: this.getButtonConfig()
        });

        this.callParent(arguments);
    },

    getButtonConfig: function(){
        var buttons = NBRI_EHR.panel.SelectCasePanel.getButtonConfig();
        buttons.push({
            text: 'Close',
            handler: function(btn){
                btn.up('window').close();
            }
        });

        return buttons;
    }
});

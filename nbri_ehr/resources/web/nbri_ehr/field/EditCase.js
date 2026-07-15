/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.form.EditCases', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.nbri_ehr-editCases',
    layout: {
        type: 'hbox',
    },
    editing: false,
    disabled: false,
    readonly: false,

    initComponent: function(){
        this.on('resize', function () {
            this.setWidth(700);
        });

        let caseid = LABKEY.ActionURL.getParameter('caseid');
        let taskid = LABKEY.ActionURL.getParameter('taskId');

        this.editBtn = Ext4.create('Ext.button.Button', {
            text: 'Edit',
            margin: '20 10 0 0',
            hidden: !caseid && !taskid,
            scope: this,
            handler: this.onEditButtonClick
        });

        this.items = [
            this.editBtn,
        ]

        this.mon(EHR.DemographicsCache, 'caseselected', this.onCaseSelect, this);

        this.callParent();
    },

    disableItem(item, disable) {
        if (disable) {
            item.disable();
        }
        else {
            item.enable();
        }
    },

    disabledDisplay: function(item, disable){
        if (item.labelEl && item.inputEl) {
            if (disable) {
                item.labelEl.addCls('form-panel-input-disabled');
                item.inputEl.addCls('form-panel-input-disabled');
            }
            else {
                item.labelEl.removeCls('form-panel-input-disabled');
                item.inputEl.removeCls('form-panel-input-disabled');
            }
        }

        if (item.labelEl && item.containerEl) {
            if (disable) {
                item.labelEl.addCls('form-panel-input-disabled');
                item.containerEl.addCls('form-panel-input-disabled');
            }
            else {
                item.labelEl.removeCls('form-panel-input-disabled');
                item.containerEl.removeCls('form-panel-input-disabled');
            }
        }
    },

    fieldEnableChange: function(disable){
        var panel = this.up('panel');
        panel.items.each(function(item){
            if (item.name != 'objectid'){
                this.disableItem(item, disable);
                this.disabledDisplay(item, disable);
            }
        }, this);
    },

    onEditButtonClick: function(){
        if (this.disabled) {
            this.disabled = false;
            this.fieldEnableChange(false);
        }
        else {
            this.disabled = true;
            this.fieldEnableChange(true);
        }
    },

    onCaseSelect: function(){
        this.fieldEnableChange(true);
        if (this.readonly) {
            return;
        }
        this.editing = true;
        this.disabled = true;
        this.editBtn.show();
    },

    onNewCase: function(){
        EHR.DemographicsCache.reportCaseCreated();
        this.editing = false;
        this.editBtn.hide();
        this.fieldEnableChange(false);
    },

    disable: function(){},

    enable: function(){}
});
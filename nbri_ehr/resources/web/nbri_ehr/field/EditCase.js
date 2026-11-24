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
            hidden: this.readonly || (!caseid && !taskid),
            scope: this,
            handler: this.onEditButtonClick
        });

        this.items = [
            this.editBtn,
        ]

        this.mon(EHR.DemographicsCache, 'caseselected', this.onCaseSelect, this);
        this.on('afterrender', this.bindFormPanel, this, {single: true});

        this.callParent();
    },

    // A date-time field's own date and time inputs ignore their container's disabled state
    getInnerDateTimeFields: function(item){
        return item.dateField && item.timeField ? [item.dateField, item.timeField] : [];
    },

    disableItem(item, disable) {
        [item].concat(this.getInnerDateTimeFields(item)).forEach(function(field){
            if (disable) {
                field.disable();
            }
            else {
                field.enable();
            }
        });
    },

    disabledDisplay: function(item, disable){
        this.getInnerDateTimeFields(item).forEach(function(field){
            if (field.inputEl) {
                if (disable)
                    field.inputEl.addCls('form-panel-input-disabled');
                else
                    field.inputEl.removeCls('form-panel-input-disabled');
            }
        });

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
        // A case opened from its own task never fires caseselected, so the saved record also counts
        var record = EHR.DataEntryUtils.getBoundRecord(this);
        var savedCase = this.editing || (record && !record.phantom);
        panel.items.each(function(item){
            if (item.name === 'objectid')
                return;

            // A saved case stays bound to its animal, so Edit must never reopen the Id.
            var itemDisable = disable || (savedCase && item.name === 'Id');

            this.disableItem(item, itemDisable);
            this.disabledDisplay(item, itemDisable);
        }, this);
    },

    // A saved case arrives by record binding, and caseselected only fires on some load paths
    bindFormPanel: function(){
        var formPanel = this.up('form');
        if (!formPanel || this.readonly)
            return;

        this.mon(formPanel, 'bindrecord', this.lockSavedCase, this);
        if (formPanel.rendered)
            this.lockSavedCase(formPanel);
        else
            this.mon(formPanel, 'afterrender', this.lockSavedCase, this, {single: true});
    },

    lockSavedCase: function(formPanel){
        var record = formPanel.getBoundRecord();
        if (formPanel.rendered && record && !record.phantom && !this.editing)
            this.onCaseSelect();
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
        this.editing = true;
        this.fieldEnableChange(true);
        if (this.readonly) {
            return;
        }
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
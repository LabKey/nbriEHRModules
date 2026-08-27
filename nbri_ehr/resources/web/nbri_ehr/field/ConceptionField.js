/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * The conception Id field on the Births form.  The value is never typed: clicking it opens the same window the Start
 * with Conception button uses, and the conception picked there is copied onto the row that was clicked.  It serves as
 * both the grid's cell editor and the row editor's form field, so it looks for the row it is editing in either place.
 */
Ext4.define('NBRI_EHR.field.ConceptionField', {
    extend: 'Ext.form.field.Trigger',
    alias: 'widget.nbri_ehr-conceptionField',

    editable: false,
    triggerCls: 'x4-form-search-trigger',

    initComponent: function(){
        this.callParent(arguments);

        if (!this.triggerToolTip){
            this.triggerToolTip = 'Click to pick the conception this birth came from';
        }

        this.on('render', function(){
            this.triggerEl.set({'data-qtip': Ext4.htmlEncode(this.triggerToolTip)});
        }, this);
    },

    initEvents: function(){
        this.callParent(arguments);

        if (this.readOnly){
            return;
        }

        // the base class only watches the trigger itself, but the field holds nothing that can be typed, so a click
        // anywhere in it should open the window
        this.mon(this.inputEl, 'click', this.onTriggerClick, this);

        // as a cell editor the field is created and focused by the same single click that starts editing the cell,
        // which the grid consumes.  Opening on focus as well keeps that one click enough.  In the row editor focus
        // arrives by tabbing through the form, where a window must not open, so this is limited to the editor.
        if (this.inEditor){
            this.on('focus', this.onTriggerClick, this);
        }
    },

    onTriggerClick: function(){
        // both the click and the focus can report the same gesture, and the window is modal, so only ever open one
        if (this.readOnly || this.disabled || this.pickerOpen){
            return;
        }

        this.pickerOpen = true;

        var target = this.getTargetRow();
        if (!target){
            this.pickerOpen = false;
            return;
        }

        var picker = Ext4.create('NBRI_EHR.window.StartWithConceptionWindow', target);
        picker.on('destroy', function(){
            this.pickerOpen = false;
        }, this);
        picker.show();
    },

    // as a cell editor the row comes from the grid's editing plugin; in the row editor it is whatever record the form
    // is bound to.  Either way the window is handed the row so it updates it rather than adding a new one.
    getTargetRow: function(){
        var grid = this.up('gridpanel');
        if (grid){
            var editingPlugin = grid.getPlugin(grid.editingPluginId);
            var record = editingPlugin ? editingPlugin.getActiveRecord() : null;

            // close the cell editor before the modal window covers the grid; an editor left open would write its
            // stale value back over the conception copied onto the row
            if (editingPlugin){
                editingPlugin.completeEdit();
            }

            return {
                targetStore: grid.store,
                formConfig: grid.formConfig,
                targetRecord: record
            };
        }

        var form = this.up('form');
        var boundRecord = form ? form.getForm().getRecord() : null;
        if (!boundRecord){
            return null;
        }

        return {
            targetStore: boundRecord.store,
            targetRecord: boundRecord
        };
    }
});

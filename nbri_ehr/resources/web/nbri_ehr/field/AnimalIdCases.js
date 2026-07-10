/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.form.AnimalIdCases', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.nbri_ehr-animalIdCases',
    layout: {
        type: 'hbox',
    },

    initComponent: function(){
        this.on('resize', function(){
           this.setWidth(700);
        });

        this.idField = Ext4.create('EHR.form.AnimalIdUpperField', {
            id: 'animal_id',
            name: 'Id',
            width: 245
        });

        this.existingCasesBtn = Ext4.create('NBRI_EHR.form.field.SelectCaseButton', {
            id: 'existing_cases',
        });

        this.label = Ext4.create('Ext.form.Label', {
            text: 'Id:',
            forId: 'animal_id',
            margin: '0 138 0 0',
            cls: 'black-text-color'
        });

        this.items = [
            this.label,
            this.idField,
            this.existingCasesBtn
        ]

        this.existingCasesBtn.disable();

        if (this.idField){
            this.mon(this.idField, 'animalchange', this.onAnimalChange, this, {buffer: 1000});
        }

        this.callParent();
    },

    onAnimalChange: function(){
        this.existingCasesBtn.onAnimalChange(this.idField.getValue());
        this.existingCasesBtn.enable();
    },

    setValue: function(value){
        this.idField.setValue(value);
    },

    disable: function(){
        this.idField.disable();
        this.label.addCls('form-panel-input-disabled');
        this.idField.inputEl.addCls('form-panel-input-disabled');

    },

    enable: function(){
        this.idField.enable();
        this.label.removeCls('form-panel-input-disabled');
        this.idField.inputEl.removeCls('form-panel-input-disabled');
    }
});
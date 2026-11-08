/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// Holds the animal, date and technician of a daily health log entry and writes them into every row of the entry's grid.
Ext4.define('NBRI_EHR.panel.DailyHealthEntryPanel', {
    extend: 'Ext.form.Panel',
    alias: 'widget.nbri_ehr-dailyhealthentrypanel',

    ENTRY_FIELDS: ['Id', 'date', 'performedby'],

    bodyStyle: 'padding: 5px;',

    initComponent: function(){
        this.logStore = this.dataEntryPanel.storeCollection.clientStores.findBy(function(s){
            return /-dailyHealthLog$/.test(s.storeId);
        });

        Ext4.apply(this, {
            defaults: {
                border: false
            },
            items: this.getEntryFieldConfigs()
        });

        this.callParent(arguments);

        // the date field relays its date and time parts' change events, which carry the part rather than the field
        Ext4.Array.forEach(this.ENTRY_FIELDS, function(name){
            var field = this.getForm().findField(name);
            this.mon(field, 'change', function(){
                this.onEntryFieldChange(field);
            }, this, {buffer: 100});
        }, this);

        this.mon(this.logStore, 'add', this.onRecordAdd, this);
        this.mon(this.logStore, 'validation', this.showRecordErrors, this, {buffer: 200});

        if (this.dataEntryPanel.storeCollection.hasLoaded)
            this.loadEntryValues();
        else
            this.mon(this.dataEntryPanel.storeCollection, 'initialload', this.loadEntryValues, this, {single: true});
    },

    getEntryFieldConfigs: function(){
        var section = this.logStore.sectionCfg;
        var fields = EHR.model.DefaultClientModel.getFieldConfigs(section.fieldConfigs, section.configSources, this.extraMetaData);

        return Ext4.Array.map(this.ENTRY_FIELDS, function(name){
            var field = Ext4.Array.filter(fields, function(f){
                return f.name === name;
            })[0];
            if (field.jsonType === 'date' && field.extFormat && Ext4.Date.formatContainsHourInfo(field.extFormat))
                field.xtype = 'xdatetime';

            return Ext4.apply(EHR.DataEntryUtils.getFormEditorConfig(field), {
                labelWidth: EHR.form.Panel.defaultLabelWidth,
                width: EHR.form.Panel.defaultFieldWidth
            });
        }, this);
    },

    // an existing entry supplies its values; a new one takes the defaults a new row would get
    loadEntryValues: function(){
        var source = this.logStore.getCount() ? this.logStore.getAt(0) : this.logStore.model.create();
        var values = {};
        Ext4.Array.forEach(this.ENTRY_FIELDS, function(name){
            values[name] = source.get(name);
        });

        this.getForm().setValues(values);
    },

    onEntryFieldChange: function(field){
        var value = field.getValue();
        this.logStore.each(function(rec){
            if (!this.isSameValue(value, rec.get(field.name)))
                rec.set(field.name, value);
        }, this);
    },

    onRecordAdd: function(store, records){
        var form = this.getForm();
        var values = form.getFieldValues();
        Ext4.Array.forEach(records, function(rec){
            var changed = {};
            Ext4.Array.forEach(this.ENTRY_FIELDS, function(name){
                // an empty field takes the row's default, and its change event passes that on to every row
                if (Ext4.isEmpty(values[name])){
                    if (!Ext4.isEmpty(rec.get(name))){
                        values[name] = rec.get(name);
                        form.findField(name).setValue(values[name]);
                    }
                }
                else if (!this.isSameValue(values[name], rec.get(name)))
                    changed[name] = values[name];
            }, this);
            if (!Ext4.Object.isEmpty(changed))
                rec.set(changed);
        }, this);
    },

    // the grid leaves these columns out, so their errors would otherwise show only in each row's error tooltip
    showRecordErrors: function(){
        if (!this.logStore.getCount())
            return;

        var errors = this.logStore.getAt(0).validate();
        Ext4.Array.forEach(this.ENTRY_FIELDS, function(name){
            var field = this.getForm().findField(name);
            var messages = Ext4.Array.map(errors.getByField(name), function(e){
                return e.message;
            });
            if (messages.length)
                field.markInvalid(messages);
            else
                field.validate();
        }, this);
    },

    isSameValue: function(a, b){
        return Ext4.isDate(a) && Ext4.isDate(b) ? a.getTime() === b.getTime() : a === b;
    }
});

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
// A new entry's task id doubles as its daily health logEntryId, which is a UUIDv7 like the ones scanners generate.
Ext4.define('NBRI_EHR.panel.DailyHealthDataEntryPanel', {
    extend: 'EHR.panel.TaskDataEntryPanel',

    initComponent: function(){
        this.taskId = this.taskId || LABKEY.ActionURL.getParameter('taskid') || LABKEY.ActionURL.getParameter('taskId') || this.generateUUIDv7();
        this.callParent();
    },

    // Every row of a log entry must share its animal, date and technician, and each new row would otherwise default its own time.
    ENTRY_FIELDS: ['Id', 'date', 'performedby'],

    onStoreCollectionInitialLoad: function(){
        this.callParent(arguments);

        var store = this.storeCollection.clientStores.findBy(function(s){
            return /-dailyHealthLog$/.test(s.storeId);
        });
        if (store){
            store.on('add', this.onDailyHealthAdd, this);
            store.on('update', this.onDailyHealthUpdate, this);
        }
    },

    onDailyHealthAdd: function(store, records){
        var source = null;
        store.each(function(rec){
            if (records.indexOf(rec) === -1){
                source = rec;
                return false;
            }
        });
        if (source){
            Ext4.Array.forEach(records, function(rec){
                this.copyEntryFields(source, rec, this.ENTRY_FIELDS);
            }, this);
        }
    },

    onDailyHealthUpdate: function(store, rec, operation, modifiedFieldNames){
        if (operation !== Ext4.data.Model.EDIT || this.syncingEntryFields)
            return;

        var changed = Ext4.Array.intersect(modifiedFieldNames || [], this.ENTRY_FIELDS);
        if (!changed.length)
            return;

        this.syncingEntryFields = true;
        try {
            store.each(function(other){
                if (other !== rec)
                    this.copyEntryFields(rec, other, changed);
            }, this);
        }
        finally {
            this.syncingEntryFields = false;
        }
    },

    copyEntryFields: function(source, target, fields){
        var values = {};
        Ext4.Array.forEach(fields, function(field){
            var value = source.get(field);
            if (Ext4.isEmpty(value))
                return;

            var current = target.get(field);
            var same = Ext4.isDate(value) && Ext4.isDate(current) ? value.getTime() === current.getTime() : value === current;
            if (!same)
                values[field] = value;
        });
        if (!Ext4.Object.isEmpty(values))
            target.set(values);
    },

    // RFC 9562: 48-bit Unix millisecond timestamp, version 7, variant 10, the rest random
    generateUUIDv7: function(){
        var bytes = new Uint8Array(16);
        window.crypto.getRandomValues(bytes);

        var ms = Date.now();
        for (var i = 5; i >= 0; i--){
            bytes[i] = ms % 256;
            ms = Math.floor(ms / 256);
        }
        bytes[6] = (bytes[6] & 0x0f) | 0x70;
        bytes[8] = (bytes[8] & 0x3f) | 0x80;

        var hex = Array.prototype.map.call(bytes, function(b){
            return (b < 16 ? '0' : '') + b.toString(16);
        }).join('');
        return [hex.substr(0, 8), hex.substr(8, 4), hex.substr(12, 4), hex.substr(16, 4), hex.substr(20)].join('-');
    }
});

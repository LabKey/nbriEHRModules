/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Fills the read-only current-value columns from the animal's open records, which saving the new row closes.
 * The demographics cache lacks groups, so these are read from study.Animal like the server does for saved rows.
 * Set silently: the grid redraws the row on the 'validation' event once any open cell editor closes.
 */
Ext4.define('NBRI_EHR.data.CurrentValueClientStore', {
    extend: 'EHR.data.DataEntryClientStore',

    currentValueFields: [
        'Id/activeAnimalGroups/groups',
        'Id/activeProtocolAssignments/protocolDisplayName',
        'Id/activeProjectAssignments/project'
    ],

    constructor: function(){
        this.callParent(arguments);

        this.activeCurrentValueFields = Ext4.Array.filter(this.currentValueFields, function(name){
            return !!this.getFields().get(name);
        }, this);
    },

    onUpdate: function(record, operation, modified){
        if (modified && Ext4.Array.contains(modified, 'Id'))
            this.retrieveCurrentValues([record]);

        this.callParent(arguments);
    },

    insert: function(index, records){
        var ret = this.callParent(arguments);

        this.retrieveCurrentValues(Ext4.Array.from(records));

        return ret;
    },

    retrieveCurrentValues: function(records){
        if (!this.activeCurrentValueFields.length)
            return;

        var ids = [];
        Ext4.Array.forEach(records, function(record){
            var id = record.get('Id');
            if (id)
                ids.push(id);
            else
                this.setCurrentValues(record, []);
        }, this);

        if (!ids.length)
            return;

        ids = Ext4.Array.unique(ids);
        LABKEY.Query.selectRows({
            schemaName: 'study',
            queryName: 'Animal',
            columns: ['Id'].concat(Ext4.Array.map(this.activeCurrentValueFields, function(name){
                return name.substring('Id/'.length);
            })),
            filterArray: [LABKEY.Filter.create('Id', ids.join(';'), LABKEY.Filter.Types.EQUALS_ONE_OF)],
            scope: this,
            success: function(results){
                var rowsById = {};
                Ext4.Array.forEach(results.rows || [], function(row){
                    var key = String(row.Id).toLowerCase();
                    rowsById[key] = rowsById[key] || [];
                    rowsById[key].push(row);
                });

                // the Id may have changed while the query ran, so match on each record's current Id
                this.each(function(record){
                    var id = record.get('Id');
                    if (id && Ext4.Array.contains(ids, id))
                        this.setCurrentValues(record, rowsById[String(id).toLowerCase()] || []);
                }, this);
            },
            failure: LDK.Utils.getErrorCallback()
        });
    },

    setCurrentValues: function(record, rows){
        record.beginEdit();
        Ext4.Array.forEach(this.activeCurrentValueFields, function(name){
            var values = [];
            Ext4.Array.forEach(rows, function(row){
                // group_concat comes back as an array
                var value = row[name.substring('Id/'.length)];
                Ext4.Array.forEach(Ext4.isArray(value) ? value : [value], function(v){
                    v = Ext4.isEmpty(v) ? null : Ext4.String.trim(String(v));
                    if (v && !Ext4.Array.contains(values, v))
                        values.push(v);
                });
            });

            record.set(name, values.length ? values.join(', ') : null);
        }, this);
        record.endEdit(true);

        this.fireEvent('validation', this, record);
    }
});

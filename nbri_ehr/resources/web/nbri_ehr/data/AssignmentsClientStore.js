/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * @param fieldConfigs
 */
Ext4.define('NBRI_EHR.data.AssignmentsClientStore', {
    extend: 'EHR.data.DataEntryClientStore',

    // Read-only columns echoing a value from the row's lookup target. Maintained here rather than from the
    // combo's select event because nothing guarantees a combo: bulk add builds its rows through createModel(),
    // and the parent store populates the project itself when an animal has a single active assignment.
    lookupEchoes: [
        {keyField: 'protocol', echoField: 'protocol/description', valueColumn: 'description'},
        {keyField: 'project', echoField: 'project/account', valueColumn: 'account'}
    ],

    constructor: function(){
        this.callParent(arguments);

        // One class serves both assignment sections, so keep only the pair this query actually carries
        this.activeEchoes = Ext4.Array.filter(this.lookupEchoes, function(echo){
            return !!this.getFields().get(echo.keyField) && !!this.getFields().get(echo.echoField);
        }, this);
    },

    onUpdate: function(record, operation, modified){
        this.callParent(arguments);

        this.syncLookupEchoes(record);
    },

    insert: function(index, records){
        // After the parent, which may set the project itself
        var ret = this.callParent(arguments);

        Ext4.Array.forEach(Ext4.Array.from(records), function(record){
            this.syncLookupEchoes(record);
        }, this);

        return ret;
    },

    syncLookupEchoes: function(record){
        // set() below re-enters this through onUpdate, and a field whose convert() rewrites the value would
        // never satisfy the equality check that normally stops it, so stop on the way in instead.
        if (this.syncingEchoes)
            return;

        this.syncingEchoes = true;
        try {
            Ext4.Array.forEach(this.activeEchoes, function(echo){
                this.syncLookupEcho(record, echo);
            }, this);
        }
        finally {
            this.syncingEchoes = false;
        }
    },

    syncLookupEcho: function(record, echo){
        var keyValue = record.get(echo.keyField);
        if (Ext4.isEmpty(keyValue)){
            this.setEchoValue(record, echo.echoField, null);
            return;
        }

        var field = this.getFields().get(echo.keyField);
        if (!field || !field.lookup || !field.lookup.keyColumn)
            return;

        var lookupStore = Ext4.StoreMgr.get(LABKEY.ext4.Util.getLookupStoreId(field));
        if (!lookupStore)
            return;

        // The lookup store autoLoads, so a row can arrive while it is still in flight. getCount() reads 0 both
        // then and for a store that loaded nothing, and neither can resolve anything, so wait for the load
        // rather than echoing the null an empty store would produce.
        if (lookupStore.isLoading() || !lookupStore.getCount()){
            lookupStore.on('load', function(){
                this.syncLookupEcho(record, echo);
            }, this, {single: true});

            return;
        }

        // findRecord(field, value, start, anyMatch, caseSensitive, exactMatch) defaults to a prefix match,
        // which would resolve one protocol id to another that merely starts with it.
        var match = lookupStore.findRecord(field.lookup.keyColumn, keyValue, 0, false, false, true);

        this.setEchoValue(record, echo.echoField, match ? match.get(echo.valueColumn) : null);
    },

    setEchoValue: function(record, fieldName, value){
        // A row loaded from the server already carries the right echo, so this keeps the sync from dirtying it.
        // Empty-aware because a string field with no useNull converts a set null straight back to ''.
        var current = record.get(fieldName);
        if (current === value || (Ext4.isEmpty(current) && Ext4.isEmpty(value)))
            return;

        record.suspendEvents();
        record.set(fieldName, value);
        record.resumeEvents();
    },

    getExtraContext: function(){
        var rows = [];
        var allRecords = this.getRange();
        for (var idx = 0; idx < allRecords.length; ++idx){
            var record = allRecords[idx];

            var date = record.get('date');
            var id = record.get('Id');
            var protocol = record.get('protocol');
            var project = record.get('project');
            if (!id || !date || !protocol || !project)
                continue;

            date = Ext4.Date.format(date, LABKEY.extDefaultDateFormat);

            if (protocol) {
                rows.push({
                    Id: id,
                    objectid: record.get('objectid'),
                    date: date,
                    enddate: record.get('enddate'),
                    qcstate: record.get('QCState'),
                    protocol: record.get('protocol')
                });
            }
            else if (project) {
                rows.push({
                    Id: id,
                    objectid: record.get('objectid'),
                    date: date,
                    enddate: record.get('enddate'),
                    qcstate: record.get('QCState'),
                    project: record.get('project')
                });
            }

        }

        if (!Ext4.isEmpty(rows)){
            rows = Ext4.encode(rows);

            return {
                assignmentsInTransaction: rows
            }
        }

        return null;
    }
});

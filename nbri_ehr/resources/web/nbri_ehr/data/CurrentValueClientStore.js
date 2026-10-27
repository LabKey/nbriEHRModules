/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Fills the read-only current-value columns from the demographics cache, which the Id editor has usually loaded by
 * the time the edit commits. Set before the parent's update so the row draws them with the Id; an async fallback
 * only appears once any open cell editor closes, as the grid defers 'validation' redraws until then.
 */
Ext4.define('NBRI_EHR.data.CurrentValueClientStore', {
    extend: 'EHR.data.DataEntryClientStore',

    // demographics property and its column for each current-value field
    currentValueSources: {
        'Id/activeAnimalGroups/groups': {property: 'activeAnimalGroups', column: 'groupId/title'},
        'Id/activeProtocolAssignments/protocolDisplayName': {property: 'protocolAssignments', column: 'protocol/displayName'},
        'Id/activeProjectAssignments/project': {property: 'activeAssignments', column: 'project/name'}
    },

    constructor: function(){
        this.callParent(arguments);

        this.activeCurrentValueFields = Ext4.Array.filter(Ext4.Object.getKeys(this.currentValueSources), function(name){
            return !!this.getFields().get(name);
        }, this);
    },

    onUpdate: function(record, operation, modified){
        if (modified && Ext4.Array.contains(modified, 'Id'))
            this.updateCurrentValues([record]);

        this.callParent(arguments);
    },

    insert: function(index, records){
        this.updateCurrentValues(Ext4.Array.from(records));

        return this.callParent(arguments);
    },

    updateCurrentValues: function(records){
        if (!this.activeCurrentValueFields.length)
            return;

        var pending = [];
        Ext4.Array.forEach(records, function(record){
            var id = record.get('Id');
            var cached = id ? EHR.DemographicsCache.getDemographicsSynchronously(id) : null;
            this.setCurrentValues(record, cached ? cached[id] : null);

            if (id && !(cached && cached[id]))
                pending.push(record);
        }, this);

        if (!pending.length)
            return;

        EHR.DemographicsCache.getDemographics(Ext4.Array.unique(Ext4.Array.map(pending, function(record){
            return record.get('Id');
        })), function(ids, idMap){
            if (!idMap)
                return;

            // the record may have been removed, or its Id changed, while this loaded
            Ext4.Array.forEach(pending, function(record){
                var demographics = idMap[record.get('Id')];
                if (demographics && this.indexOf(record) !== -1 && this.setCurrentValues(record, demographics))
                    this.fireEvent('validation', this, record);
            }, this);
        }, this, -1);
    },

    /** Returns whether any value changed. Skips equal values so syncing a loaded row doesn't dirty it. */
    setCurrentValues: function(record, demographics){
        var changed = false;
        record.beginEdit();
        Ext4.Array.forEach(this.activeCurrentValueFields, function(name){
            var value = demographics ? this.getCurrentValue(demographics, this.currentValueSources[name]) : null;
            var current = record.get(name);
            if (current === value || (Ext4.isEmpty(current) && Ext4.isEmpty(value)))
                return;

            record.set(name, value);
            changed = true;
        }, this);
        record.endEdit(true);

        return changed;
    },

    getCurrentValue: function(demographics, source){
        var values = [];
        Ext4.Array.forEach(demographics.getProperty(source.property) || [], function(row){
            var v = row[source.column];
            v = Ext4.isEmpty(v) ? null : Ext4.String.trim(String(v));
            if (v && !Ext4.Array.contains(values, v))
                values.push(v);
        });

        return values.length ? values.join(', ') : null;
    }
});

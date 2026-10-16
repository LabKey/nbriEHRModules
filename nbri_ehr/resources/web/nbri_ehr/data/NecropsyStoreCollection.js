/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Store collection for the necropsy forms. Every saved row takes the button's target QC state, so an earlier stage's
 * records are only sent when edited; otherwise a draft of a later stage would demote them to In Progress.
 */
Ext4.define('NBRI_EHR.data.NecropsyStoreCollection', {
    extend: 'EHR.data.TaskStoreCollection',

    getCommands: function(commitAll){
        var earlierStageQueries = (this.formConfig && this.formConfig.earlierStageQueries) || [];
        var allCommands = [];
        var allRecords = [];

        this.serverStores.each(function(s){
            var earlierStage = earlierStageQueries.indexOf((s.schemaName + '.' + s.queryName).toLowerCase()) > -1;
            var records = earlierStage ? Ext4.Array.filter(s.getRange(), this.isEdited, this) : s.getRange();
            var ret = s.getCommands(records, commitAll && !earlierStage);
            if (ret.commands.length){
                allCommands = allCommands.concat(ret.commands);
                allRecords = allRecords.concat(ret.records);
            }
        }, this);

        return {
            commands: allCommands,
            records: allRecords
        };
    },

    /** Saves an earlier stage's record even if none of its values changed. */
    forceSave: function(rec){
        rec.setDirty();
        rec.necropsyForceSave = true;
    },

    // a child insert re-copies inherited fields onto every child record, flagging them modified with unchanged values
    isEdited: function(rec){
        return rec.phantom || rec.necropsyForceSave || Ext4.Object.getKeys(rec.modified || {}).some(function(name){
            return !rec.isEqual(rec.modified[name], rec.get(name));
        });
    }
});

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
            var ret = s.getCommands(s.getRange(), commitAll && !earlierStage);
            if (ret.commands.length){
                allCommands = allCommands.concat(ret.commands);
                allRecords = allRecords.concat(ret.records);
            }
        }, this);

        return {
            commands: allCommands,
            records: allRecords
        };
    }
});

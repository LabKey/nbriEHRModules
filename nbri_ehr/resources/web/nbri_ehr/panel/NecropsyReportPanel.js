/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Details view for a necropsy task: one grid per form section, each shown through its 'Necropsy Report' view.
 */
Ext4.define('NBRI_EHR.panel.NecropsyReportPanel', {
    extend: 'EHR.panel.DataEntryFormDetailsPanel',

    onLoad: function(results){
        if (results.canRead === false){
            this.removeAll();
            this.add({
                html: 'You do not have permission to read this type of form.',
                border: false
            });
            return;
        }

        var queries = [];
        var queryMap = {};
        Ext4.Array.forEach(results.form.sections, function(section){
            Ext4.Array.forEach(section.queries || [], function(query){
                var key = query.schemaName + '.' + query.queryName;
                if (!queryMap[key] && query.schemaName !== 'ehr'){
                    query.label = section.label;
                    queries.push(query);
                    queryMap[key] = true;
                }
            });
        });

        LABKEY.NavTrail.setTrail('Necropsy Report', null, 'Necropsy Report', true, false);

        var filterArray = [LABKEY.Filter.create('taskid', this.taskId, LABKEY.Filter.Types.EQUAL)];
        Ext4.Array.forEach(queries, function(q){
            var id = LABKEY.Utils.id();
            Ext4.get('detailsParent').createChild({id: id, tag: 'div', style: 'margin-bottom: 20px;'});

            new LABKEY.QueryWebPart({
                renderTo: id,
                title: LABKEY.Utils.encodeHtml(q.label),
                frame: q.queryName === 'necropsyTask' ? 'portal' : 'dialog',
                schemaName: q.schemaName,
                queryName: q.queryName,
                viewName: 'Necropsy Report',
                filters: filterArray,
                success: this.onDataRegionLoad,
                scope: this
            });
        }, this);

        this.showEditBtn('Completed', results.form.permissions);
        this.removeAll();
    }
});

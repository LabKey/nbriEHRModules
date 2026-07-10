/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * @param fieldConfigs
 */
Ext4.define('NBRI_EHR.data.AssignmentsClientStore', {
    extend: 'EHR.data.DataEntryClientStore',

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

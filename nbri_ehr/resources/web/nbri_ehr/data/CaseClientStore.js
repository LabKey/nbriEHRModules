/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.define('NBRI_EHR.data.CaseClientStore', {
    extend: 'EHR.data.ParentClientStore',

    getExtraContext: function(){
        // Pass through cases for validation in clinremarks
        var caseMap = {};
        var allRecords = this.getRange();
        for (var idx = 0; idx < allRecords.length; ++idx){
            var record = allRecords[idx];
            if (record.get('objectid')){
                var id = record.get('Id');
                var date = record.get('date');
                if (!id || !date)
                    continue;

                date = Ext4.Date.format(date, LABKEY.extDefaultDateFormat);

                if (!caseMap[id])
                    caseMap[id] = [];

                caseMap[id].push({
                    objectid: record.get('objectid'),
                    date: date,
                    qcstate: record.get('QCState'),
                    caseid: record.get('caseid'),
                    enddate: record.get('enddate')
                });
            }
        }

        if (!LABKEY.Utils.isEmptyObj(caseMap)){
            caseMap = Ext4.encode(caseMap);

            return {
                casesInTransaction: caseMap
            }
        }

        return null;
    }
});
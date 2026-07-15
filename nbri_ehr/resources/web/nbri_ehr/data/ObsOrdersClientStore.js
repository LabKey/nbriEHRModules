/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.data.ObsOrdersClientStore', {
    extend: 'EHR.data.DataEntryClientStore',

    constructor: function(){
        this.callParent(arguments);

        this.observationTypesStore = EHR.DataEntryUtils.getObservationTypesStore();
    },

    getExtraContext: function(){
        // Pass through cases for validation in clinremarks
        var orders = [];
        var allRecords = this.getRange();
        for (var idx = 0; idx < allRecords.length; ++idx){
            var record = allRecords[idx];
            if (record.get('category')){
                var id = record.get('Id');
                var date = record.get('date');
                if (!id || !date)
                    continue;

                date = Ext4.Date.format(date, LABKEY.extDefaultDateFormat);

                orders.push({
                    id: id,
                    objectid: record.get('objectid'),
                    date: date,
                    qcstate: record.get('QCState'),
                    taskid: record.get('taskid'),
                    category: record.get('category'),
                    frequency: record.get('frequency')
                });
            }
        }

        return {
                ordersInTransaction: orders
        }
    }
});
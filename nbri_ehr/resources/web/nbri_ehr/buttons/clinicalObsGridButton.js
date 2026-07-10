/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.DataEntryUtils.registerGridButton('NBRI_AUTO_POPULATE_DAILY_OBS', function(config){
    return Ext4.Object.merge({
        text: 'Auto Populate Clinical Obs',
        xtype: 'button',
        hidden: true,
        listeners: {
            render: function(btn){
                const id = LABKEY.ActionURL.getParameter('id');
                const caseid = LABKEY.ActionURL.getParameter('caseid');
                const scheduledDate = LABKEY.ActionURL.getParameter('scheduledDate');
                const scheduled = id && caseid && scheduledDate;

                LABKEY.Query.selectRows({
                    schemaName: 'ehr',
                    queryName: 'observation_types',
                    ignoreFilter: true,
                    success: function (results) {
                        var grid = btn.up('gridpanel');
                        if (grid?.store?.data?.getCount() === 0) {
                            if (results?.rows?.length > 0) {
                                for (var i = 0; i < results.rows.length; i++) {
                                    var row = results.rows[i];
                                    if (row.value === 'Verified Id?' || row.value === 'Stool' || row.value === 'Activity' ||
                                            row.value === 'Appetite' || row.value === 'Hydration' || row.value === 'BCS') {

                                        var newRecord = grid.store.createModel({});
                                        newRecord.set({
                                            category: row.value,
                                        });

                                        if (scheduled) {
                                            newRecord.set('Id', id);
                                            newRecord.set('caseid', caseid);
                                            newRecord.set('scheduledDate', scheduledDate);
                                        }
                                        grid.store.add(newRecord);
                                    }
                                }

                                if (scheduled) {
                                    this.addEvents('animalchange');
                                    this.enableBubble('animalchange');
                                    this.fireEvent('animalchange', id);
                                    grid.fireEvent('panelDataChange');
                                }
                            }
                        }
                    },
                    scope: this
                });
            }
        }
    }, config);
});
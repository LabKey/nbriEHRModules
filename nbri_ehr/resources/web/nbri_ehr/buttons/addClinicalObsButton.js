/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

EHR.DataEntryUtils.registerGridButton('NBRI_DAILY_CLINICAL_OBS', function(config){
    return Ext4.Object.merge({
        text: 'Daily Observations',
        tooltip: 'Click to add daily observations',
        handler: function(btn) {
            LABKEY.Query.selectRows({
                schemaName: 'ehr',
                queryName: 'observation_types',
                ignoreFilter: true,
                success: function (results) {
                    var grid = btn.up('gridpanel');
                    if (results?.rows?.length > 0) {
                        for (var i = 0; i < results.rows.length; i++) {
                            var row = results.rows[i];
                            if (row.value === 'Verified Id?' || row.value === 'Stool' || row.value === 'Activity' ||
                                    row.value === 'Appetite' || row.value === 'Hydration' || row.value === 'BCS') {

                                var newRecord = grid.store.createModel({});
                                newRecord.set({
                                    category: row.value,
                                });
                                grid.store.add(newRecord);
                            }
                        }
                    }
                    else {
                        Ext4.Msg.alert('Unable to load observation types. Please contact the system administrator.');
                    }
                },
                failure: function (error) {
                    Ext4.Msg.alert('Unable to load observation types. Please contact the system administrator.');
                    console.log('Error loading observation types: ' + error);
                },
                scope: this
            });
        }
    }, config);
});

EHR.DataEntryUtils.registerGridButton('NBRI_DAILY_CLINICAL_OBS_ORDERS', function(config){
    return Ext4.Object.merge({
        text: 'Daily Observations',
        tooltip: 'Click to add daily observation orders',
        handler: function(btn) {

            LABKEY.Query.selectRows({
                schemaName: 'ehr',
                queryName: 'observation_types',
                ignoreFilter: true,
                success: function (results) {
                    var grid = btn.up('gridpanel');
                    if (results?.rows?.length > 0) {
                        for (var i = 0; i < results.rows.length; i++) {
                            var row = results.rows[i];
                            if (row.value === 'Verified Id?' || row.value === 'Stool' || row.value === 'Activity' ||
                                    row.value === 'Appetite' || row.value === 'Hydration' || row.value === 'BCS') {

                                var newRecord = grid.store.createModel({});
                                newRecord.set({
                                    category: row.value,
                                    frequency: 'SID'
                                });
                                grid.store.add(newRecord);
                            }
                        }
                    }
                    else {
                        Ext4.Msg.alert('Unable to load observation types. Please contact the system administrator.');
                    }
                },
                failure: function (error) {
                    Ext4.Msg.alert('Unable to load observation types. Please contact the system administrator.');
                    console.log('Error loading observation types: ' + error);
                },
                scope: this
            });
        }
    }, config);
});
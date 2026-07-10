/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.namespace('NBRI_EHR.RecordTreatmentButton');

Ext4.define('NBRI_EHR.window.RecordTreatmentWindow', {
    extend: 'Ext.window.Window',
    returnLocation: null,

    initComponent: function() {
        Ext4.apply(this, {
            title: 'Bulk Record Treatments',
            modal: true,
            width: 450,
            bodyStyle: 'padding: 5px;',
            closeAction: 'destroy',
            items: [{
                xtype: 'labkey-combo',
                fieldLabel: 'Performed By',
                width: 350,
                labelWidth: 100,
                value: LABKEY.Security.currentUser.id,
                itemId: 'performedBy',
                displayField: 'DisplayName',
                valueField: 'UserId',
                queryMode: 'local',
                forceSelection: true,
                matchFieldWidth: false,
                store: {
                    type: 'labkey-store',
                    schemaName: 'core',
                    queryName: 'PrincipalsWithoutAdmin',
                    columns: 'UserId,DisplayName,FirstName,LastName',
                    sort: 'Type,DisplayName',
                    autoLoad: true
                },
                anyMatch: true,
                caseSensitive: false,
            }, {
                xtype: 'xdatetime',
                itemId: 'dateField',
                width: 350,
                labelWidth: 100,
                fieldLabel: 'Date',
                allowBlank: false,
                name: 'date',
                value: new Date()
            },],
            buttons: [{
                text:'Submit',
                scope: this,
                handler: function (btn) {
                    this.recordTreatment(btn, this.dataRegion);
                }
            },{
                text: 'Cancel',
                scope: this,
                handler: function(btn){
                    btn.up('window').close();
                }
            }]
        });

        this.callParent(arguments);
    },

    recordTreatment: function(btn, dataRegion) {
        let win = btn.up('window');
        let windDate = win.down('#dateField').getValue();
        let performedBy = win.down('#performedBy').getValue();
        const selectedRows = [...new Set(dataRegion.getChecked())];
        const objectIds = selectedRows.map(row => row.split('-pkSeparator-')[0]);
        var me = this;
        btn.setDisabled(true);
        Ext4.Msg.wait('Recording treatments...');

        LABKEY.Query.selectRows({
            schemaName: 'study',
            queryName: 'treatment_order',
            filterArray: [LABKEY.Filter.create('objectid', objectIds.join(';'), LABKEY.Filter.Types.EQUALS_ONE_OF)],
            scope: this,
            ignoreFilter: true,
            columns: 'Id,objectid,code,route,amount,amount_units,concentration,volume,vol_units,conc_units,dosage,dosage_units,orderedby,category,caseid',
            success: function (data) {
                const rowsToInsert = [];
                Ext4.each(data.rows, function(row) {
                    let scheduledDate;
                    Ext4.each(selectedRows, function(selectedRow) {
                        const parts = selectedRow.split('-pkSeparator-');
                        const objectId = parts[0];
                        const date = parts[1];
                        if (row.objectid === objectId) {
                            scheduledDate = date;
                            rowsToInsert.push({
                                Id: row.Id,
                                treatmentid: row.objectid,
                                date: windDate,
                                performedby: performedBy,
                                objectid: LABKEY.Utils.generateUUID(),
                                scheduledDate: scheduledDate,
                                code: row.code,
                                route: row.route,
                                amount: row.amount,
                                amount_units: row.amount_units,
                                concentration: row.concentration,
                                conc_units: row.conc_units,
                                dosage: row.dosage,
                                dosage_units: row.dosage_units,
                                volume: row.volume,
                                vol_units: row.vol_units,
                                orderedby: row.orderedby,
                                category: row.category,
                                caseid: row.caseid
                            });
                        }
                    });
                });

                LABKEY.Query.insertRows({
                    schemaName: 'study',
                    queryName: 'drug',
                    rows: rowsToInsert,
                    scope: this,
                    success: function() {
                        Ext4.Msg.alert('Success', 'Treatments recorded successfully.', function(){
                            dataRegion.clearSelected();
                            window.location = me.returnLocation;
                            window.location.reload();
                        });
                        win.close();
                    },
                    failure: function(error) {
                        btn.setDisabled(false);
                        Ext4.Msg.alert('Error', error?.exception ?? 'An error occurred while recording treatments.');
                        console.error(error);
                    }
                });
            },
            failure: function(error) {
                btn.setDisabled(false);
                Ext4.Msg.alert('Error', error?.exception ?? 'An error occurred querying treatments.');
                console.error(error);
            }
        });
    }

});

NBRI_EHR.RecordTreatmentButton = new function () {
    return {
        recordTreatmentsHandler: function(dataRegion) {
            if (dataRegion && dataRegion.getChecked().length > 0) {
                Ext4.create('NBRI_EHR.window.RecordTreatmentWindow', {
                    dataRegion: dataRegion,
                    returnLocation: window.location.href
                }).show();
            }
            else {
                Ext4.Msg.alert('Error', 'Please select at least one treatment.');
            }
        },
    }
}

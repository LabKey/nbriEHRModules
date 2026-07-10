/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.namespace('NBRI_EHR.RecordProcedureButton');

Ext4.define('NBRI_EHR.window.RecordProcedureWindow', {
    extend: 'Ext.window.Window',
    returnLocation: null,

    initComponent: function() {
        Ext4.apply(this, {
            title: 'Record Procedures',
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
                    this.recordProcedure(btn, this.dataRegion);
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

    recordProcedure: function(btn, dataRegion) {
        let win = btn.up('window');
        let windDate = win.down('#dateField').getValue();
        let performedBy = win.down('#performedBy').getValue();
        const selectedRows = [...new Set(dataRegion.getChecked())];
        var me = this;
        btn.setDisabled(true);
        Ext4.Msg.wait('Recording procedures...');

        LABKEY.Query.selectRows({
            schemaName: 'study',
            queryName: 'prc_order',
            filterArray: [LABKEY.Filter.create('lsid', selectedRows.join(';'), LABKEY.Filter.Types.EQUALS_ONE_OF)],
            scope: this,
            ignoreFilter: true,
            columns: 'Id,objectid,procedure,category,caseid,orderedby,lsid',
            success: function (data) {
                const rowsToInsert = [];
                Ext4.each(data.rows, function(row) {
                    Ext4.each(selectedRows, function(selectedRow) {
                        if (row.lsid === selectedRow) {
                            rowsToInsert.push({
                                Id: row.Id,
                                procedure: row.procedure,
                                orderid: row.objectid,
                                date: windDate,
                                performedby: performedBy,
                                objectid: LABKEY.Utils.generateUUID(),
                                orderedby: row.orderedby,
                                category: row.category,
                                caseid: row.caseid
                            });
                        }
                    });
                });

                LABKEY.Query.insertRows({
                    schemaName: 'study',
                    queryName: 'prc',
                    rows: rowsToInsert,
                    scope: this,
                    success: function() {
                        Ext4.Msg.alert('Success', 'Procedures recorded successfully.', function(){
                            dataRegion.clearSelected();
                            window.location = me.returnLocation;
                            window.location.reload();
                        });
                        win.close();
                    },
                    failure: function(error) {
                        btn.setDisabled(false);
                        Ext4.Msg.alert('Error', error?.exception ?? 'An error occurred while recording procedures.');
                        console.error(error);
                    }
                });
            },
            failure: function(error) {
                btn.setDisabled(false);
                Ext4.Msg.alert('Error', error?.exception ?? 'An error occurred querying procedures.');
                console.error(error);
            }
        });
    }

});

NBRI_EHR.RecordProcedureButton = new function () {
    return {
        recordProceduresHandler: function(dataRegion) {
            if (dataRegion && dataRegion.getChecked().length > 0) {
                Ext4.create('NBRI_EHR.window.RecordProcedureWindow', {
                    dataRegion: dataRegion,
                    returnLocation: window.location.href
                }).show();
            }
            else {
                Ext4.Msg.alert('Error', 'Please select at least one procedure.');
            }
        },
    }
}

/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.namespace('NBRI_EHR.ProcedureOrderCompleteButton');

Ext4.define('NBRI_EHR.window.ProcedureOrderCompleteWindow', {
    extend: 'Ext.window.Window',
    returnLocation: null,

    initComponent: function() {
        Ext4.apply(this, {
            title: 'Mark Procedure Order Completed',
            modal: true,
            width: 450,
            bodyStyle: 'padding: 5px;',
            closeAction: 'destroy',
            items: [{
                xtype: 'panel',
                border: false,
                html: '<p style="padding: 10px; border: 0px;">This will mark the procedure order(s) completed without entering a procedure. Ensure the procedure has been entered separately if it should be recorded.</p>',
            }],
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
        const selectedRows = [...new Set(dataRegion.getChecked())];
        var me = this;
        btn.setDisabled(true);
        Ext4.Msg.wait('Completing procedure orders...');

        LABKEY.Query.selectRows({
            schemaName: 'core',
            queryName: 'QCState',
            filterArray: [LABKEY.Filter.create('label', 'Completed', LABKEY.Filter.Types.EQUALS)],
            scope: this,
            ignoreFilter: true,
            columns: 'RowId',
            success: function (data) {
                const rowsToInsert = [];
                const completedRowId = data?.rows?.[0]?.RowId;
                if (completedRowId) {
                    for (const row of selectedRows) {
                        rowsToInsert.push({
                            lsid: row,
                            qcstate: completedRowId
                        });
                    }
                }

                LABKEY.Query.updateRows({
                    schemaName: 'study',
                    queryName: 'prc_order',
                    rows: rowsToInsert,
                    scope: this,
                    success: function() {
                        Ext4.Msg.alert('Success', 'Procedure order(s) marked completed.', function(){
                            dataRegion.clearSelected();
                            window.location = me.returnLocation;
                            window.location.reload();
                        });
                        win.close();
                    },
                    failure: function(error) {
                        btn.setDisabled(false);
                        Ext4.Msg.alert('Error', error?.exception ?? 'An error occurred while recording procedure orders.');
                        console.error(error);
                    }
                });
            },
            failure: function(error) {
                btn.setDisabled(false);
                Ext4.Msg.alert('Error', error?.exception ?? 'An error occurred querying qcstates.');
                console.error(error);
            }
        });
    }

});

NBRI_EHR.ProcedureOrderCompleteButton = new function () {
    return {
        procedureOrderCompleteHandler: function(dataRegion) {
            if (dataRegion && dataRegion.getChecked().length > 0) {
                Ext4.create('NBRI_EHR.window.ProcedureOrderCompleteWindow', {
                    dataRegion: dataRegion,
                    returnLocation: window.location.href
                }).show();
            }
            else {
                Ext4.Msg.alert('Error', 'Please select at least one procedure order.');
            }
        },
    }
}

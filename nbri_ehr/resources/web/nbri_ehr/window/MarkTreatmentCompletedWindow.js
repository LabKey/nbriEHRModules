/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.define('NBRI_EHR.window.MarkTreatmentCompletedWindow', {
    extend: 'Ext.window.Window',
    pkColName: null,
    targetField: 'enddate',
    skipNonNull: true,

    statics: {
        buttonHandler: function(dataRegionName, schemaName, queryName, fieldXtype, pkColName, skipNonNull, targetField){
            var dataRegion = LABKEY.DataRegions[dataRegionName];

            var checked = dataRegion.getChecked();
            if (!checked || !checked.length){
                alert('No records selected');
                return;
            }

            Ext4.create('NBRI_EHR.window.MarkTreatmentCompletedWindow', {
                dataRegionName: dataRegionName,
                schemaName: schemaName,
                queryName: queryName,
                fieldXtype: fieldXtype,
                pkColName: pkColName,
                skipNonNull: skipNonNull !== false,
                targetField: targetField || 'enddate'
            }).show();
        }
    },

    initComponent: function(){
        Ext4.apply(this, {
            modal: true,
            title: 'Set End Date',
            closeAction: 'destroy',
            width: 380,
            items: [{
                bodyStyle: 'padding: 5px;',
                items: [{
                    xtype: 'labkey-combo',
                    // bodyStyle: 'padding: 5px;',
                    fieldLabel: 'Ordered By',
                    width: 350,
                    labelWidth: 100,
                    value: LABKEY.Security.currentUser.id,
                    itemId: 'orderedBy',
                    displayField: 'DisplayName',
                    valueField: 'UserId',
                    queryMode: 'local',
                    forceSelection: true,
                    matchFieldWidth: false,
                    store: {
                        type: 'labkey-store',
                        schemaName: 'ehr_lookups',
                        queryName: 'Veterinarians',
                        columns: 'UserId,DisplayName',
                        sort: 'DisplayName',
                        autoLoad: true
                    },
                    anyMatch: true,
                    caseSensitive: false,
                },{
                    xtype: (this.fieldXtype || 'xdatetime'),
                    fieldLabel: 'Date',
                    width: 350,
                    value: new Date(),
                    itemId: 'dateField'
                }]
            }],
            buttons: [{
                text:'Submit',
                disabled:false,
                scope: this,
                handler: this.onSubmit
            },{
                text: 'Close',
                handler: function(btn){
                    btn.up('window').close();
                }
            }]
        });

        this.callParent();

        LDK.Assert.assertNotEmpty('No pkColName provided to MarkTreatmentCompletedWindow', this.pkColName);
    },

    onSubmit: function(btn){
        Ext4.Msg.wait('Loading...');
        var date = btn.up('window').down('#dateField').getValue();
        var orderedBy = btn.up('window').down('#orderedBy').getValue();
        if (!date){
            Ext4.Msg.alert('Error', 'Must enter a date');
            return;
        }

        if (!orderedBy){
            Ext4.Msg.alert('Error', 'Must enter who ordered ending the treatment');
            return;
        }

        var dataRegion = LABKEY.DataRegions[this.dataRegionName];
        var checked = dataRegion.getChecked();

        LABKEY.Query.selectRows({
            method: 'POST',
            schemaName: this.schemaName,
            queryName: this.queryName,
            columns: this.pkColName + ',' + this.targetField,
            filterArray: [
                LABKEY.Filter.create(this.pkColName, checked.join(';'), LABKEY.Filter.Types.EQUALS_ONE_OF)
            ],
            scope: this,
            ignoreFilter: true,
            success: this.onSuccess,
            failure: LDK.Utils.getErrorCallback()
        });
    },

    onSuccess: function(data){
        var toUpdate = [];
        var skipped = [];
        var dataRegion = LABKEY.DataRegions[this.dataRegionName];
        var date = this.down('#dateField').getValue();
        var orderedBy = this.down('#orderedBy').getValue();

        if (!data.rows || !data.rows.length){
            Ext4.Msg.hide();
            dataRegion.selectNone();
            dataRegion.refresh();
            return;
        }

        Ext4.Array.forEach(data.rows, function(row){
            if (!row[this.targetField] || !this.skipNonNull){
                var obj = {};
                obj[this.targetField] = date;
                obj["endTreatmentOrderedBy"] = orderedBy;
                obj[this.pkColName] = row[this.pkColName];
                toUpdate.push(obj);
            }
            else {
                skipped.push(row[this.pkColName]);
            }
        }, this);

        Ext4.Msg.hide();
        if (skipped.length){
            Ext4.Msg.alert('', 'One or more rows will be skipped because it already has an end date', function(){
                this.doUpdate(toUpdate);
            }, this);
        }
        else {
            this.doUpdate(toUpdate);
        }
    },

    doUpdate: function(toUpdate){
        var dataRegion = LABKEY.DataRegions[this.dataRegionName];

        if (toUpdate.length){
            Ext4.Msg.wait('Saving...');
            LABKEY.Query.updateRows({
                method: 'POST',
                schemaName: this.schemaName,
                queryName: this.queryName,
                rows: toUpdate,
                scope: this,
                success: function(){
                    this.close();
                    Ext4.Msg.hide();
                    dataRegion.selectNone();
                    dataRegion.refresh();
                },
                failure: LDK.Utils.getErrorCallback()
            });
        }
        else {
            Ext4.Msg.hide();
            dataRegion.selectNone();
            dataRegion.refresh();
        }
    }
});
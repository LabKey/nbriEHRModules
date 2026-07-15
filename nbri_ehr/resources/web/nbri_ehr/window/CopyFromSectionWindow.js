/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * This is similar to the EHR CopyFromSectionWindow with the following exceptions. Project and performedBy columns are
 * removed. Date is a date field, not a datetime field.
 *
 * @cfg targetGrid
 * @cfg parentStore
 * @cfg sourceLabel
 */
Ext4.define('NBRI_EHR.window.CopyFromSectionWindow', {
    extend: 'EHR.window.CopyFromSectionWindow',

    getInitialItems: function(){
        var items = [{
            html: '<b>Animal</b>'
        },{
            html: '<b>Date</b>'
        },{
            html: '<b>Performed By</b>'
        },{
            html: '<b>Skip?</b>'
        }];

        var keys = {}, key;
        var keyFields = ['Id'];

        //console.log(keyFields);
        var orderedKeys = [];
        Ext4.Array.forEach(this.parentRecords, function(record){
            key = this.getKeyValue(record, keyFields);
            if (orderedKeys.indexOf(key) == -1){
                orderedKeys.push(key);
            }

            keys[key] = keys[key] || {
                Id: record.get('Id'),
                performedby: [],
                dates: [],
                total: 0
            };

            keys[key].total++;
            if (record.fields.get('performedby') && record.get('performedby'))
                keys[key].performedby.push(record.get('performedby'));
            keys[key].dates.push(record.get('date'))
        }, this);

        var existingIds = this.getExistingIds(keyFields);
        Ext4.Array.forEach(orderedKeys, function(key){
            var o = keys[key];

            items.push({
                xtype: 'displayfield',
                key: key,
                value: o.Id,
                fieldName: 'Id'
            });

            var dates = [];
            var minDate;
            Ext4.Array.forEach(o.dates, function(date){
                if (!minDate || date < minDate)
                    minDate = date;

                dates.push(Ext4.Date.format(date, LABKEY.extDefaultDateTimeFormat));
            }, this);

            o.performedby = Ext4.unique(o.performedby);
            var performedby = o.performedby.length == 1 ? o.performedby[0] : null;

            items.push({
                xtype: 'xdatetime',
                width: 300,
                format: LABKEY.extDefaultDateTimeFormat,
                timeFormat: 'H:i',
                fieldName: 'date',
                key: key,
                value: minDate
            });

            items.push({
                xtype: 'textfield',
                width: 200,
                fieldName: 'performedby',
                key: key,
                value: performedby
            });

            items.push({
                xtype: 'checkbox',
                key: key,
                fieldName: 'exclude',
                checked: existingIds[key]
            });
        }, this);

        return [{
            itemId: 'theTable',
            border: false,
            layout: {
                type: 'table',
                columns: 4
            },
            defaults: {
                border: false,
                style: 'margin: 5px;'
            },
            items: items
        }]
    }
});


EHR.DataEntryUtils.registerGridButton('NBRICOPYFROMSECTION', function(config){
    return Ext4.Object.merge({
        text: 'Copy From Section',
        xtype: 'button',
        tooltip: EHR.DataEntryUtils.shouldShowTooltips() ? 'Click to copy records from one of the other sections' : undefined,
        listeners: {
            beforerender: function(btn){
                var grid = btn.up('gridpanel');
                LDK.Assert.assertNotEmpty('Unable to find gridpanel in NBRICOPYFROMSECTION button', grid);

                btn.grid = grid;

                btn.appendButtons.call(btn);
            }
        },
        menu: {
            xtype: 'menu',
            items: [{
                text: 'Loading...'
            }]
        },
        appendButtons: function(){
            this.dataEntryPanel = this.grid.up('ehr-dataentrypanel');
            LDK.Assert.assertNotEmpty('Unable to find dataEntryPanel in NBRICOPYFROMSECTION button', this.dataEntryPanel);

            var toAdd = [];
            Ext4.Array.forEach(this.dataEntryPanel.formConfig.sections, function(section){
                if (section.name == this.grid.formConfig.name){
                    return;
                }

                var store = this.dataEntryPanel.storeCollection.getClientStoreByName(section.name);
                if (store){
                    //only allow copying from sections with an ID field
                    if (!store.getFields().get('Id')){
                        return;
                    }

                    toAdd.push({
                        text: section.label,
                        scope: this,
                        handler: function(menu){
                            Ext4.create('NBRI_EHR.window.CopyFromSectionWindow', {
                                targetGrid: this.grid,
                                sourceLabel: section.label,
                                parentStore: store
                            }).show();
                        }
                    });
                }
            }, this);

            this.menu.removeAll();
            if (toAdd.length){
                this.menu.add(toAdd);
            }
            else {
                this.menu.add({
                    text: 'There are no other sections'
                })
            }
        }
    });
});
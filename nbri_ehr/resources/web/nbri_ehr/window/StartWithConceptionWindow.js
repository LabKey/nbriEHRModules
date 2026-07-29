/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Adds a birth record pre-populated from an existing conception record.
 *
 * @cfg {Object} targetStore
 * @cfg {Object} formConfig
 */
Ext4.define('NBRI_EHR.window.StartWithConceptionWindow', {
    extend: 'Ext.window.Window',

    initComponent: function(){
        Ext4.apply(this, {
            title: 'Start with Conception',
            modal: true,
            closeAction: 'destroy',
            border: true,
            bodyStyle: 'padding: 5px',
            width: 400,
            defaults: {
                border: false,
                width: 370
            },
            items: [{
                html: 'Select a conception record.  A new birth record will be added using the conception Id, along with the dam, sire and species from that conception.',
                style: 'padding-bottom: 10px;'
            },{
                xtype: 'labkey-combo',
                itemId: 'conceptionField',
                fieldLabel: 'Conception Id',
                displayField: 'ConceptId',
                valueField: 'ConceptId',
                forceSelection: true,
                queryMode: 'local',
                anyMatch: true,
                caseSensitive: false,
                store: {
                    type: 'labkey-store',
                    schemaName: 'nbri_ehr',
                    queryName: 'Conception',
                    columns: 'ConceptId,ConceptDate,Dam,Sire',
                    sort: '-ConceptDate',
                    autoLoad: true
                }
            }],
            buttons: [{
                text: 'Submit',
                scope: this,
                handler: this.onSubmit
            },{
                text: 'Close',
                handler: function(btn){
                    btn.up('window').close();
                }
            }]
        });

        this.callParent(arguments);
    },

    onSubmit: function(btn){
        var field = this.down('#conceptionField');
        var conceptId = field.getValue();
        if (!conceptId){
            Ext4.Msg.alert('Error', 'Must select a conception Id');
            return;
        }

        var record = field.findRecordByValue(conceptId);
        if (!record){
            Ext4.Msg.alert('Error', 'Unable to find the conception record for: ' + conceptId);
            return;
        }

        var dam = record.get('Dam');
        var sire = record.get('Sire');

        btn.disable();
        this.getSpecies(dam, function(species){
            this.addRow(conceptId, dam, sire, species);
            btn.enable();
            this.close();
        }, this);
    },

    // the species of the offspring is inferred from the dam of the conception
    getSpecies: function(dam, callback, scope){
        if (!dam){
            callback.call(scope, null);
            return;
        }

        LABKEY.Query.selectRows({
            schemaName: 'study',
            queryName: 'demographics',
            columns: 'Id,species',
            filterArray: [LABKEY.Filter.create('Id', dam, LABKEY.Filter.Types.EQUAL)],
            scope: this,
            success: function(results){
                var species = results.rows && results.rows.length ? results.rows[0].species : null;
                callback.call(scope, species);
            },
            failure: function(error){
                console.error(error);
                callback.call(scope, null);
            }
        });
    },

    addRow: function(conceptId, dam, sire, species){
        this.targetStore.add(this.targetStore.createModel({
            conceptId: conceptId,
            'Id/demographics/dam': dam,
            'Id/demographics/sire': sire,
            'Id/demographics/species': species
        }));
    }
});

EHR.DataEntryUtils.registerGridButton('NBRI_START_WITH_CONCEPTION', function(config){
    return Ext4.Object.merge({
        text: 'Start with Conception',
        tooltip: EHR.DataEntryUtils.shouldShowTooltips() ? 'Click to add a birth record populated from an existing conception record' : undefined,
        handler: function(btn){
            var grid = btn.up('gridpanel');

            Ext4.create('NBRI_EHR.window.StartWithConceptionWindow', {
                targetStore: grid.store,
                formConfig: grid.formConfig
            }).show();
        }
    }, config);
});

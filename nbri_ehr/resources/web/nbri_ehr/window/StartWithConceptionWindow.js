/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Populates a birth record from an existing conception record.
 *
 * With no targetRecord a new birth row is added; with one, only that row's conception fields are replaced and the
 * rest of the row is left untouched.
 *
 * @cfg {Object} targetStore
 * @cfg {Object} formConfig
 * @cfg {Object} [targetRecord] the birth row to populate, or null to add a new one
 */
Ext4.define('NBRI_EHR.window.StartWithConceptionWindow', {
    extend: 'Ext.window.Window',

    initComponent: function(){
        var isExistingRow = !!this.targetRecord;

        Ext4.apply(this, {
            title: isExistingRow ? 'Change Conception' : 'Start with Conception',
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
                html: isExistingRow
                        ? 'Select a conception record.  The conception Id, dam, sire and species on this birth record will be replaced with the values from that conception.  Everything else on the row is left as it is.'
                        : 'Select a conception record.  A new birth record will be added using the conception Id, along with the dam, sire and species from that conception.',
                style: 'padding-bottom: 10px;'
            },{
                xtype: 'labkey-combo',
                itemId: 'conceptionField',
                fieldLabel: 'Conception Id',
                value: isExistingRow ? this.targetRecord.get('conceptId') : null,
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
        this.getSpecies(dam, function(species, speciesError){
            this.applyConception(conceptId, dam, sire, species);
            btn.enable();
            this.close();

            // the row is still populated so the conception values are not lost, but a blank species would otherwise
            // surface only as a bare "Species is required" error with no hint that the copy from the dam failed
            if (speciesError){
                Ext4.Msg.alert('Species Not Copied', speciesError + '  Enter the species on the birth record manually.');
            }
        }, this);
    },

    // the species of the offspring is inferred from the dam of the conception.  When it cannot be determined the
    // callback receives a message explaining why, rather than a null that is indistinguishable from an unset field.
    getSpecies: function(dam, callback, scope){
        if (!dam){
            callback.call(scope, null, 'The conception record has no dam, so the species could not be determined.');
            return;
        }

        LABKEY.Query.selectRows({
            schemaName: 'study',
            queryName: 'demographics',
            columns: 'Id,species',
            filterArray: [LABKEY.Filter.create('Id', dam, LABKEY.Filter.Types.EQUAL)],
            scope: this,
            success: function(results){
                var rows = (results && results.rows) || [];
                if (!rows.length){
                    callback.call(scope, null, 'No demographics record was found for dam ' + dam + '.');
                    return;
                }

                if (!rows[0].species){
                    callback.call(scope, null, 'No species is recorded on the demographics record for dam ' + dam + '.');
                    return;
                }

                callback.call(scope, rows[0].species);
            },
            failure: function(error){
                console.error(error);
                callback.call(scope, null, 'Unable to look up the species of dam ' + dam + ': ' + ((error && error.exception) || 'the query failed') + '.');
            }
        });
    },

    applyConception: function(conceptId, dam, sire, species){
        var values = {
            conceptId: conceptId,
            'Id/demographics/dam': dam,
            'Id/demographics/sire': sire,
            'Id/demographics/species': species
        };

        // only the fields the conception owns are written, so anything already entered on the row survives
        if (this.targetRecord){
            this.targetRecord.set(values);
            return;
        }

        this.targetStore.add(this.targetStore.createModel(values));
    }
});

EHR.DataEntryUtils.registerGridButton('NBRI_START_WITH_CONCEPTION', function(config){
    return Ext4.Object.merge({
        text: 'Start with Conception',
        tooltip: EHR.DataEntryUtils.shouldShowTooltips() ? 'Click to add a birth record populated from an existing conception record' : undefined,
        handler: function(btn){
            var grid = btn.up('gridpanel');
            if (!grid.store || !grid.store.hasLoaded()){
                console.log('no store or store hasnt loaded');
                return;
            }

            // commit any in-progress cell edit first; the modal window blocks the grid, so an open editor would
            // otherwise be abandoned and its pending value lost
            var cellEditing = grid.getPlugin(grid.editingPluginId);
            if (cellEditing){
                cellEditing.completeEdit();
            }

            Ext4.create('NBRI_EHR.window.StartWithConceptionWindow', {
                targetStore: grid.store,
                formConfig: grid.formConfig
            }).show();
        }
    }, config);
});

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * The final necropsy diagnoses grid. Until the final results are first saved it starts from a copy of the preliminary
 * diagnoses, which stay unchanged.
 */
Ext4.define('NBRI_EHR.grid.FinalDiagnosesGridPanel', {
    extend: 'EHR.grid.Panel',
    alias: 'widget.nbri_ehr-finaldiagnosesgridpanel',

    COPIED_FIELDS: ['tissueSample', 'diagnosis', 'severity', 'distribution', 'remark'],

    initComponent: function(){
        // grids are built after the form's initial load, so both stores already hold their saved rows
        var preliminary = this.store.storeCollection.getClientStoreByName('preliminaryDiagnoses');
        // a saved final results row means the final diagnoses were already seeded, so an empty grid was emptied on purpose
        var finalResults = this.store.storeCollection.getClientStoreByName('necropsyFinal');
        var finalSaved = finalResults && finalResults.findBy(function(rec){
            return !rec.phantom;
        }) > -1;
        if (preliminary && !finalSaved && this.store.getCount() === 0) {
            preliminary.each(function(rec){
                var data = {};
                Ext4.Array.forEach(this.COPIED_FIELDS, function(name){
                    data[name] = rec.get(name);
                });
                this.store.addClientModel(data);
            }, this);
        }

        this.callParent(arguments);
    }
});

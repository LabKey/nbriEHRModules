/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Fills the read-only current location from the housing 'location', since the shared getCurrentLocation() joins room
 * and cage and NBRI's cage already holds the full location id.
 */
Ext4.define('NBRI_EHR.data.HousingTransferClientStore', {
    extend: 'EHR.data.DataEntryClientStore',

    onUpdate: function(record, operation, modified){
        // the parent only fills an empty location, so a changed Id would keep the previous animal's
        if (modified && Ext4.Array.contains(modified, 'Id') && record.get('Id/curLocation/location')){
            record.suspendEvents();
            record.set('Id/curLocation/location', null);
            record.resumeEvents();
        }

        this.callParent(arguments);
    },

    ensureLocation: function(record){
        var id = record.get('Id');
        if (id && !record.get('Id/curLocation/location')){
            var cached = EHR.DemographicsCache.getDemographicsSynchronously(id);
            if (cached && cached[id]){
                record.suspendEvents();
                record.set('Id/curLocation/location', this.getActiveLocation(cached[id]));
                record.resumeEvents();

                return true;
            }

            return false;
        }

        return true;
    },

    retrieveLocation: function(ids){
        EHR.DemographicsCache.getDemographics(ids, function(idArr, idMap){
            if (idMap){
                for (var id in idMap){
                    var location = this.getActiveLocation(idMap[id]);
                    this.each(function(rec){
                        if (rec.get('Id') === id){
                            rec.beginEdit();
                            rec.set('Id/curLocation/location', location);
                            rec.endEdit(true);
                            this.fireEvent('validation', this, rec);
                        }
                    }, this);
                }
            }
        }, this, -1);
    },

    getActiveLocation: function(demographics){
        var housing = demographics.getActiveHousing();
        return housing && housing.length ? housing[0].location : null;
    }
});

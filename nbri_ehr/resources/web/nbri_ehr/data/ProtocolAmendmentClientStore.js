/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Parent store for the Protocol Amendment form. Every approved amendment states the protocol's complete per-species
 * counts, so when a protocol is chosen the child counts grid is seeded with the counts currently in effect for it and
 * the user adjusts from there rather than retyping every species.
 */
Ext4.define('NBRI_EHR.data.ProtocolAmendmentClientStore', {
    extend: 'EHR.data.ParentClientStore',

    COUNTS_SECTION: 'protocol_counts',

    afterEdit: function(record, modifiedFieldNames){
        this.callParent(arguments);

        if (Ext4.isArray(modifiedFieldNames) && modifiedFieldNames.indexOf('protocol') > -1){
            this.seedCountsForProtocol(record.get('protocol'));
        }
    },

    getCountsStore: function(){
        return this.storeCollection ? this.storeCollection.getClientStoreByName(this.COUNTS_SECTION) : null;
    },

    seedCountsForProtocol: function(protocol){
        var countsStore = this.getCountsStore();
        if (!countsStore){
            return;
        }

        // rows seeded for a previous protocol are meaningless once it changes; rows the user added by hand are theirs
        var stale = countsStore.queryBy(function(r){ return r.seededFromProtocol; }).getRange();
        if (stale.length){
            countsStore.remove(stale);
        }

        // a saved task reloading its own rows, or a user who has already started typing, is left alone
        if (!protocol || countsStore.getCount() > 0){
            return;
        }

        LABKEY.Query.selectRows({
            schemaName: 'ehr',
            queryName: 'protocolCountsEffective',
            columns: 'species,allowed',
            filterArray: [LABKEY.Filter.create('protocol', protocol, LABKEY.Filter.Types.EQUAL)],
            sort: 'species',
            scope: this,
            success: function(results){
                // the protocol may have changed again while the request was in flight
                var parent = this.getAt(0);
                if (!parent || parent.get('protocol') !== protocol || countsStore.getCount() > 0){
                    return;
                }

                Ext4.each(results.rows, function(row){
                    var added = countsStore.add({
                        species: row.species,
                        allowed: row.allowed
                    });
                    Ext4.each(added, function(r){ r.seededFromProtocol = protocol; });
                }, this);
            },
            failure: EHR.Utils.onError
        });
    }
});

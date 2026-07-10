/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.data.CaseStoreCollection', {
    extend: 'EHR.data.StoreCollection',

    constructor: function(){
        this.callParent(arguments);

        this.mon(EHR.DemographicsCache, 'caseselected', this.onCaseSelected, this);
        this.mon(EHR.DemographicsCache, 'casecreated', this.onCaseCreated, this);
    },

    onCaseCreated: function(){
        var record = this.getClientCaseRec();
        if (record){
            // iterate through record properties and set them to null
            Ext4.Object.each(record.data, function(key, value){
                if(key !== 'Id' && key !== 'category')
                    record.set(key, null);
            });

        }

        record = this.getServerCaseRec();
        if (record){
            // iterate through record properties and set them on the case record
            Ext4.Object.each(record.data, function(key, value){
                if(key !== 'Id' && key !== 'category')
                    record.set(key, null);
            });

        }
    },

    onCaseSelected: function(record){
        var rec = this.getClientCaseRec();
        if (rec && record){
            // iterate through record properties and set them on the case record
            Ext4.Object.each(record.data, function(key, value){
                if (rec.fields.keys.indexOf(key) !== -1)
                    rec.set(key, value);
            });

            // Required to not try to insert a new case. Indicates if a record is not saved in the db. This one is already saved.
            rec.phantom = false;
        }

        rec = this.getServerCaseRec();
        if (rec && record){
            // iterate through record properties and set them on the case record
            Ext4.Object.each(record.data, function(key, value){
                if (rec.fields.keys.indexOf(key) !== -1)
                    rec.set(key, value);
            });

            // Required to not try to insert a new case. Indicates if a record is not saved in the db. This one is already saved.
            rec.phantom = false;
        }

        this.validateAll();
    },

    getClientCaseStore: function(){
        if (this.clientCaseStore){
            return this.clientCaseStore;
        }

        this.clientCaseStore = this.getClientStoreByName('cases');
        LDK.Assert.assertNotEmpty('Unable to find cases store in CaseStoreCollection', this.clientCaseStore);

        return this.clientCaseStore;
    },

    getClientCaseRec: function(){
        var caseStore = this.getClientCaseStore();
        if (caseStore){
            LDK.Assert.assertTrue('More than 1 record found in Cases store, actual: ' + caseStore.getCount(), caseStore.getCount() <= 1);
            if (caseStore.getCount() == 1){
                return caseStore.getAt(0);
            }
        }
    },

    getServerCaseStore: function(){
        if (this.serverCaseStore){
            return this.serverCaseStore;
        }

        this.serverCaseStore = this.getServerStoreByName('study.cases');
        LDK.Assert.assertNotEmpty('Unable to find cases store in CaseStoreCollection', this.serverCaseStore);

        return this.serverCaseStore;
    },

    getServerCaseRec: function(){
        var caseStore = this.getServerCaseStore();
        if (caseStore){
            LDK.Assert.assertTrue('More than 1 record found in Cases store, actual: ' + caseStore.getCount(), caseStore.getCount() <= 1);
            if (caseStore.getCount() == 1){
                return caseStore.getAt(0);
            }
        }
    },

    getDefaultValues: function(){
        var rec = this.getClientCaseRec();
        if (rec){
            return {
                Id: rec.get('Id'),
                caseid: rec.get('objectid')
            }
        }

        return null;
    },

    onClientStoreUpdate: function(){
        this.doUpdateRecords();
        this.callParent(arguments);
    },

    doUpdateRecords: function(){
        var newValues = this.getDefaultValues() || {};
        var cacheKey = newValues ? (newValues.Id + '||' + newValues.caseid) : null;

        if (cacheKey !== this._cachedKey){
            var caseStore = this.getClientCaseStore();
            this.clientStores.each(function(cs){
                if (cs.storeId == caseStore.storeId){
                    return;
                }

                var toSet = {};

                if (cs.getFields().get('Id') != null){
                    toSet.Id = newValues.Id;
                }
                if (cs.getFields().get('caseid') != null){
                    toSet.caseid = newValues.caseid;
                }

                if (Ext4.Object.isEmpty(toSet)){
                    return;
                }

                var storeChanged = false;
                cs.suspendEvents();
                cs.each(function(rec){
                    var needsUpdate = false;
                    for (var field in toSet){
                        if (toSet[field] !== rec.get(field)){
                            needsUpdate = true;
                        }
                    }

                    if (needsUpdate){
                        storeChanged = true;
                        rec.set(toSet);
                    }
                }, this);
                cs.resumeEvents();

                if (storeChanged)
                    cs.fireEvent('datachanged', cs);
            }, this);
        }
        // Only validate once stores have loaded, otherwise can have perf issues when loading existing cases
        else if( this.haveStoresLoaded ) {
            // Validate across the sections, such as clin_remark not required if case close date entered
            this.validateAll();
        }

        this._cachedKey = cacheKey;
    },

    getTaskId: function(){
        if (this.taskId) {
            return this.taskId;
        }

        var model = this.getServerStoreForQuery('ehr', 'tasks').getAt(0);
        if (model){
            return model.get('taskid');
        }

        console.error('Unable to find taskid');
        return null
    },

    setClientModelDefaults: function(model){
        if (!model.get('taskid')){
            model.suspendEvents();
            model.set('taskid', this.getTaskId());
            model.resumeEvents();
        }

        return this.callParent([model]);
    },

    commitChanges: function(){
        // ensure all records are using this taskId and alert if not
        var taskid = this.getTaskId();
        if (taskid){
            this.clientStores.each(function(cs){
                if (cs.getFields().get('taskid') != null && cs.storeId !== this.collectionId + '-cases'){
                    cs.each(function(r){
                        if (taskid != r.get('taskid')){
                            LDK.Assert.assertEquality('Incorrect taskid for client store:' + cs.storeId, taskid, r.get('taskid'));
                            r.beginEdit();
                            r.set('taskid', this.getTaskId());
                            r.endEdit(true);
                        }
                    }, this);
                }
            }, this);

            this.serverStores.each(function(cs){
                if (cs.getFields().get('taskid') != null && cs.storeId.indexOf('|cases|') === -1){
                    cs.each(function(r){
                        if (r.isRemovedRequest){
                            return;  //do not check these records.  they have deliberately been separated.
                        }

                        if (taskid != r.get('taskid')){
                            LDK.Assert.assertEquality('Incorrect taskid for server store:' + cs.storeId, taskid, r.get('taskid'));
                            r.beginEdit();
                            r.set('taskid', this.getTaskId());
                            r.endEdit(true);
                        }
                    }, this);
                }
            }, this);
        }

        return this.callParent(arguments);
    }
});
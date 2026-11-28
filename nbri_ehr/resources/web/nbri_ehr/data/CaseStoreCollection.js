/*
 * Copyright (c) 2026 LabKey Corporation
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

    // Forms that can take over an existing case; the shared dataset trigger keeps the same list
    caseFormTypes: ['Clinical Cases', 'Behavioral Cases'],

    commitChanges: function(commitAll, extraContext, retainErrors){
        if (this.needsCaseStatusChoice(extraContext)){
            this.chooseCaseStatus(commitAll, extraContext, retainErrors);
            return;
        }

        // ensure all records are using this taskId and alert if not
        var taskid = this.getTaskId();
        if (taskid){
            // A case loaded from another task carries this one so the server can tell; the server keeps the case on its own task
            this.clientStores.each(function(cs){
                if (cs.getFields().get('taskid') != null){
                    var isCases = cs.storeId === this.collectionId + '-cases';
                    cs.each(function(r){
                        if (taskid != r.get('taskid')){
                            if (!isCases)
                                LDK.Assert.assertEquality('Incorrect taskid for client store:' + cs.storeId, taskid, r.get('taskid'));
                            r.beginEdit();
                            r.set('taskid', this.getTaskId());
                            r.endEdit(true);
                        }
                    }, this);
                }
            }, this);

            this.serverStores.each(function(cs){
                if (cs.getFields().get('taskid') != null){
                    var isCases = cs.storeId.indexOf('|cases|') !== -1;
                    cs.each(function(r){
                        if (r.isRemovedRequest){
                            return;  //do not check these records.  they have deliberately been separated.
                        }

                        if (taskid != r.get('taskid')){
                            if (!isCases)
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
    },

    needsCaseStatusChoice: function(extraContext){
        var caseRec = this.getClientCaseRec();
        return !!extraContext && extraContext.moveCase === undefined && !!extraContext.targetQC && extraContext.targetQC !== 'In Progress'
                && !!caseRec && !caseRec.phantom && !!this.formConfig && this.caseFormTypes.indexOf(this.formConfig.name) !== -1;
    },

    // Asks whether a submit moves an existing case to the new status; only a Completed case or one on no task can move to this task
    chooseCaseStatus: function(commitAll, extraContext, retainErrors){
        var caseRec = this.getClientCaseRec();
        var target = extraContext.targetQC;
        var commit = function(moveCase){
            this.commitChanges(commitAll, Ext4.apply({moveCase: moveCase}, extraContext), retainErrors);
        };

        LABKEY.Query.selectRows({
            schemaName: 'study',
            queryName: 'cases',
            columns: 'taskid,QCState/Label',
            filterArray: [LABKEY.Filter.create('objectid', caseRec.get('objectid'))],
            scope: this,
            success: function(results){
                var stored = results.rows[0];
                var status = stored ? stored['QCState/Label'] : null;
                var caseTaskId = stored ? stored.taskid : null;
                var thisTaskId = this.getTaskId();
                var otherTask = !caseTaskId || !thisTaskId || String(caseTaskId).toLowerCase() !== String(thisTaskId).toLowerCase();
                if (!status || status === target || !otherTask || (caseTaskId && status !== 'Completed')){
                    commit.call(this, false);
                    return;
                }

                var encode = Ext4.String.htmlEncode;
                Ext4.Msg.show({
                    title: 'Case Status',
                    msg: 'The case for ' + encode(caseRec.get('Id')) + ' is in ' + encode(status) + ' status. Would you like to move the whole case to ' + encode(target) + ', or only the other records in this form?',
                    buttons: Ext4.Msg.YESNOCANCEL,
                    buttonText: {yes: 'Move Whole Case', no: 'Only Other Records', cancel: 'Cancel'},
                    icon: Ext4.Msg.QUESTION,
                    scope: this,
                    fn: function(choice){
                        if (choice !== 'yes' && choice !== 'no')
                            return;

                        Ext4.Msg.wait('Saving Changes...');
                        commit.call(this, choice === 'yes');
                    }
                });
            },
            failure: function(error){
                Ext4.Msg.alert('Error', 'Unable to check the case status: ' + Ext4.String.htmlEncode(error && error.exception || 'unknown error'));
            }
        });
    }
});
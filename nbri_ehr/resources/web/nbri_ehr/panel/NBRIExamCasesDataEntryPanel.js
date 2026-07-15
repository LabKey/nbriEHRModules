/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.panel.ExamCasesDataEntryPanel', {
    extend: 'EHR.panel.TaskDataEntryPanel',

   onBeforeSubmit: function(btn){
       if (!btn || !btn.targetQC || ['Completed', 'Review Required'].indexOf(btn.targetQC) == -1){
           return;
       }

       var storeServer = this.storeCollection.getServerStoreByName('study.cases');
       var storeClient = this.storeCollection.getClientStoreByName('cases');

       var caseid;

       // Existing case
       if (storeClient && storeClient.data && storeClient.data.get(0) && storeClient.data.get(0).get('objectid')){
           caseid = storeClient.data.get(0).get('objectid');
       }
       else { // New case
           caseid = LABKEY.Utils.generateUUID();
           storeServer.each(function(r){
               r.set('objectid', caseid);
           }, this);

           storeClient.each(function(r){
               r.set('objectid', caseid);
           }, this);
       }

       this.storeCollection.clientStores.each(function(s){
              if (s.getFields().get('caseid')) {
                  s.each(function(r){
                      r.set('caseid', caseid);
                  }, this);
              }
       }, this);

       return true;
    }
    });

/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.window.ClinicalHistoryWindow', {
    extend: 'EHR.window.ClinicalHistoryWindow',

    statics: {
        showClinicalHistory: function(objectId, Id, date, el){
            var ctx = EHR.Utils.getEHRContext();
            LDK.Assert.assertNotEmpty('EHRContext not loaded.  This might indicate a ClientDependency issue', ctx);
            if (!ctx){
                return;
            }

            Ext4.create('NBRI_EHR.window.ClinicalHistoryWindow', {
                subjectId: Id,
                containerPath: ctx['EHRStudyContainer']
            }).show(el);
        }
    },

    getButtonCfg: function(){
        var ret = [{
            text: 'Close',
            handler: function(btn){
                btn.up('window').close();
            }
        },{
            text: 'Full Screen',
            scope: this,
            handler: function(btn){
                window.open(LABKEY.ActionURL.buildURL('ehr', 'animalHistory', this.containerPath) + '#subjects:' + this.subjectId + '&inputType:singleSubject&showReport:1&activeReport:clinicalHistory', '_blank');
                btn.up('window').close();
            }
        }];

        return ret;
    },

    getItems: function(){
        return [{
            xtype: EHR.reports.clinicalHistoryPanelXtype || 'ehr-smallformsnapshotpanel',
            showActionsButton: false,
            subjectId: this.subjectId,
            hideHeader: true,
            style: 'padding: 5px;',
            showExtendedInformation: true
        },{
            xtype: 'tabpanel',
            items: [{
                xtype: 'nbri_ehr-clinicalhistorypanel',
                title: 'History',
                border: true,
                width: 1230,
                gridHeight: 400,
                height: 400,
                autoLoadRecords: true,
                autoScroll: true,
                subjectId: this.subjectId,
                containerPath: this.containerPath,
                minDate: this.minDate || Ext4.Date.add(new Date(), Ext4.Date.YEAR, -2)
            },{
                xtype: 'ehr-weightgraphpanel',
                title: 'Weights',
                subjectId: this.subjectId,
                containerPath: this.containerPath,
                width: 1230,
                border: true
            }]
        }];
    }
});
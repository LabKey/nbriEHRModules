/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.window.CaseHistoryWindow', {
    extend: 'NBRI_EHR.window.ClinicalHistoryWindow',

    statics: {
        showCaseHistory: function(objectId, subjectId, el){
            var ctx = EHR.Utils.getEHRContext();
            LDK.Assert.assertNotEmpty('EHRContext not loaded.  This might indicate a ClientDependency issue', ctx);
            if (!ctx){
                return;
            }

            Ext4.create('NBRI_EHR.window.CaseHistoryWindow', {
                subjectId: subjectId,
                caseId: objectId,
                containerPath: ctx['EHRStudyContainer']
            }).show(el);
        }
    },

    initComponent: function(){
        Ext4.apply(this, {
            title: 'Case History: ' + this.subjectId
        });

        this.callParent(arguments);
    },

    getItems: function(){
        var items = this.callParent();
        items[1].items[0].title = 'Entire History';
        items[1].items.splice(1, 0, {
            title: 'Case History',
            xtype: 'nbri_ehr-casehistorypanel',
            containerPath: this.containerPath,
            border: true,
            width: 1180,
            gridHeight: 400,
            height: 400,
            autoScroll: true,
            autoLoadRecords: true,
            subjectId: this.subjectId,
            caseId: this.caseId
        });
        items[1].activeTab = 1;

        return items;
    }
});

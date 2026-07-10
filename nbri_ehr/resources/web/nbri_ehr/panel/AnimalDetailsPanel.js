/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/*
 * @param subjectId
 */
Ext4.define('NBRI_EHR.panel.AnimalDetailsPanel', {
    extend: 'NBRI_EHR.panel.SnapshotPanel',
    alias: 'widget.nbri_ehr-animaldetailspanel',

    border: true,
    showExtendedInformation: false,
    showActionsButton: false,
    doSuspendLayouts: false,
    showDisableButton: true,
    caseid: undefined,

    initComponent: function(){
        Ext4.apply(this, {
            border: true,
            bodyStyle: 'padding: 5px;',
            minHeight: 285,
            defaults: {
                border: false
            }
        });

        this.callParent(arguments);

        if (this.dataEntryPanel){
            this.mon(this.dataEntryPanel, 'animalchange', this.onAnimalChange, this, {buffer: 500});
            this.mon(EHR.DemographicsCache, 'caseselected', this.onCaseSelected, this, {buffer: 500});
        }

        this.mon(EHR.DemographicsCache, 'cachechange', this.demographicsListener, this);
    },

    demographicsListener: function(animalId){
        if (this.isDestroyed){
            console.log('is destroyed');
            return;
        }

        if (animalId == this.subjectId){
            this.loadAnimal(animalId, true);
        }
    },

    onCaseSelected: function(rec) {
        this.caseid = rec?.data?.objectid
    },

    onAnimalChange: function(animalId){
        //the intent of this is to avoid querying partial strings as the user types
        if (animalId && animalId.length < 2){
            animalId = null;
        }

        this.loadAnimal(animalId);
    },

    loadAnimal: function(animalId, forceReload){
        if (!forceReload && animalId == this.subjectId){
            return;
        }

        this.subjectId = animalId;

        if (animalId)
            EHR.DemographicsCache.getDemographics([this.subjectId], this.onLoad, this, (forceReload ? 0 : -1));
        else
            this.getForm().reset();
    },

    onLoad: function(ids, resultMap){
        if (ids && ids.length && ids[0] != this.subjectId){
            return;
        }

        this.callParent(arguments);
    },

    getItems: function(){
        return [{
            itemId: 'columnSection',
            layout: 'column',
            defaults: {
                border: false,
                bodyStyle: 'padding-right: 20px;'
            },
            items: [{
                xtype: 'container',
                width: 380,
                defaults: {
                    xtype: 'displayfield',
                    labelWidth: this.defaultLabelWidth
                },
                items: [{
                    fieldLabel: 'Id',
                    name: 'animalId'
                },{
                    fieldLabel: 'Location',
                    name: 'location'
                },{
                    fieldLabel: 'Sex',
                    name: 'gender'
                },{
                    fieldLabel: 'Species',
                    name: 'species'
                },{
                    fieldLabel: 'Age',
                    name: 'age'
                },{
                    xtype: 'displayfield',
                    fieldLabel: 'Source',
                    name: 'source'
                },{
                    name: 'protocolAssignment',
                    fieldLabel: 'Protocol'
                },{
                    name: 'projectAssignment',
                    fieldLabel: 'Project'
                }]
            },{
                xtype: 'container',
                width: 350,
                defaults: {
                    xtype: 'displayfield'
                },
                items: [{
                    fieldLabel: 'Status',
                    name: 'calculated_status'
                },{
                    fieldLabel: 'Flags',
                    name: 'flags'
                },{
                    fieldLabel: 'Cagemates',
                    name: 'cagemates'
                },{
                    fieldLabel: 'Weight',
                    name: 'weights'
                },{
                    xtype: 'ldk-linkbutton',
                    style: 'margin-top: 10px;',
                    scope: this,
                    text: '[Show Clinical/Case Hx]',
                    handler: function(){
                        if (this.subjectId && this.caseid){
                            NBRI_EHR.window.CaseHistoryWindow.showCaseHistory(this.caseid, this.subjectId, null);
                        }
                        else if (this.subjectId){
                            NBRI_EHR.window.ClinicalHistoryWindow.showClinicalHistory(null, this.subjectId, null);
                        }
                        else {
                            console.log('no id');
                        }
                    }
                },{
                    xtype: 'ldk-linkbutton',
                    style: 'margin-top: 5px;',
                    scope: this,
                    text: '[Show Recent Cases]',
                    handler: function(){
                        if (this.subjectId){
                            NBRI_EHR.window.RecentCasesWindow.showRecentCases(this.subjectId);
                        }
                        else {
                            console.log('no id');
                        }
                    }
                },{
                    xtype: 'ldk-linkbutton',
                    style: 'margin-top: 5px;',
                    scope: this,
                    text: '[Show Recent Vet/Behavior Remarks]',
                    handler: function(){
                        if (this.subjectId){
                            NBRI_EHR.window.RecentRemarksWindow.showRecentRemarks(this.subjectId);
                        }
                        else {
                            console.log('no id');
                        }
                    }
                }]
            }]
        },{
            name: 'treatments',
            xtype: 'ehr-snapshotchildpanel',
            headerLabel: 'Current Medications / Prescribed Diets',
            emptyText: 'There are no active medications',
            style: 'margin-top: 20px;'
        },{
            name: 'caseSummary',
            xtype: 'ehr-snapshotchildpanel',
            headerLabel: 'Active Cases',
            emptyText: 'There are no active cases'
        },{
            layout: 'hbox',
            style: 'padding-top: 10px;',
            items: [{
                xtype: 'button',
                border: true,
                text: 'Reload',
                scope: this,
                handler: function(btn){
                    this.loadAnimal(this.subjectId, true);
                }
            },{
                xtype: 'button',
                hidden: !this.showDisableButton,
                border: true,
                text: 'Disable',
                style: 'margin-left: 10px;',
                scope: this,
                handler: function(btn){
                    this.disableAnimalLoad = btn.getText() == 'Disable';

                    btn.setText(this.disableAnimalLoad ? 'Enable' : 'Disable');
                    this.down('#columnSection').setDisabled(this.disableAnimalLoad);
                }
            }]
        }];
    },

    appendWeightResults: function(toSet, results){
        var text;
        if (results && results.length){
            var row = results[0];
            var date = LDK.ConvertUtils.parseDate(row.date);
            var interval = '';
            if (date){
                //round to day for purpose of this comparison
                var d1 = Ext4.Date.clearTime(new Date(), true);
                var d2 = Ext4.Date.clearTime(date, true);
                interval = Ext4.Date.getElapsed(d1, d2);
                interval = interval / (1000 * 60 * 60 * 24);
                interval = Math.floor(interval);
                interval = interval + ' days ago';
            }

            text = row.weight + ' kg, ' + Ext4.Date.format(date, LABKEY.extDefaultDateFormat) + (!Ext4.isEmpty(interval) ? ' (' + interval + ')' : '');
        }

        toSet['weights'] = text;
    },
});
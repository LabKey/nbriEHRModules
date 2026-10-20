/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

Ext4.define('NBRI_EHR.window.AddAnimalsWindow', {
    extend: 'EHR.window.AddAnimalsWindow',
    upperCaseAnimalId: true,

    initComponent: function(){
        this.callParent(arguments);
    },

    locationHandler: function(){
        var form = this.resetFormFields(true);
        form.add([{
            html: 'This will return any animals currently housed in the selected location.  You can leave any of the fields blank.',
            style: 'padding-bottom: 10px;'
        },{
            xtype: 'nbri_ehr-locationField',
            itemId: 'cageField',
            fieldLabel: 'Cage(s)'
        }]);

        form.getAnimals = function(){

            var cage = this.down('#cageField').getValue();

            var filterArray = this.getBaseFilterArray().concat([LABKEY.Filter.create('isActive', true, LABKEY.Filter.Types.EQUAL)]);

            if (cage)
                filterArray.push(LABKEY.Filter.create('cage/rowid', cage, LABKEY.Filter.Types.EQUALS_ONE_OF));

            this.doQuery({
                schemaName: 'study',
                queryName: 'housing',
                sort: 'room,cage,Id',
                filterArray: filterArray
            });
        }
    },

    // NBRI groups are ehr_lookups.breeding_type codes rather than ehr.animal_groups rows.
    animalGroupHandler: function(){
        var form = this.resetFormFields(true);
        form.add([{
            html: 'This will return any animals currently assigned to the selected group.',
            style: 'padding-bottom: 10px;'
        },{
            xtype: 'combo',
            fieldLabel: 'Animal Group',
            itemId: 'groupField',
            displayField: 'title',
            valueField: 'value',
            queryMode: 'local',
            editable: false,
            store: Ext4.create('LABKEY.ext4.data.Store', {
                schemaName: 'ehr_lookups',
                queryName: 'breeding_type',
                columns: 'value,title',
                sort: 'title',
                autoLoad: true
            })
        }]);

        form.getAnimals = function(){
            var group = this.down('#groupField').getValue();
            if (!group){
                Ext4.Msg.alert('Error', 'Must choose a group');
                return;
            }

            var filterArray = this.getBaseFilterArray().concat([
                LABKEY.Filter.create('groupId', group, LABKEY.Filter.Types.EQUAL),
                LABKEY.Filter.create('isActive', true, LABKEY.Filter.Types.EQUAL)
            ]);

            this.doQuery({
                schemaName: 'study',
                queryName: 'animal_group_members',
                filterArray: filterArray
            });
        }
    },

    projectHandler: function(){
        var form = this.resetFormFields(true);
        form.add([{
            html: 'This will return any animals currently assigned to the selected project or protocol.  Choose one or the other.',
            style: 'padding-bottom: 10px;'
        },{
            xtype: 'ehr-projectfield',
            emptyText: '',
            itemId: 'projectField',
            width: 400,
            labelWidth: 140,
            onlyIncludeProjectsWithAssignments: true
        },{
            xtype: 'ehr-protocolfield',
            emptyText: '',
            itemId: 'protocolField',
            width: 400,
            labelWidth: 140,
            onlyIncludeProtocolsWithAssignments: true
        }]);

        form.getAnimals = function(){
            var projectId = this.down('#projectField').getValue();
            var protocol = this.down('#protocolField').getValue();
            if (!projectId && !protocol){
                Ext4.Msg.alert('Error', 'Must choose a project or protocol');
                return;
            }

            if (projectId && protocol){
                Ext4.Msg.alert('Error', 'Cannot pick both a project and protocol');
                return;
            }

            var filterArray = this.getBaseFilterArray().concat([LABKEY.Filter.create('isActive', true, LABKEY.Filter.Types.EQUAL)]);

            let queryName;
            if (projectId) {
                filterArray.push(LABKEY.Filter.create('project', projectId, LABKEY.Filter.Types.EQUAL));
                queryName = 'assignment';
            }
            else if (protocol) {
                filterArray.push(LABKEY.Filter.create('protocol', protocol, LABKEY.Filter.Types.EQUAL));
                queryName = 'protocolAssignment';
            }

            this.doQuery({
                schemaName: 'study',
                queryName: queryName,
                filterArray: filterArray
            });
        }
    },
});

EHR.DataEntryUtils.registerGridButton('NBRI_ADDANIMALS', function(config){
    return Ext4.Object.merge({
        text: 'Add Batch',
        tooltip: EHR.DataEntryUtils.shouldShowTooltips() ? 'Click to add a batch of animals, either as a list or by location' : undefined,
        handler: function(btn){
            var grid = btn.up('gridpanel');

            Ext4.create('NBRI_EHR.window.AddAnimalsWindow', {
                targetStore: grid.store,
                formConfig: grid.formConfig
            }).show();
        }
    }, config);
});
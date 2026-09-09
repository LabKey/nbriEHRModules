/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

EHR.model.DataModelManager.registerMetadata('Assignment', {
    byQuery: {
        'study.assignment': {
            // a new project assignment ends the open one automatically, so the end date is never entered here
            'enddate': {
                hidden: true
            },
            'project': {
                xtype: 'combo',
                nullable: false,
                lookup: {
                    schemaName: 'ehr',
                    queryName: 'project',
                    keyColumn: 'project',
                    columns: 'project,name,account',
                    filterArray: [
                        LABKEY.Filter.create('isActive', true, LABKEY.Filter.Types.EQUAL),
                    ]
                },
                editorConfig: {
                    listeners: {
                        select: function (combo, recs) {
                            if (!recs || recs.length !== 1)
                                return;

                            EHR.DataEntryUtils.setSiblingFields(combo, {'project/account': recs[0].get('account')});
                        },
                        change: function (combo, newVal) {
                            if (Ext4.isEmpty(newVal))
                                EHR.DataEntryUtils.setSiblingFields(combo, {'project/account': null});
                        }
                    }
                }
            },
            // read-only echo of the selected project's account; the select listener above keeps it current
            'project/account': {
                header: 'Project Account',
                label: 'Project Account',
                editable: false,
                columnConfig: {
                    editable: false,
                    width: 200
                }
            }
        },
        'study.protocolAssignment': {
            'enddate': {
                hidden: true
            },
            'project': {
              hidden: true
            },
            'protocol': {
                xtype: 'combo',
                nullable: false,
                columnConfig: {
                    fixed: true,
                    width: 250
                },
                lookup: {
                    schemaName: 'ehr',
                    queryName: 'activeProtocols',
                    keyColumn: 'protocol',
                    displayColumn: 'displayText',
                    columns: 'protocol,title,description,displayText'
                },
                editorConfig: {
                    listeners: {
                        select: function (combo, recs) {
                            if (!recs || recs.length !== 1)
                                return;

                            EHR.DataEntryUtils.setSiblingFields(combo, {'protocol/description': recs[0].get('description')});
                        },
                        change: function (combo, newVal) {
                            if (Ext4.isEmpty(newVal))
                                EHR.DataEntryUtils.setSiblingFields(combo, {'protocol/description': null});
                        }
                    }
                }
            },
            // read-only echo of the selected protocol's description; the select listener above keeps it current
            'protocol/description': {
                header: 'Protocol Description',
                label: 'Protocol Description',
                editable: false,
                columnConfig: {
                    editable: false,
                    width: 300
                }
            }
        }
    }
});
/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 *
 * @cfg pairedWithRoomField.  Note: if true, you must implement getRoomField(), which returns the cognate ehr-roomfield
 */
Ext4.define('NBRI_EHR.field.LocationField', {
    extend: 'Ext.ux.CheckCombo',
    alias: 'widget.nbri_ehr-locationField',

    fieldLabel: 'Location',
    nullCaption: '[Blank]',
    editable: true,
    expandToFitContent: true,
    addAllSelector: true,
    typeAhead: true,

    initComponent: function(){
        Ext4.apply(this, {
            displayField:'cage',
            valueField: 'rowid',
            queryMode: 'local',
            anyMatch: true,
            store: Ext4.create('LABKEY.ext4.data.Store', {
                type: 'labkey-store',
                schemaName: 'ehr_lookups',
                queryName: 'cage',
                sort: 'cage',
                autoLoad: true,
                columns: 'rowid,cage',
            })
        });

        if (!Ext4.isDefined(this.initialConfig.multiSelect)){
            this.multiSelect = true;
        }

        this.callParent(arguments);
    },

});
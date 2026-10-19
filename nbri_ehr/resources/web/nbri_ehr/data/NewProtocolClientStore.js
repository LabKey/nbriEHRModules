/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

/**
 * Protocol store for the New Protocol form. Defaults the end date from the approval date and mirrors the protocol into
 * the hidden amendment 0, which the counts grid hangs off.
 */
Ext4.define('NBRI_EHR.data.NewProtocolClientStore', {
    extend: 'EHR.data.DataEntryClientStore',

    AMENDMENT_SECTION: 'protocolAmendment',
    APPROVAL_YEARS: 3,

    // the table key is the protocol the user types, so every edit to it would orphan the pending server row and save
    // the protocol twice; objectid is generated client-side and never changes. See NewProtocolDataEntryPanel.
    keyFieldName: 'objectid',

    afterEdit: function(record, modifiedFieldNames){
        this.callParent(arguments);

        if (!Ext4.isArray(modifiedFieldNames)){
            return;
        }

        if (modifiedFieldNames.indexOf(this.getFieldName('approve')) > -1){
            this.applyApprovalDefaults(record);
        }

        this.syncInitialAmendment(record);
    },

    // ehr.xml and the NBRI metadata disagree on the case of some protocol columns, and record.get() is case-sensitive
    getFieldName: function(name){
        var field = this.getFields().findBy(function(f){
            return f.name.toLowerCase() === name.toLowerCase();
        });
        return field ? field.name : name;
    },

    applyApprovalDefaults: function(record){
        var approve = record.get(this.getFieldName('approve'));
        var enddate = record.get(this.getFieldName('enddate'));
        var updates = {};
        updates[this.getFieldName('first_approval')] = approve;

        // the end date follows the approval date until someone enters their own
        var lastDefault = record.defaultEnddate;
        if (approve && (!enddate || (lastDefault && enddate.getTime() === lastDefault.getTime()))){
            record.defaultEnddate = Ext4.Date.add(approve, Ext4.Date.YEAR, this.APPROVAL_YEARS);
            updates[this.getFieldName('enddate')] = record.defaultEnddate;
        }

        record.set(updates);
    },

    syncInitialAmendment: function(record){
        var amendmentStore = this.storeCollection.getClientStoreByName(this.AMENDMENT_SECTION);
        var amendment = amendmentStore ? amendmentStore.getAt(0) : null;
        if (!amendment){
            return;
        }

        amendment.set({
            protocol: record.get(this.getFieldName('protocol')),
            amendmentNumber: 0,
            date: record.get(this.getFieldName('approve')),
            enddate: record.get(this.getFieldName('enddate'))
        });
    }
});

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * @param subjects
 */
Ext4.define('NBRI.panel.BloodSummaryPanel', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.nbri-bloodsummarypanel',
    intervals: {},
    plotHeight: 400,

    bloodPerKgCol: 'species/scientific_name/blood_per_kg',
    bloodMaxDrawPctCol: 'species/scientific_name/max_draw_pct',
    bloodDrawIntervalCol: 'species/scientific_name/blood_draw_interval',

    initComponent: function(){
        Ext4.apply(this, {
            border: false,
            defaults: {
                border: false
            }
        });

        this.callParent();

        var target = this.add({
            xtype: 'ehr-bloodsummarypanel',
            subjects: this.subjects,
            getSubjectItems: this.summaryItems,
            bloodPerKgCol: this.bloodPerKgCol,
            bloodMaxDrawPctCol: this.bloodMaxDrawPctCol,
            bloodDrawIntervalCol: this.bloodDrawIntervalCol
        });


        if (!Ext4.isFunction(target.getWith)) {
            target.getWidth = function () {
                return 1000;
            }
        }
    },

    summaryItems: function(subject, demographics) {
        var toAdd = [];
        toAdd.push({
            html: '',
            border: false,
            style: 'margin-bottom: 10px;'
        });

        toAdd.push({
            xtype: 'ldk-querycmp',
            style: 'margin-bottom: 10px;',
            queryConfig: LDK.Utils.getReadOnlyQWPConfig({
                title: 'Recent Blood Draws: ' + subject,
                schemaName: 'study',
                queryName: 'bloodDrawsByDay',
                allowHeaderLock: false,
                frame: 'title',
                filters: [
                    LABKEY.Filter.create('Id', subject, LABKEY.Filter.Types.EQUAL),
                    LABKEY.Filter.create('date', '-' + (demographics.getValue(this.bloodDrawIntervalCol) * 2) + 'd', LABKEY.Filter.Types.DATE_GREATER_THAN_OR_EQUAL)
                ],
                sort: '-date'
            })
        });

        toAdd.push({
            html: '',
            border: false,
            style: 'margin-bottom: 10px;'
        });

        return toAdd;
    }
});
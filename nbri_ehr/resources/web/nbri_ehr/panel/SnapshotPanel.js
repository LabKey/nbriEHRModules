/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.define('NBRI_EHR.panel.SnapshotPanel', {
    extend: 'EHR.panel.SnapshotPanel',
    alias: 'widget.nbri_ehr-snapshotpanel',

    initComponent: function() {
        Ext4.apply(this, {
            defaults: {
                border: false,
            },
            showExtendedInformation: true,
            items: this.getItems(),
        });

        this.callParent();


        this.on('afterrender', function() {

            var displayField = this.down('#flags');
            if (displayField && displayField.getEl()) {

                var anchor = displayField.getEl('nbriFlagsLink');

                if (anchor) {
                    Ext4.get(anchor).on('click', function(e) {
                        e.preventDefault();
                        NBRI_EHR.Utils.showFlagPopup(this.subjectId, this);
                    });
                }
            }
        });
    },

    onLoad: function(ids, resultMap){
        if (ids && ids.length && ids[0] != this.subjectId){
            return;
        }

        this.callParent(arguments);
    },

    getBaseItems: function(){
        return [{
            xtype: 'container',
            border: false,
            defaults: {
                border: false
            },
            items: [{
                xtype: 'container',
                html: '<b>Summary:</b><hr>'
            },{
                bodyStyle: 'padding-bottom: 20px;',
                layout: 'column',
                defaults: {
                    border: false
                },
                items: [{
                    xtype: 'container',
                    columnWidth: 0.25,
                    defaults: {
                        labelWidth: this.defaultLabelWidth,
                        style: 'margin-right: 20px;'
                    },
                    items: [{
                        xtype: 'displayfield',
                        fieldLabel: 'Location',
                        name: 'location'
                    },{
                        xtype: 'displayfield',
                        hidden: this.redacted,
                        name: 'protocolAssignment',
                        fieldLabel: 'Protocol'
                    },{
                        xtype: 'displayfield',
                        hidden: this.redacted,
                        name: 'projectAssignment',
                        fieldLabel: 'Project'
                    },{
                        xtype: 'displayfield',
                        fieldLabel: 'Source',
                        name: 'source'
                    },{
                        xtype: 'displayfield',
                        fieldLabel: 'Prev Id',
                        name: 'prev_id'
                    }]
                },{
                    xtype: 'container',
                    columnWidth: 0.25,
                    defaults: {
                        labelWidth: this.defaultLabelWidth,
                        style: 'margin-right: 20px;'
                    },
                    items: [{
                        xtype: 'displayfield',
                        fieldLabel: 'Status',
                        name: 'calculated_status'
                    },{
                        xtype: 'displayfield',
                        fieldLabel: 'Sex',
                        name: 'gender'
                    },{
                        xtype: 'displayfield',
                        fieldLabel: 'Species',
                        name: 'species'
                    },{
                        xtype: 'displayfield',
                        fieldLabel: 'Age',
                        name: 'age'
                    }]
                },{
                    xtype: 'container',
                    columnWidth: 0.35,
                    defaults: {
                        labelWidth: this.defaultLabelWidth,
                        style: 'margin-right: 20px;'
                    },
                    items: [{
                        xtype: 'displayfield',
                        fieldLabel: 'Flags',
                        name: 'flags'
                    },{
                        xtype: 'displayfield',
                        fieldLabel: 'Remark',
                        name: 'remark'
                    },{
                        xtype: 'displayfield',
                        fieldLabel: 'Last TB',
                        name: 'lastTB'
                    },{
                        xtype: 'displayfield',
                        fieldLabel: 'Weights',
                        name: 'weights'
                    }]
                }]
            }]
        }];
    },

    appendDemographicsResults: function(toSet, row, id){
        if (!row){
            console.log('Id not found');
            return;
        }

        var animalId = row.getId() || id;
        if (!Ext4.isEmpty(animalId)){
            toSet['animalId'] = LABKEY.Utils.encodeHtml(id);
        }

        var data = row.getData();
        var status = data ? data.necropsy_status : undefined;
        var statusVal = 'Unknown';
        if (status) {
            if (typeof status == 'string') {
                statusVal = status;
            } else {
                statusVal = status[0] && (Object.keys(status[0]).length > 0) ? status[0].necropsy_status : statusVal;
            }
        }
        toSet['calculated_status'] = '<span ' + (statusVal !== 'Alive' ? 'style="background-color:yellow"' : '') + '>'
                + LABKEY.Utils.encodeHtml(statusVal) + '</span>';

        toSet['species'] = LABKEY.Utils.encodeHtml(row.getSpeciesCommonName());
        toSet['geographic_origin'] = LABKEY.Utils.encodeHtml(row.getGeographicOrigin());
        toSet['gender'] = LABKEY.Utils.encodeHtml(row.getGender());
        toSet['age'] = LABKEY.Utils.encodeHtml(row.getAgeInYearsAndDays());
        toSet['source'] = LABKEY.Utils.encodeHtml(row.getSourceRecord());

        var location;
        if (row.getActiveHousing() && row.getActiveHousing().length){
            var housingRow = row.getActiveHousing();
            location = '';
            if (housingRow)
                location +=  LABKEY.Utils.encodeHtml(housingRow[0]?.['cage/cage']);

            if (location){
                if (this.showLocationDuration && housingRow[0].date){
                    var date = LDK.ConvertUtils.parseDate(housingRow[0].date);
                    if (date)
                        location += ' (' + Ext4.Date.format(date, LABKEY.extDefaultDateFormat) + ')';
                }
            }
        }

        toSet['location'] = location || 'No active housing';
        this.appendProtocolAssignment(toSet, row);
    },

    appendFlags: function(toSet, results){
        var values = [];
        if (results){
            Ext4.each(results, function(row){
                var category = row['flag/category'];
                var highlight = row['flag/category/doHighlight'];
                var omit = row['flag/category/omitFromOverview'];

                //skip
                if (omit === true)
                    return;

                if (category)
                    category = Ext4.String.trim(category);

                var val = LABKEY.Utils.encodeHtml(this.getFlagDisplayValue(row));
                var text = val;
                if (category)
                    text = LABKEY.Utils.encodeHtml(category) + ': ' + val;

                if (text && highlight)
                    text = '<span style="background-color:yellow">' + text + '</span>';

                if (text)
                    values.push(text);
            }, this);

            if (values.length) {
                values = Ext4.unique(values);
            }
        }

        toSet['flags'] = values.length ? '<a id="nbriFlagsLink">' + values.join('<br>') + '</div>' : null;
    },

    getFlagDisplayValue: function(row) {
        return row['flag/description'] || row['flag/value'];
    },

    appendAssignments: function(toSet, results){
        toSet['projectAssignment'] = null;

        if (this.redacted) {
            return;
        }

        var values = [];
        if (results){
            Ext4.each(results, function(row){
                if (row['project/name']) {
                    values.push(LABKEY.Utils.encodeHtml(row['project/name']));
                }
            }, this);
        }

        toSet['projectAssignment'] = values.length ? values.join('<br>') : 'None';
    },

    appendProtocolAssignment: function(toSet, results){
        let paRecords = results.getData()['protocolAssignments'];
        let values = [];

        if (Ext4.isArray(paRecords) && paRecords.length > 0) {
            Ext4.each(paRecords, function(record) {
                let val = LABKEY.Utils.encodeHtml(record['protocol']);
                if (record['protocol/InvestigatorId/lastName']) {
                    val += ' - ' + LABKEY.Utils.encodeHtml(record['protocol/InvestigatorId/lastName']);
                }
                values.push(val);
            });
        }

        toSet['protocolAssignment'] = values.length > 0 ? values.join('<br/>') : 'None';
    },

    appendRoommateResults: function(toSet, results, id){
        var cagemates = 0;
        var animals = [];
        if (results && results.length){
            var row = results[0];
            if (row.animals){
                animals = row.animals.replace(/( )*,( )*/g, ',');
                animals = animals.split(',');
                animals.sort();
                var index = animals.indexOf(id);
                if (index !== -1) {
                    animals.splice(index, 1);
                }
            }
        }

        toSet['cagemates'] = LABKEY.Utils.encodeHtml(cagemates);  // encoding not currently useful, but future-proofing here

        if (animals.length > 3){
            toSet['cagemates'] = animals.length + ' animals';
        }
        else if (animals.length == 0){
            toSet['cagemates'] = 'None';
        }
        else {
            var html = '';
            var sep = '';
            Ext4.each(animals, function(id) {
                html += sep + '<a href="' + LABKEY.ActionURL.buildURL('ehr', 'participantView', null, {participantId: encodeURIComponent(id)}) + '" target="_blank">' + encodeURIComponent(id) + '</a>';
                sep = ', ';
            });
            toSet['cagemates'] = html;
        }
    },

    appendCaseSummary: function(toSet, results){
        var el = this.down('panel[name=caseSummary]');
        if (!el){
            return;
        }

        results = results || [];
        var filteredResults = [];
        Ext4.Array.forEach(results, function(row){
            var enddate = row.enddate ? LDK.ConvertUtils.parseDate(row.enddate) : null;
            if (!enddate || enddate.getTime() > (new Date()).getTime()){
                filteredResults.push(row);
            }
        }, this);

        el.appendTable({
            rows: filteredResults
        }, [{
            name: 'category',
            label: 'Category'
        },{
            name: 'problemCategory',
            label: 'Problem Area',
            attrs: {
                style: 'width: 150px !important;"'
            }
        },{
            name: 'problemSubcategory',
            label: 'Problem Subcategory',
            attrs: {
                style: 'width: 150px !important;"'
            }
        },{
            name: 'date',
            label: 'Open Date',
            attrs: {
                style: 'width: 120px !important;"'
            },
            dateFormat: LABKEY.extDefaultDateFormat
        },{
            name: 'openRemark',
            label: 'Open Remark',
            maxWidth: 500,
            attrs: {
                style: 'white-space: normal !important;"'
            }
        }]);
    },

    appendParentageResults: function(toSet, results){
        if (results){
            var parentMap = {};
            Ext4.each(results, function(row){
                var parent = row.parent;
                var relationship = row.relationship;

                if (parent && relationship){
                    var text = LABKEY.Utils.encodeHtml(relationship + ' - ' + parent);

                    if (!parentMap[text])
                        parentMap[text] = [];

                    var method = row.method;
                    if (method){
                        parentMap[text].push(LABKEY.Utils.encodeHtml(method));
                    }
                }
            }, this);

            var values = [];
            Ext4.Array.forEach(Ext4.Object.getKeys(parentMap).sort(), function(text){
                parentMap[text] = Ext4.unique(parentMap[text]);
                var subject = text;
                var textParts = text.split(' - ');
                if (textParts.length > 1){
                    subject = textParts[1];
                }

                values.push('<a href="' + LABKEY.ActionURL.buildURL('ehr', 'participantView', null, {participantId: encodeURIComponent(subject)}) + '" target="_blank">' + encodeURIComponent(subject) + '</a>');
            }, this);

            if (values.length)
                toSet['parents'] = values.join('<br>');
        }
        else {
            toSet['parents'] = 'No data';
        }
    },
});
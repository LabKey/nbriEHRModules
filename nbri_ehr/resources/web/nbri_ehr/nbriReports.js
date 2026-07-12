/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
Ext4.namespace('EHR.reports');

EHR.reports.clinicalHistoryPanelXtype = 'nbri_ehr-snapshotpanel';


EHR.reports.snapshot = function(panel, tab, showActionsBtn){
    if (tab.filters.subjects){
        renderSubjects(tab.filters.subjects, tab);
    }
    else
    {
        panel.resolveSubjectsFromHousing(tab, renderSubjects, this);
    }

    function renderSubjects(subjects, tab){
        var toAdd = [];
        if (!subjects.length){
            toAdd.push({
                html: 'No animals were found.',
                border: false
            });
        }
        else if (subjects.length < 10) {
            for (var i=0;i<subjects.length;i++){
                toAdd.push({
                    xtype: 'ldk-webpartpanel',
                    title: 'Overview: ' + subjects[i],
                    items: [{
                        xtype: 'nbri_ehr-snapshotpanel',
                        showExtendedInformation: true,
                        showActionsButton: !!showActionsBtn,
                        hrefTarget: '_blank',
                        border: false,
                        subjectId: subjects[i]
                    }]
                });

                toAdd.push({
                    border: false,
                    height: 20
                });

                toAdd.push(EHR.reports.renderWeightData(panel, tab, subjects[i]));
            }
        }
        else {
            toAdd.push({
                html: 'Because more than 10 subjects were selected, the condensed report is being shown.  Note that you can click the animal ID to open this same report in a different tab, showing that animal in more detail or click the link labeled \'Show Hx\'.',
                style: 'padding-bottom: 20px;',
                border: false
            });

            var filterArray = panel.getFilterArray(tab);
            var title = panel.getTitleSuffix();
            toAdd.push({
                xtype: 'ldk-querycmp',
                style: 'margin-bottom:20px;',
                queryConfig: {
                    title: 'Overview' + title,
                    schemaName: 'study',
                    queryName: 'demographics',
                    viewName: 'Snapshot',
                    filterArray: filterArray.removable.concat(filterArray.nonRemovable)
                }
            });
        }

        if (toAdd.length)
            tab.add(toAdd);
    }
};

EHR.reports.currentBlood = function(panel, tab){
    var filterArray = panel.getFilterArray(tab);
    var title = panel.getTitleSuffix();

    tab.add({
        html: 'This report summarizes the blood available for the animals below.  ' +
                '<br><br>If there have been recent blood draws for the animal, a graph will show the available blood over time.  On the graph, dots indicate dates when either blood was drawn or a previous blood draw fell off.  The horizontal lines indicate the maximum allowable blood that can be drawn on that date.',
        border: false,
        style: 'padding-bottom: 20px;'
    });

    tab.add({
        xtype: 'ldk-querycmp',
        style: 'margin-bottom: 10px;',
        queryConfig: panel.getQWPConfig({
            title: 'Summary',
            schemaName: 'study',
            queryName: 'Demographics',
            viewName: 'Blood Draws',
            filterArray: filterArray.removable.concat(filterArray.nonRemovable)
        })
    });

    var subjects = tab.filters.subjects || [];

    if (subjects.length){
        tab.add({
            xtype: 'nbri-bloodsummarypanel',
            subjects: subjects
        });
    }
    else
    {
        panel.resolveSubjectsFromHousing(tab, function(subjects, tab){
            tab.add({
                xtype: 'nbri-bloodsummarypanel',
                subjects: subjects
            });
        }, this);
    }
};

EHR.reports.clinicalHistory = function(panel, tab, showActionsBtn, includeAll){
    if (tab.filters.subjects){
        renderSubjects(tab.filters.subjects, tab);
    }
    else
    {
        panel.resolveSubjectsFromHousing(tab, renderSubjects, this);
    }

    function renderSubjects(subjects, tab){
        if (subjects.length > 10){
            tab.add({
                html: 'Because more than 10 subjects were selected, the condensed report is being shown.  Note that you can click the animal ID to open this same report in a different tab, showing that animal in more detail or click the link labeled \'Show Hx\'.',
                style: 'padding-bottom: 20px;',
                border: false
            });

            var filterArray = panel.getFilterArray(tab);
            var title = panel.getTitleSuffix();
            tab.add({
                xtype: 'ldk-querycmp',
                style: 'margin-bottom:20px;',
                queryConfig: {
                    title: 'Overview' + title,
                    schemaName: 'study',
                    queryName: 'demographics',
                    viewName: 'Snapshot',
                    filterArray: filterArray.removable.concat(filterArray.nonRemovable)
                }
            });

            return;
        }

        if (!subjects.length){
            tab.add({
                html: 'No animals were found.',
                border: false
            });

            return;
        }

        tab.addCls('ehr-snapshotsubpanel');

        var minDate = includeAll ? null : Ext4.Date.add(new Date(), Ext4.Date.YEAR, -30);
        var toAdd = [];
        Ext4.each(subjects, function(s){
            toAdd.push({
                html: '<span style="font-size: large;"><b>Animal: ' + Ext4.htmlEncode(s) + '</b></span>',
                style: 'padding-bottom: 20px;',
                border: false
            });

            toAdd.push({
                xtype: EHR.reports.clinicalHistoryPanelXtype,
                showActionsButton: !!showActionsBtn,
                hrefTarget: '_blank',
                border: false,
                subjectId: s
            });

            toAdd.push({
                html: '<b>Chronological History:</b><hr>',
                style: 'padding-top: 5px;',
                border: false
            });

            toAdd.push({
                xtype: 'nbri_ehr-clinicalhistorypanel',
                border: true,
                subjectId: s,
                autoLoadRecords: true,
                minDate: minDate,
                //maxGridHeight: 1000,
                hrefTarget: '_blank',
                style: 'margin-bottom: 20px;'
            });
        }, this);

        if (toAdd.length){
            tab.add(toAdd);
        }
    }
};

EHR.reports.observationSchedule = function(panel, tab, viewName){
    var filterArray = panel.getFilterArray(tab);
    var title = panel.getTitleSuffix();

    var date = Ext4.Date.format(new Date(), LABKEY.extDefaultDateFormat);
    tab.add({
        xtype: 'ldk-querypanel',
        style: 'margin-bottom:20px;',
        queryConfig: panel.getQWPConfig({
            schemaName: 'study',
            queryName: 'observationSchedule',
            title: 'Observation Schedule ' + title,
            filters: filterArray.nonRemovable,
            removeableFilters: filterArray.removable,
            parameters: {
                StartDate: date,
                NumDays: 1
            }
        })
    });
}

EHR.reports.incompleteTreatments = function(panel, tab, viewName){
    const currentDate = new Date();
    const previousDate = new Date();
    previousDate.setDate(currentDate.getDate() - 30);

    const gridFilterArray = panel.getFilterArray(tab);
    const filterArray = gridFilterArray.nonRemovable.concat(LABKEY.Filter.create('treatmentStatus', "Completed", LABKEY.Filter.Types.NEQ_OR_NULL));

    const config = panel.getQWPConfig({
        title: 'Incomplete Treatments For Past 30 Days',
        schemaName: 'study',
        queryName: 'treatmentSchedule',
        dataRegionName: 'incomplete_treatments',
        parameters: {
            StartDate: `${String(previousDate.getMonth() + 1).padStart(2, '0')}/${String(previousDate.getDate()).padStart(2, '0')}/${previousDate.getFullYear()}`,
            NumDays: 30
        },
        filters: filterArray,
        removeableFilters: gridFilterArray.removable,
        frame: true
    });

    tab.add({
        xtype: 'ldk-querycmp',
        style: 'margin-bottom:20px;',
        queryConfig: config
    });
}

EHR.reports.incompleteObservations = function(panel, tab, viewName){
    const currentDate = new Date();
    const previousDate = new Date();
    previousDate.setDate(currentDate.getDate() - 30);

    const gridFilterArray = panel.getFilterArray(tab);
    const filterArray = gridFilterArray.nonRemovable.concat(
            [LABKEY.Filter.create('status', "Completed", LABKEY.Filter.Types.DOES_NOT_CONTAIN),
            ]
    );

    const config = panel.getQWPConfig({
        title: 'Incomplete Observations For Past 30 Days',
        schemaName: 'study',
        queryName: 'observationSchedule',
        dataRegionName: 'incomplete_observations',
        parameters: {
            StartDate: `${String(previousDate.getMonth() + 1).padStart(2, '0')}/${String(previousDate.getDate()).padStart(2, '0')}/${previousDate.getFullYear()}`,
            NumDays: 30
        },
        filters: filterArray,
        removeableFilters: gridFilterArray.removable,
        frame: true
    });

    tab.add({
        xtype: 'ldk-querycmp',
        style: 'margin-bottom:20px;',
        queryConfig: config
    });
}
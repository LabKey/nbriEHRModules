/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('TreatmentSchedule', {
    allQueries: {
        project: {
            hidden: true,
            allowBlank: true
        },
        code: {
            allowBlank: false,
            inheritFromParent: false,
            editorConfig: {
                xtype: 'labkey-combo',
                displayField: 'meaning',
                valueField: 'code',
                anyMatch: true,
                queryMode: 'local',
                store: {
                    type: 'labkey-store',
                    schemaName: 'ehr_lookups',
                    queryName: 'snomed',
                    columns: 'code,meaning',
                    sort: 'meaning,code',
                    filterArray: [LABKEY.Filter.create('dateDisabled', null, LABKEY.Filter.Types.ISBLANK)],
                    autoLoad: true,
                    getRecordForCode: function(code){
                        debugger
                        var recIdx = this.findExact('code', code);
                        if (recIdx !== -1){
                            return this.getAt(recIdx);
                        }
                    }
                }
            }
        },
        volume: {
            inheritFromParent: false,
            shownInGrid: true,
            compositeField: 'Volume',
            xtype: 'nbri_ehr-drugvolumefield',
            noSaveInTemplateByDefault: true,
            editorConfig: {
                decimalPrecision: 3
            },
            header: 'Vol',
            columnConfig: {
                width: 90
            }
        },
        type: {
            hidden: true
        },
        attachmentFile: {
            hidden: true
        }
    },
    byQuery: {
        'study.drug': {
            date: {
                header: 'Date/Time',
            }
        },
        'study.treatment_order': {
            enddate: {
                allowBlank: true,
            }
        }
    }
});
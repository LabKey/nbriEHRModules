/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('MedicationEndDate', {
    allQueries: {
        'code': {
            editorConfig: {
                listeners: {
                    // End date calculated from Default Duration in the formulary and the date field of data entry grid.
                    // Only calculate end date when treatment is selected and there is a date in the field.
                    // Don’t recalculate if changing drug multiple times.
                    change: function (field, newVal, oldVal) {
                        if (newVal && !oldVal) {
                            let record = EHR.DataEntryUtils.getBoundRecord(field);
                            if (record) {
                                LABKEY.Query.selectRows({
                                    schemaName: 'ehr_lookups',
                                    queryName: 'drug_defaults',
                                    columns: ['duration','offset'],
                                    scope: this,
                                    ignoreFilter: true,
                                    filterArray: [LABKEY.Filter.create('code', newVal)],
                                    success: function (data) {
                                        if (data.rows && data.rows.length > 0) {
                                            let defaultDuration = data.rows[0].duration;
                                            let offset = data.rows[0].offset;
                                            let date = record.get('date');
                                            if (defaultDuration && date) {
                                                let endDate = new Date(date);
                                                endDate.setDate(endDate.getDate() + defaultDuration);
                                                if (offset) {
                                                    endDate.setHours(endDate.getHours() + offset);
                                                }
                                                EHR.DataEntryUtils.setSiblingFields(field, {
                                                    enddate: endDate
                                                });
                                            }
                                        }
                                    },
                                    failure: function(error) {
                                        console.error(error);
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    },
});
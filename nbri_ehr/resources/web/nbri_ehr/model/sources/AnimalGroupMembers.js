/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('AnimalGroupMembers', {
    byQuery: {
        'study.animal_group_members': {
            Id: {
                xtype: 'ehr-animalIdUpperField',
                dataIndex: 'Id',
                nullable: false,
                allowBlank: false,
                lookups: false,
                noSaveInTemplateByDefault: true,
                columnConfig: {
                    width: 95,
                    showLink: false
                },
                editorConfig: {
                    allowAnyId: true
                }
            },
            date: {
                allowBlank: false,
                nullable: false,
                noSaveInTemplateByDefault: true,
                hidden: false,
                getInitialValue: function(v, rec){
                    if (v)
                        return v;

                    let curDate = new Date();
                    curDate.setHours(0, 0, 0, 0);
                    return curDate;
                }
            },
            groupId: {
                allowBlank: false,
                nullable: false,
                lookup: {
                    // the shared default filters on a date column that the breeding type lookup does not have
                    filterArray: []
                }
            },
            performedBy: {
                shownInGrid: false,
                hidden: true
            }
        }
    }
});

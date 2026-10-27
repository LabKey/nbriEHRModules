/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
EHR.model.DataModelManager.registerMetadata('NewProtocol', {
    allQueries: {

    },
    byQuery: {
        'ehr.protocol': {
            // the shared EHR defaults hide this as a system key; here it is the IACUC number the user types. The column
            // is described as a textarea, which would size the box to several rows.
            protocol: {
                xtype: 'textfield',
                inputType: 'text',
                height: null,
                hidden: false,
                userEditable: true,
                allowBlank: false,
                nullable: false
            },
            title: {
                allowBlank: false,
                nullable: false
            },
            investigatorId: {
                allowBlank: false,
                nullable: false
            },
            project_type: {
                allowBlank: false,
                nullable: false
            },
            approve: {
                xtype: 'datefield',
                extFormat: LABKEY.extDefaultDateFormat,
                allowBlank: false,
                nullable: false
            },
            enddate: {
                xtype: 'datefield',
                extFormat: LABKEY.extDefaultDateFormat,
                allowBlank: false,
                nullable: false
            },
            // the shared EHR defaults hide every description column
            description: {
                hidden: false,
                xtype: 'textarea',
                height: 75,
                editorConfig: {
                    resizeDirections: 's'
                }
            },
            ibc_approval_required: {
                editorConfig: {
                    listeners: {
                        change: function(field, value){
                            var form = field.up('form');
                            var num = form ? form.getForm().findField('ibc_approval_num') : null;
                            if (num){
                                num.setVisible(!!value);
                            }
                        }
                    }
                }
            },
            // shown once IBC approval is marked required
            ibc_approval_num: {
                editorConfig: {
                    hidden: true
                }
            },
            first_approval: {
                hidden: true
            },
            objectid: {
                hidden: true
            }
        }
    }
});

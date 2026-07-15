/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

EHR.DataEntryUtils.registerDataEntryFormButton('NBRI_TREATMENT_SUBMIT', {
    text: 'Submit Final',
    name: 'submit',
    requiredQC: 'Completed',
    targetQC: 'Completed',
    errorThreshold: 'INFO',
    successURL: LABKEY.ActionURL.getParameter('returnUrl') || LABKEY.ActionURL.buildURL('ehr', 'enterData.view'),
    disabled: true,
    itemId: 'submitBtn',
    handler: function(btn){

        const panel = btn.up('ehr-dataentrypanel');
        const casesStore = panel?.storeCollection?.getClientStoreByName('cases');
        if (casesStore) {
            const rec = casesStore.getAt(0);
            const caseid = casesStore.getAt(0).get('caseid');
            const qcstate = casesStore.getAt(0).get('qcstate/label');
            if (!caseid || (qcstate && qcstate !== 'Completed')) { // only check for new cases or cases going from some other state to Completed
                const id = rec.get('Id');
                const problemCategory = rec.get('problemCategory');
                const category = rec.get('category');
                if (id && problemCategory && category) {
                    const filters = [
                        LABKEY.Filter.create('Id', id),
                        LABKEY.Filter.create('category', category),
                        LABKEY.Filter.create('problemCategory', problemCategory),
                        LABKEY.Filter.create('isActive', true),
                        LABKEY.Filter.create('QCState/Label', "Completed", LABKEY.Filter.Types.EQUAL)
                    ]
                    const caseid = rec.get('caseid');
                    if (caseid) {
                        filters.push(LABKEY.Filter.create('caseid', caseid, LABKEY.Filter.Types.NEQ))
                    }
                    LABKEY.Query.selectRows({
                        schemaName: 'study',
                        queryName: 'cases',
                        filterArray: filters,
                        columns: 'problemSubcategory',
                        scope: this,
                        ignoreFilter: true,
                        failure: LDK.Utils.getErrorCallback(),
                        success: function (results) {
                            if (results.rows.length > 0) {
                                const subcategories = [];
                                for (let i = 0; i < results.rows.length; i++) {
                                    subcategories.push(results.rows[i].problemSubcategory);
                                }
                                let msg;
                                if (subcategories.length === 1) {
                                    msg = 'This animal already has a case with the problem ' + problemCategory + '. The subcategory is ' + results.rows[0].problemSubcategory + '. Do you still want to submit this case?';
                                }
                                else {
                                    msg = 'This animal already has ' + subcategories.length + ' cases with the problem ' + problemCategory + '. The subcategories are ' + subcategories.join(', ') + '. Do you still want to submit this case?';
                                }

                                Ext4.Msg.confirm('Similar Case Exists', msg, function (v) {
                                    if (v == 'yes')
                                        this.onSubmit(btn);
                                }, this);
                            }
                            else {
                                Ext4.Msg.confirm('Finalize Form', 'You are about to finalize this form.  Do you want to do this?', function (v) {
                                    if (v == 'yes')
                                        this.onSubmit(btn);
                                }, this);
                            }
                        }
                    });
                }
                else {
                    Ext4.Msg.confirm('Finalize Form', 'You are about to finalize this form.  Do you want to do this?', function (v) {
                        if (v == 'yes')
                            this.onSubmit(btn);
                    }, this);
                }
            }
            else {
                Ext4.Msg.confirm('Finalize Form', 'You are about to finalize this form.  Do you want to do this?', function (v) {
                    if (v == 'yes')
                        this.onSubmit(btn);
                }, this);
            }
        }
        else {
            Ext4.Msg.confirm('Finalize Form', 'You are about to finalize this form.  Do you want to do this?', function (v) {
                if (v == 'yes')
                    this.onSubmit(btn);
            }, this);
        }
    },
    listeners: {
        afterRender: function(btn){
            const treatmentid = LABKEY.ActionURL.getParameter('treatmentid');
            const scheduledDate = LABKEY.ActionURL.getParameter('scheduledDate');
            const obsTask = LABKEY.ActionURL.getParameter('obsTask');
            const id = LABKEY.ActionURL.getParameter('id');
            const observations = LABKEY.ActionURL.getParameter('observations');
            const prcOrderId = LABKEY.ActionURL.getParameter('prcOrderId');

            if (treatmentid) {

                this.addEvents('animalchange');
                this.enableBubble('animalchange');

                function onSuccess(results) {
                    if (results.rows.length === 0) {
                        console.error('No treatment order found for treatmentid ' + treatmentid);
                        return;
                    }

                    const drugGrid = this.up('ehr-dataentrypanel').query('grid').find(e => e.title === "Medications/Treatments Given");
                    if (!drugGrid) {
                        console.error('Medication/Treatments grid not found');
                        return;
                    }

                    const row = results.rows[0];
                    const record = {
                        Id: row.Id?.value,
                        category: row.category?.value,
                        code: row.code?.value,
                        route: row.route?.value,
                        concentration: row.concentration?.value,
                        conc_units: row.conc_units?.value,
                        amount: row.amount?.value,
                        amount_units: row.amount_units?.value,
                        dosage: row.dosage?.value,
                        dosage_units: row.dosage_units?.value,
                        volume: row.volume?.value,
                        vol_units: row.vol_units?.value,
                        orderedby: row.orderedby?.value,
                        treatmentid: row.objectid?.value,
                        caseid: row.caseid?.value
                    };

                    if (scheduledDate) {
                        record.scheduledDate = scheduledDate;
                    }

                    drugGrid.store.add(record);

                    this.fireEvent('animalchange', row.Id?.value);
                    drugGrid.fireEvent('panelDataChange');
                }

                LABKEY.Query.selectRows({
                    requiredVersion: 9.1,
                    schemaName: 'study',
                    queryName: 'treatment_order',
                    columns: 'Id,meaning,code,qualifier,route,concentration,conc_units,amount,amount_units,dosage,dosage_units,volume,vol_units,remark,category,objectid,orderedby',
                    filterArray: [LABKEY.Filter.create('objectid', treatmentid, LABKEY.Filter.Types.EQUAL)],
                    scope: this,
                    ignoreFilter: true,
                    success: onSuccess,
                    failure: LDK.Utils.getErrorCallback()
                });

            }

            if (id && observations) {
                this.addEvents('animalchange');
                this.enableBubble('animalchange');

                function onObsSuccess(results) {
                    if (results.rows.length === 0) {
                        console.error('No observations found for taskid ' + obsTask + ' and id ' + id);
                        return;
                    }

                    const obsGrid = this.up('ehr-dataentrypanel').query('grid').find(e => e.title === "Observations");
                    if (!obsGrid) {
                        console.error('Observations grid not found');
                        return;
                    }

                    const categories = [];
                    for (let i = 0; i < results.rows.length; i++) {
                        let row = results.rows[i];

                        if (!categories.includes(row.category.value)) {
                            let record = {
                                Id: row.Id?.value,
                                category: row.category?.value,
                                area: row.area?.value,
                                orderId: row.objectid?.value,
                                caseid: row.caseid?.value
                            };

                            if (scheduledDate) {
                                record.scheduledDate = scheduledDate;
                            }

                            obsGrid.store.add(record);
                            categories.push(row.category?.value);
                        }
                    }

                    this.fireEvent('animalchange', results.rows[0].Id.value);
                    obsGrid.fireEvent('panelDataChange');
                }

                LABKEY.Query.selectRows({
                    requiredVersion: 9.1,
                    schemaName: 'study',
                    queryName: 'observation_order',
                    columns: 'Id,category,area,objectid,caseid',
                    filterArray: [LABKEY.Filter.create('taskid', obsTask, LABKEY.Filter.Types.EQUALS_ONE_OF),
                        LABKEY.Filter.create('id', id, LABKEY.Filter.Types.EQUAL),
                        LABKEY.Filter.create('category', observations, LABKEY.Filter.Types.EQUALS_ONE_OF)],
                    scope: this,
                    ignoreFilter: true,
                    success: onObsSuccess,
                    failure: LDK.Utils.getErrorCallback()
                });
            }

            if (prcOrderId) {

                this.addEvents('animalchange');
                this.enableBubble('animalchange');

                function onSuccess(results) {
                    if (results.rows.length === 0) {
                        console.error('No procedure order found for procedure id ' + prcOrderId);
                        return;
                    }

                    const prcGrid = this.up('ehr-dataentrypanel').query('grid').find(e => e.title === "Procedures");
                    if (!prcGrid) {
                        console.error('Procedures grid not found');
                        return;
                    }

                    const row = results.rows[0];
                    const record = {
                        Id: row.Id?.value,
                        category: row.category?.value,
                        procedure: row.procedure?.value,
                        caseid: row.caseid?.value,
                        orderedby: row.orderedby?.value,
                        orderid: row.objectid?.value
                    };

                    prcGrid.store.add(record);

                    this.fireEvent('animalchange', row.Id.value);
                    prcGrid.fireEvent('panelDataChange');
                }

                LABKEY.Query.selectRows({
                    requiredVersion: 9.1,
                    schemaName: 'study',
                    queryName: 'prc_order',
                    columns: 'Id,procedure,remark,category,objectid,orderedby,caseid',
                    filterArray: [LABKEY.Filter.create('objectid', prcOrderId, LABKEY.Filter.Types.EQUAL)],
                    ignoreFilter: true,
                    scope: this,
                    success: onSuccess,
                    failure: LDK.Utils.getErrorCallback()
                });

            }

        }
    },
    disableOn: 'WARN'
});
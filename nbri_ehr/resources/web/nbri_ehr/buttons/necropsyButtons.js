/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
/**
 * Submit buttons for the Necropsy Request, Necropsy and Necropsy Final forms. Each stamps the necropsy status on the
 * necropsy task and records its stage as the task's form type.
 */
(function(){

    // Resubmitting an earlier stage's form must not move a necropsy back from a later stage.
    var STATUS_RANK = {
        'Request Pending': 0,
        'Preliminary Results Submitted': 1,
        'Final Results Submitted': 2,
        'Final Results Submitted: Pending Addendum': 2
    };
    var FORM_TYPES = ['Necropsy Request', 'Necropsy', 'Necropsy Final'];

    var successURL = LABKEY.ActionURL.getParameter('returnUrl') || LABKEY.ActionURL.buildURL('query', 'executeQuery.view', null, {
        schemaName: 'nbri_ehr',
        'query.queryName': 'necropsyTask'
    });

    // force is for the final stage, which may move between its two statuses
    function setStage(panel, status, formType, force){
        var necropsyTask = panel.storeCollection.getClientStoreByName('necropsyTask');
        if (necropsyTask) {
            necropsyTask.each(function(rec){
                var current = STATUS_RANK[rec.get('status')];
                if (force || current === undefined || current < STATUS_RANK[status])
                    rec.set('status', status);
            });
        }

        // later stages save it only when modified, and its QC state must follow this submission
        var necropsyTaskServer = panel.storeCollection.getServerStoreForQuery('nbri_ehr', 'necropsyTask');
        if (necropsyTaskServer) {
            necropsyTaskServer.each(function(rec){
                rec.setDirty();
            });
        }

        var tasks = panel.storeCollection.getClientStoreByName('tasks');
        if (tasks) {
            tasks.each(function(rec){
                if (FORM_TYPES.indexOf(rec.get('formtype')) < FORM_TYPES.indexOf(formType))
                    rec.set('formtype', formType);
            });
        }
    }

    function submitForReview(panel, btn){
        Ext4.create('EHR.window.SubmitForReviewWindow', {
            dataEntryPanel: panel,
            dataEntryBtn: btn,
            reviewRequiredRecipient: panel.formConfig.defaultReviewRequiredPrincipal
        }).show();
    }

    function registerStageButtons(prefix, formType, status, submitText, submitHandler){
        EHR.DataEntryUtils.registerDataEntryFormButton(prefix + '_REVIEW', {
            text: 'Submit for Review',
            name: prefix.toLowerCase() + '_review',
            requiredQC: 'Review Required',
            targetQC: 'Review Required',
            errorThreshold: 'WARN',
            successURL: successURL,
            disabled: true,
            itemId: prefix.toLowerCase() + 'ReviewBtn',
            disableOn: 'ERROR',
            handler: function(btn){
                var panel = btn.up('ehr-dataentrypanel');
                setStage(panel, status, formType);
                submitForReview(panel, btn);
            }
        });

        EHR.DataEntryUtils.registerDataEntryFormButton(prefix + '_SUBMIT', {
            text: submitText,
            name: prefix.toLowerCase() + '_submit',
            requiredQC: 'Completed',
            targetQC: 'Completed',
            errorThreshold: 'WARN',
            successURL: successURL,
            disabled: true,
            itemId: prefix.toLowerCase() + 'SubmitBtn',
            disableOn: 'WARN',
            handler: function(btn){
                var panel = btn.up('ehr-dataentrypanel');
                if (submitHandler) {
                    submitHandler(panel, btn);
                }
                else {
                    setStage(panel, status, formType);
                    panel.onSubmit(btn);
                }
            }
        });
    }

    registerStageButtons('NECROPSY_REQUEST', 'Necropsy Request', 'Request Pending', 'Submit Request');
    registerStageButtons('NECROPSY_PRELIMINARY', 'Necropsy', 'Preliminary Results Submitted', 'Submit Preliminary Results');
    registerStageButtons('NECROPSY_FINAL', 'Necropsy Final', 'Final Results Submitted', 'Submit Final Results', function(panel, btn){
        Ext4.Msg.show({
            title: 'Submit Final Necropsy',
            msg: 'Are there any pending ancillary tests or addenda?',
            buttons: Ext4.Msg.YESNOCANCEL,
            buttonText: {
                yes: 'Submit with Pending Addenda',
                no: 'Submit Final'
            },
            fn: function(buttonId){
                if (buttonId === 'cancel')
                    return;

                setStage(panel, buttonId === 'yes' ? 'Final Results Submitted: Pending Addendum' : 'Final Results Submitted', 'Necropsy Final', true);
                panel.onSubmit(btn);
            }
        });
    });
})();

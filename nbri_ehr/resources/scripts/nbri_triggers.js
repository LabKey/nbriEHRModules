/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

var console = require("console");
var LABKEY = require("labkey");

// For more complicated logic, I highly recommend implementing this in java.  This provides much better debugging, and the full use of the server-side java APIs
// Methods on this TriggerHelper can be called from JS, and can return values to the JS code.  In general, I try to only pass primitives; however, that may not always be required

// NOTE: NBRI specific script options should be set here in INIT triggers

exports.init = function (EHR) {


    EHR.Server.TriggerManager.registerHandler(EHR.Server.TriggerManager.Events.INIT, function (event, helper, EHR) {

        EHR.Server.TriggerManager.unregisterAllHandlersForQueryNameAndEvent('ehr_lookups', 'cage', EHR.Server.TriggerManager.Events.BEFORE_UPSERT);
        EHR.Server.TriggerManager.unregisterAllHandlersForQueryNameAndEvent('ehr_lookups', 'procedures', EHR.Server.TriggerManager.Events.BEFORE_DELETE);
        EHR.Server.TriggerManager.unregisterAllHandlersForQueryNameAndEvent('study', 'assignment', EHR.Server.TriggerManager.Events.BEFORE_UPSERT);
        EHR.Server.TriggerManager.unregisterAllHandlersForQueryNameAndEvent('study', 'clinremarks', EHR.Server.TriggerManager.Events.BEFORE_UPSERT);
        EHR.Server.TriggerManager.unregisterAllHandlersForQueryNameAndEvent('study', 'clinremarks', EHR.Server.TriggerManager.Events.AFTER_UPSERT);
        EHR.Server.TriggerManager.unregisterAllHandlersForQueryNameAndEvent('study', 'blood', EHR.Server.TriggerManager.Events.BEFORE_UPSERT);
        EHR.Server.TriggerManager.unregisterAllHandlersForQueryNameAndEvent('study', 'cases', EHR.Server.TriggerManager.Events.AFTER_UPSERT);
        EHR.Server.TriggerManager.unregisterAllHandlersForQueryNameAndEvent('study', 'cases', EHR.Server.TriggerManager.Events.AFTER_DELETE);

        helper.setScriptOptions({
            datasetsToClose: ['assignment', 'protocolAssignment' , 'housing', 'treatment_order', 'observation_order', 'cases', 'pairings', 'exemptions', 'flags']
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'arrival', function(event, helper) {
        helper.setScriptOptions({
            allowAnyId: true,
            requiresStatusRecalc: false, // set in upsert to handle rearrival
            allowDatesInDistantPast: true,
            skipAssignmentCheck: true,
        });
    });

    // the arrival and birth forms assign animals that do not have a demographics record yet, so those forms ask for Id validation to be relaxed
    function isAllowAnyIdRequested(helper) {
        helper.decodeExtraContextProperty('allowAnyId', false);
        return helper.getProperty('allowAnyId') === true; // this can be true or an empty object
    }

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'protocolAssignment', function(event, helper) {
        helper.setScriptOptions({
            allowAnyId: isAllowAnyIdRequested(helper),
            requiresStatusRecalc: false,
            allowDatesInDistantPast: true
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'assignment', function(event, helper) {
        helper.setScriptOptions({
            allowAnyId: isAllowAnyIdRequested(helper),
            requiresStatusRecalc: false,
            allowDatesInDistantPast: true,
            skipAssignmentCheck: true,
            removeTimeFromDate: false,
            doStandardProtocolCountValidation: false
        });

        helper.decodeExtraContextProperty('assignmentsInTransaction', []);

        helper.registerRowProcessor(function(helper, row){
            if (!row)
                return;

            if (!row.Id || !row.project){
                return;
            }

            var assignmentsInTransaction = helper.getProperty('assignmentsInTransaction');
            assignmentsInTransaction = assignmentsInTransaction || [];

            var shouldAdd = true;
            if (row.objectid){
                LABKEY.ExtAdapter.each(assignmentsInTransaction, function(r){
                    if (r.objectid === row.objectid){
                        shouldAdd = false;
                        return false;
                    }
                }, this);
            }

            if (shouldAdd){
                assignmentsInTransaction.push({
                    Id: row.Id,
                    objectid: row.objectid,
                    date: row.date,
                    enddate: row.enddate,
                    qcstate: row.QCState,
                    project: row.project
                });
            }

            helper.setProperty('assignmentsInTransaction', assignmentsInTransaction);
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'alias', function(event, helper) {
        helper.setScriptOptions({
            allowDatesInDistantPast: true,
            removeTimeFromDate: false,
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'drug', function(event, helper) {
        helper.setScriptOptions({
            allowDeadIds: true,
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'flags', function(event, helper) {
        helper.setScriptOptions({
            allowFutureDates: true,
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'treatment_order', function(event, helper) {
        helper.setScriptOptions({
            allowFutureDates: true,
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'observation_order', function(event, helper, EHR) {
        helper.setScriptOptions({
            allowFutureDates: true,
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'deaths', function(event, helper) {

        helper.setScriptOptions({
            datasetsToClose: ['assignment', 'protocolAssignment' , 'housing', 'treatment_order', 'observation_order', 'cases', 'pairings', 'exemptions', 'flags'],
            allowShippedIds: false,
            allowDeadIds: false,
            requiresStatusRecalc: true,
            allowRequestsInPast: true,
            removeTimeFromDate: false,
            allowDatesInDistantPast: true,
            allowAnyId: true
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'necropsy', function(event, helper) {

        helper.setScriptOptions({
            removeTimeFromDate: false
        });
    });

    EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'cases', function(event, helper) {
        helper.setScriptOptions({
            allowDatesInDistantPast: true
        });
    });
}

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);
EHR.Server.Utils = require("ehr/utils").EHR.Server.Utils;

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);
var idsToSync = [];

function onInit(event, helper){
    helper.setScriptOptions({
        allowAnyId: true,
        requiresStatusRecalc: true,
        allowDeadIds: true,
        skipIdFormatCheck: true,
        skipHousingCheck: true,
        announceAllModifiedParticipants: true,
        allowDatesInDistantPast: true,
        removeTimeFromDate: true,
        skipAssignmentCheck: true,
    });

    // the script scope can outlive a single save, so never inherit ids from a prior one
    idsToSync = [];

    helper.decodeExtraContextProperty('birthsInTransaction');
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.COMPLETE, 'study', 'birth', function(event, errors, helper){

    // Owns updates to the denormalized demographics birth date, derived from the saved birth records.
    // createDemographicsRecord() in the shared ehr script seeds it but never overwrites an existing row.
    if (!helper.isETL() && idsToSync.length) {
        var demographicsUpdates = triggerHelper.computeDemographicsSync(idsToSync);
        if (demographicsUpdates.size() > 0) {
            helper.getJavaHelper().updateDemographicsRecord(demographicsUpdates);
        }
        idsToSync = [];
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'birth', function(helper, scriptErrors, row, oldRow) {

    if (!oldRow && row.Id && triggerHelper.birthExists(row.Id)) {
        EHR.Server.Utils.addError(scriptErrors, 'Id', 'Birth record already exists for this Id, update existing record to change birth information', 'ERROR');
    }

    if (!helper.isETL() && row.conceptId) {
        if (triggerHelper.totalRecords('nbri_ehr', 'Conception', 'ConceptId', row.conceptId) === 0) {
            EHR.Server.Utils.addError(scriptErrors, 'conceptId', 'This conception Id does not match any conception record', 'WARN');
        }

        //when updating a record that already carries this conception id, the existing row accounts for one match
        var conceptIdThreshold = (oldRow && oldRow.conceptId === row.conceptId) ? 1 : 0;
        if (triggerHelper.totalRecords('study', 'birth', 'conceptId', row.conceptId) > conceptIdThreshold) {
            EHR.Server.Utils.addError(scriptErrors, 'conceptId', 'This conception Id is already used by another birth record', 'WARN');
        }

        if (triggerHelper.totalRecords('study', 'pregnancy', 'conceptId', row.conceptId) > 0) {
            EHR.Server.Utils.addError(scriptErrors, 'conceptId', 'This conception Id is already used by a pregnancy outcome record', 'INFO');
        }
    }

    if (!helper.isETL()) {

        if (row.QCStateLabel) {
            row.qcstate = helper.getJavaHelper().getQCStateForLabel(row.QCStateLabel).getRowId();
        }

        if (row.Id && row.date) {

            let assignmentRec = {
                Id: row.Id,
                date: row.date,
                taskid: row.taskid,
                remark: row.remark,
                qcstate: row.qcstate,
                performedby: row.performedby
            }

            if (row.project) {
                assignmentRec['project'] = row.project;
                triggerHelper.createAssignmentRecord("assignment", row.Id, assignmentRec);
            }

            if (row.birthProtocol) {
                assignmentRec['protocol'] = row.birthProtocol;
                triggerHelper.createAssignmentRecord("protocolAssignment", row.Id, assignmentRec);
            }
        }

        if (!helper.isGeneratedByServer() && !helper.isValidateOnly()) {

            // if 'cage', labeled as "Birth Location" is provided, then insert into housing.
            if (row.cage && row.Id && row.date) {
                var housingRec = {
                    Id: row.Id,
                    date: row.date,
                    cage: row.cage,
                    taskid: row.taskid,
                    qcstate: row.qcstate,
                    reason: 'Husbandry',
                    performedby: row.performedby
                }

                var housingErrors = triggerHelper.createHousingRecord(row.Id, housingRec, "birth");
                if (housingErrors) {
                    EHR.Server.Utils.addError(scriptErrors, 'Id', housingErrors, 'ERROR');
                }
            }

            // this allows demographic records in qcstates other than completed
            var extraDemographicsFieldMappings = {
                'qcstate': helper.getJavaHelper().getQCStateForLabel(row.QCStateLabel).getRowId()
            }

            var calc_status = (row.QCStateLabel.toUpperCase() === 'IN PROGRESS' || row.QCStateLabel.toUpperCase() === 'REVIEW REQUIRED') ? 'Alive - In Progress' : 'Alive';

            var obj = {
                Id: row.Id,
                date: row.date,
                calculated_status: calc_status,
                dam: row['Id/demographics/dam'] || null,
                sire: row['Id/demographics/sire'] || null,
                species: row['Id/demographics/species'] || null,
                birth: row.date || null,
                gender: row['Id/demographics/gender'] || null,
                socialCode: row['Id/demographics/socialCode'] || null,
                taskid: row.taskid,
                remark: row.remark,
                QCStateLabel: row.QCStateLabel,
                performedby: row.performedby
            };

            //find dam, if provided
            if (obj.dam && !obj.origin) {
                obj.origin = helper.getJavaHelper().getGeographicOrigin(obj.dam);
            }

            if (obj.dam && !obj.species) {
                obj.species = helper.getJavaHelper().getSpecies(obj.dam);
            }

            if (!oldRow) {
                //if not already present, we insert into demographics
                helper.getJavaHelper().createDemographicsRecord(row.Id, obj, extraDemographicsFieldMappings);
            }
            else {
                //Update demographics records
                var ar = helper.getJavaHelper().getDemographicRecord(obj.Id);
                var data = ar || {};

                var record = {};
                var hasUpdates = false;

                if (obj.gender && obj.gender !== data.gender) {
                    record.gender = obj.gender;
                    hasUpdates = true;
                }

                if (obj.species && obj.species !== data.species) {
                    record.species = obj.species;
                    hasUpdates = true;
                }

                if (obj.sire && obj.sire !== data.sire) {
                    record.sire = obj.sire;
                    hasUpdates = true;
                }

                if (obj.dam && obj.dam !== data.dam) {
                    record.dam = obj.dam;
                    hasUpdates = true;
                }

                if (obj.socialCode && obj.socialCode !== data.socialCode) {
                    record.socialCode = obj.socialCode;
                    hasUpdates = true;
                }

                if (obj.performedby && obj.performedby !== data.performedby) {
                    record.performedby = obj.performedby;
                    hasUpdates = true;
                }

                if (obj.QCStateLabel && obj.QCStateLabel !== data.QCStateLabel) {
                    record.QCStateLabel = obj.QCStateLabel;
                    hasUpdates = true;
                }

                if (hasUpdates) {
                    console.info("Birth update for animal Id " + row.Id + " included demographic changes. Demographic record updated.");
                    record.Id = obj.Id;
                    var demographicsUpdates = [record];
                    helper.getJavaHelper().updateDemographicsRecord(demographicsUpdates);
                    helper.cacheDemographics(row.Id, row);
                }
            }

            if (row.Id && idsToSync.indexOf(row.Id) === -1) {
                idsToSync.push(row.Id);
            }
        }
    }
});
/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
require("ehr/triggers").initScript(this);

var triggerHelper = new org.labkey.nbri_ehr.query.NBRI_EHRTriggerHelper(LABKEY.Security.currentUser.id, LABKEY.Security.currentContainer.id);
var idsToSync = [];

// generation 0 is a real value, so emptiness cannot be tested by truthiness the way the other demographics fields test it
function isBlankGeneration(value) {
    return value === null || value === undefined || value === '';
}

// opens one assignment record against the animal being entered; each dataset carries the assignment under its own field
function createAssignment(scriptErrors, dataset, fieldName, value, row) {
    if (!value)
        return;

    var assignmentRec = {
        Id: row.Id,
        date: row.date,
        taskid: row.taskid,
        remark: row.remark,
        qcstate: row.qcstate,
        performedby: row.performedby
    };
    assignmentRec[fieldName] = value;

    var error = triggerHelper.createAssignmentRecord(dataset, row.Id, assignmentRec);
    if (error) {
        EHR.Server.Utils.addError(scriptErrors, 'Id', error, 'ERROR');
    }
}

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.INIT, 'study', 'Arrival', function(event, helper){

    // the script scope can outlive a single save, so never inherit ids from a prior one
    idsToSync = [];
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.BEFORE_UPSERT, 'study', 'Arrival', function(helper, scriptErrors, row, oldRow) {

    if(!row.rearrival){
        helper.setScriptOptions({requiresStatusRecalc: true});
    }

    // Due to order of operation, this needs to be done in upsert instead of insert
    if (!row.rearrival && helper.getEvent() == 'insert' && row.Id && triggerHelper.animalIdExists(row.Id)) {
        EHR.Server.Utils.addError(scriptErrors, 'Id', 'Animal Id ' + row.Id + ' is already in use. Please use a different Id.', 'ERROR');
    }

    // the arrival form seeds this to 0, so a blank one means an insert that bypassed the form
    if (!row.rearrival && !helper.isETL() && isBlankGeneration(row['Id/demographics/generation'])) {
        EHR.Server.Utils.addError(scriptErrors, 'Id/demographics/generation', 'Generation is required', 'ERROR');
    }

    if (row.eventDate) {
        row.date = row.eventDate;
    }

    helper.registerArrival(row.Id, row.date);

    //Insert or update demographic and birth records
    if (!row.rearrival && !helper.isETL() && !helper.isGeneratedByServer() && !helper.isValidateOnly()) {

        // this allows demographic records in qcstates other than completed
        var extraDemographicsFieldMappings = {
            'taskid': row.taskid,
            'qcstate': helper.getJavaHelper().getQCStateForLabel(row.QCStateLabel).getRowId()
        }

        // null (not undefined) required for call to java trigger helper
        row.dam = row['Id/demographics/dam'] || null;
        row.sire = row['Id/demographics/sire'] || null;
        row.species = row['Id/demographics/species'] || null;
        row.birth = row['Id/demographics/birth'] || null;
        row.gender = row['Id/demographics/gender'] || null;
        row.geographic_origin = row['Id/demographics/geographic_origin'] || null;
        row.socialCode = row['Id/demographics/socialCode'] || null;
        row.generation = isBlankGeneration(row['Id/demographics/generation']) ? null : row['Id/demographics/generation'];

        if (row.QCStateLabel) {
            row.qcstate = helper.getJavaHelper().getQCStateForLabel(row.QCStateLabel).getRowId();
        }

        if (row.birth) {
            var birthInfo = {
                Id: row.Id,
                date: row.birth,
                qcstate: row.qcstate,
                taskid: row.taskid,
                performedby: row.performedby
            }

            var birthErrors = triggerHelper.saveBirthRecord(row.Id, birthInfo);
            if (birthErrors){
                EHR.Server.Utils.addError(scriptErrors, 'birth', birthErrors, 'ERROR');
            }
        }

        // an animal arrives already assigned to a project, a protocol and a group, all entered on the arrival row
        if (row.Id && row.date) {
            createAssignment(scriptErrors, 'assignment', 'project', row.project, row);
            createAssignment(scriptErrors, 'protocolAssignment', 'protocol', row.arrivalProtocol, row);
            createAssignment(scriptErrors, 'animal_group_members', 'groupId', row.groupId, row);
        }

        // if 'cage', labeled as "Initial Location" is provided, then insert into housing.
        if (row.cage && row.Id && row.date) {
            var housingRec = {
                Id: row.Id,
                date: row.date,
                cage: row.cage,
                taskid: row.taskid,
                qcstate: row.qcstate,
                reason: row.arrivalType,
                performedby: row.performedby
            }

            var housingErrors = triggerHelper.createHousingRecord(row.Id, housingRec, "arrival");
            if (housingErrors) {
                EHR.Server.Utils.addError(scriptErrors, 'Id', housingErrors, 'ERROR');
            }
        }

        row.calculated_status = (row.QCStateLabel.toUpperCase() === 'IN PROGRESS' || row.QCStateLabel.toUpperCase() === 'REVIEW REQUIRED') ? 'Alive - In Progress' : 'Alive';

        if(!oldRow) {
            //if not already present, insert into demographics
            helper.getJavaHelper().createDemographicsRecord(row.Id, row, extraDemographicsFieldMappings);
        }
        else {
            //Update demographics records
            var ar = helper.getJavaHelper().getDemographicRecord(row.id);
            var data = ar || {};

            var obj = {};
            var hasUpdates = false;

            if (row.gender && row.gender !== data.gender )
            {
                obj.gender = row.gender;
                hasUpdates = true;
            }

            if (row.species && row.species !== data.species )
            {
                obj.species = row.species;
                hasUpdates = true;
            }

            if (row.geographic_origin && row.geographic_origin !== data.geographic_origin )
            {
                obj.geographic_origin = row.geographic_origin;
                hasUpdates = true;
            }

            if (row.socialCode && row.socialCode !== data.socialCode)
            {
                obj.socialCode = row.socialCode;
                hasUpdates = true;
            }

            if (row.sire && row.sire !== data.sire)
            {
                obj.sire = row.sire;
                hasUpdates = true;
            }

            if (row.dam && row.dam !== data.dam)
            {
                obj.dam = row.dam;
                hasUpdates = true;
            }

            if (row.QCStateLabel && row.QCStateLabel !== data.QCStateLabel)
            {
                obj.QCStateLabel = row.QCStateLabel;
                hasUpdates = true;
            }

            if (row.performedby && row.performedby !== data.performedby)
            {
                obj.performedby = row.performedby;
                hasUpdates = true;
            }

            if (hasUpdates)
            {
                console.info("Arrival update for animal Id " + row.Id + " included demographic changes Demographic record updated.");
                obj.Id = row.Id;
                var demographicsUpdates = [obj];
                helper.getJavaHelper().updateDemographicsRecord(demographicsUpdates);
                helper.cacheDemographics(row.Id, row);
            }
        }

        if (row.Id && idsToSync.indexOf(row.Id) === -1) {
            idsToSync.push(row.Id);
        }
    }
});

EHR.Server.TriggerManager.registerHandlerForQuery(EHR.Server.TriggerManager.Events.COMPLETE, 'study', 'Arrival', function(event, errors, helper){

    // Owns updates to the denormalized demographics birth date, read back from the birth record saveBirthRecord()
    // just wrote. createDemographicsRecord() seeds it on insert but never overwrites an existing row.
    if (!helper.isETL() && idsToSync.length) {
        var demographicsUpdates = triggerHelper.computeDemographicsSync(idsToSync);
        if (demographicsUpdates.size() > 0) {
            helper.getJavaHelper().updateDemographicsRecord(demographicsUpdates);
        }
        idsToSync = [];
    }
});
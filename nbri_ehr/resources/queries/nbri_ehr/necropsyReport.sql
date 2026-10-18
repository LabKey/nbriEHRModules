/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- each stage's data joins to the task directly, so a missing gross findings row cannot drop the final results
SELECT
    t.objectid,
    t.taskid,
    t.status,
    t.QCState.Label AS qcStateLabel,
    t.Date AS requestDate,
    t.SubmittingPI,
    t.SubmittingVet.DisplayName AS submittingVet,
    t.SubmittingVetCell,
    r.Id,
    r.breed,
    r.clinicalDiagnosis,
    r.Id.demographics.death AS death,
    r.Id.demographics.species.common_name AS species,
    r.Id.demographics.gender.meaning AS sex,
    r.Id.age.ageFriendly AS age,
    n.accessionNo,
    n.date AS necropsyDate,
    TIMESTAMPDIFF('SQL_TSI_MINUTE', r.Id.demographics.death, n.date) AS postMortemMinutes,
    n.preliminaryCauseOfDeath,
    n.preliminaryComments,
    g.remark AS grossFindings,
    f.causeCategory,
    f.causeOfDeath,
    f.comments
FROM nbri_ehr.necropsyTask t
LEFT JOIN study.necropsyRequest r ON r.taskid = t.taskid
LEFT JOIN study.necropsy n ON n.taskid = t.taskid
LEFT JOIN study.grossPathology g ON g.taskid = t.taskid
LEFT JOIN study.necropsyFinal f ON f.taskid = t.taskid

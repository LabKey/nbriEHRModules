/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT substring(adt.PRIMARY_KEY_VALUES, length('ANIMAL_EVENT_ID = ')) AS objectid,
       CAST(adt.CHANGE_DATETIME AS TIMESTAMP) AS modified,
       adt.REFERENCE
FROM AUDIT_TRAIL adt
WHERE adt.PRIMARY_KEY_VALUES LIKE '%ANIMAL_EVENT_ID%' AND
    (adt.REFERENCE LIKE '%General Condition%' OR
     adt.REFERENCE LIKE '%Assessment%' OR
     adt.REFERENCE LIKE '%Specific System Exam%' OR
     adt.REFERENCE LIKE '%EKG%' OR
     adt.REFERENCE LIKE '%AGM Panel%' OR
     adt.REFERENCE LIKE '%Macaque Panel%' OR
     adt.REFERENCE LIKE '%General Health Panel%' OR
     adt.REFERENCE LIKE '%TB Test Positive%' OR
     adt.REFERENCE LIKE '%TB Test Negative%' OR
     adt.REFERENCE LIKE '%TB Test Administered%' OR
     adt.REFERENCE LIKE '%Ophthalmic Exam-Indirect%' OR
     adt.REFERENCE LIKE '%Ophthalmic Exam-Direct%' OR
     adt.REFERENCE LIKE '%Physical Exam%' OR
     adt.REFERENCE LIKE '%Specific System Examination%' OR
     adt.REFERENCE LIKE '%Pregnancy Examination by Ultrasound%' OR
     adt.REFERENCE LIKE '%Deworm%' OR
     adt.REFERENCE LIKE '%Max Planck Inst for Evol Anthroplogy%' OR
     adt.REFERENCE LIKE '%Safety Assessment Staging%' OR
     adt.REFERENCE LIKE '%Pre-shipment requirements%' OR
     adt.REFERENCE LIKE '%Body Condition Score%' OR
     adt.REFERENCE LIKE '%Pregnancy Examination%' OR
     adt.REFERENCE LIKE '%Testicular Volume%' ) AND
    adt.COLUMN_NAME = 'DELETE'
/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT substring(adt.PRIMARY_KEY_VALUES, length('ANIMAL_EVENT_ID = ')) AS objectid,
       CAST(adt.CHANGE_DATETIME AS TIMESTAMP)                            AS modified,
       adt.REFERENCE
FROM AUDIT_TRAIL adt
WHERE adt.PRIMARY_KEY_VALUES LIKE '%ANIMAL_EVENT_ID%' AND
    (adt.REFERENCE LIKE '%Gross Pathology%' OR
     adt.REFERENCE LIKE '%Cardiopulmonary Disorder%' OR
     adt.REFERENCE LIKE '%Dermatological Disorder%' OR
     adt.REFERENCE LIKE '%Endocrine Disorder%' OR
     adt.REFERENCE LIKE '%Environmental Exposure%' OR
     adt.REFERENCE LIKE '%Gastrointestinal Disorder%' OR
     adt.REFERENCE LIKE '%Hematological Disorder%' OR
     adt.REFERENCE LIKE '%Iatrogenic%' OR
     adt.REFERENCE LIKE '%Immune Disorder%' OR
     adt.REFERENCE LIKE '%Infectious Disease%' OR
     adt.REFERENCE LIKE '%Metabolic Disorder%' OR
     adt.REFERENCE LIKE '%Musculoskeletal Disorder%' OR
     adt.REFERENCE LIKE '%Neoplasia%' OR
     adt.REFERENCE LIKE '%Neurological Disorder%' OR
     adt.REFERENCE LIKE '%No Post Mortem exam%' OR
     adt.REFERENCE LIKE '%Opthalmologic Disorder%' OR
     adt.REFERENCE LIKE '%Reproductive Disorder%' OR
     adt.REFERENCE LIKE '%Autolysis%' OR
     adt.REFERENCE LIKE '%No gross abnormalities%' OR
     adt.REFERENCE LIKE '%General Comment%') AND
        adt.COLUMN_NAME = 'DELETE'
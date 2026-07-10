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
        (adt.REFERENCE LIKE '%Normal observations%' OR
         adt.REFERENCE LIKE '%Trauma%' OR
         adt.REFERENCE LIKE '%Reduced appetite%' OR
         adt.REFERENCE LIKE '%Abnormal stool consistency%' OR
         adt.REFERENCE LIKE '%Dehydration%' OR
         adt.REFERENCE LIKE '%Decreased activity%' OR
         adt.REFERENCE LIKE '%Abnormal body condition%' OR
         adt.REFERENCE LIKE '%Abnormal attitude%' OR
         adt.REFERENCE LIKE '%No appetite%' OR
         adt.REFERENCE LIKE '%No Stool%' OR
         adt.REFERENCE LIKE '%Cage Appendage Entrapment%' OR
         adt.REFERENCE LIKE '%Alopecia Event%' OR
         adt.REFERENCE LIKE '%Behav Assess%') AND
        adt.COLUMN_NAME = 'DELETE'
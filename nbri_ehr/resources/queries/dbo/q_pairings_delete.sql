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
    (adt.REFERENCE LIKE '%Pair Formation%' OR
     adt.REFERENCE LIKE '%Group Formation%' OR
     adt.REFERENCE LIKE '%Pair/Group-Compatible w/ Cagemates%' OR
     adt.REFERENCE LIKE '%Pair/Group-Incompatible w/ Cagemates%' OR
     adt.REFERENCE LIKE '%Pair/Group-Compatibility Undetermined%' OR
     adt.REFERENCE LIKE '%Pair/Group-Dominant%' OR
     adt.REFERENCE LIKE '%Pair/Group-Subordinate%' OR
     adt.REFERENCE LIKE '%Pathological behavior%' OR
     adt.REFERENCE LIKE '%intermittent pathological behavior%' OR
     adt.REFERENCE LIKE '%Full Contact Pair%' OR
     adt.REFERENCE LIKE '%Limited Contact Pair%' OR
     adt.REFERENCE LIKE '%Pair Separation-Clinical%' OR
     adt.REFERENCE LIKE '%Pair Separation-Behavioral%' OR
     adt.REFERENCE LIKE '%Pair Separation-Other%' OR
     adt.REFERENCE LIKE '%Compatible w/ adjacent groups%' OR
     adt.REFERENCE LIKE '%Relocation only%' OR
     adt.REFERENCE LIKE '%Reintroduction%' OR
     adt.REFERENCE LIKE '%Introduction%' ) AND
        adt.COLUMN_NAME = 'DELETE'
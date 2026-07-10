/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

SELECT an.ANIMAL_ID_NUMBER AS participantId,
       an.DEATH_DATE AS deathDate,
       an.TERMINATION_REASON_ID as reason,
       (CASE
            WHEN (ae.STAFF_ID.STAFF_FIRST_NAME IS NULL OR ae.STAFF_ID.STAFF_LAST_NAME IS NULL) THEN 'unknown'
            ELSE (trim(ae.STAFF_ID.STAFF_FIRST_NAME)
                || '|' || trim(ae.STAFF_ID.STAFF_LAST_NAME)) END)                  AS performedby,
       COALESCE(MAX(CAST(adt.CHANGE_DATETIME AS TIMESTAMP)), ae.CREATED_DATETIME) AS modified
FROM Animal an
LEFT JOIN AUDIT_TRAIL adt ON an.ANIMAL_ID = substring(PRIMARY_KEY_VALUES, length('ANIMAL_ID = '))
    AND adt.TABLE_NAME = 'ANIMAL' AND adt.COLUMN_NAME = 'death_date'
LEFT JOIN ANIMAL_EVENT ae ON ae.ANIMAL_ID = an.ANIMAL_ID AND EVENT_ID = 1  -- Received
WHERE an.ANIMAL_DISPOSITION_ID = 4 -- died
AND an.TERMINATION_REASON_ID != 10 -- Invalid Id
GROUP BY an.ANIMAL_ID_NUMBER,
    an.DEATH_DATE,
    an.TERMINATION_REASON_ID,
    ae.STAFF_ID.STAFF_FIRST_NAME,
    ae.STAFF_ID.STAFF_LAST_NAME,
    ae.CREATED_DATETIME
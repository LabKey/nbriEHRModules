/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT alt.ALTERNATE_ID AS "objectId",
       anm.ANIMAL_ID,
       anm.ANIMAL_ID_NUMBER AS "Id",
       CASE WHEN (SUBSTRING(trim(alt.NAME),3,1) = '-' OR SUBSTRING(trim(alt.NAME),5,1) = '-')
            THEN trim(SUBSTRING(trim(alt.NAME),0,11))
            ELSE trim(SUBSTRING(trim(alt.NAME), 0, 8))
            END as "projectName",
       alt.NAME AS "description",
       ae.EVENT_DATETIME AS assignmentDate,  -- Set to arrival/birth then updated by trigger
       CASE WHEN alt.DESCRIPTION IS NULL OR length(trim(alt.DESCRIPTION)) != 11 THEN NULL
       ELSE COALESCE(TO_DATE(alt.DESCRIPTION, 'DD-Mon-RR'), COALESCE(dea.deathDate, dep.eventDate)) END as endDate,
       (CASE
            WHEN (ae.STAFF_ID.STAFF_FIRST_NAME IS NULL OR ae.STAFF_ID.STAFF_LAST_NAME IS NULL) THEN 'unknown'
            ELSE (trim(ae.STAFF_ID.STAFF_FIRST_NAME)
                || '|' || trim(ae.STAFF_ID.STAFF_LAST_NAME)) END)                  AS performedby,
       COALESCE(MAX(CAST(adt.CHANGE_DATETIME AS TIMESTAMP)), ae.CREATED_DATETIME) AS modified
FROM  ALTERNATE alt
LEFT JOIN ANIMAL anm ON alt.ANIMAL_ID = anm.ANIMAL_ID
LEFT JOIN AUDIT_TRAIL adt ON alt.ALTERNATE_ID = substring(PRIMARY_KEY_VALUES, length('Alternate_ID = '))
    AND adt.TABLE_NAME = 'ALTERNATE'
LEFT JOIN ANIMAL_EVENT ae ON ae.ANIMAL_ID = anm.ANIMAL_ID AND EVENT_ID = 1  -- Received
LEFT JOIN q_deaths dea ON anm.ANIMAL_ID_NUMBER = dea.participantId
LEFT JOIN q_finalDeparture dep ON anm.ANIMAL_ID_NUMBER = dep.Id
WHERE alt.ALTERNATE_TYPE_ID = 6 -- Project Inventory Number for project transfers
GROUP BY alt.ALTERNATE_ID,
         anm.ANIMAL_ID,
         anm.ANIMAL_ID_NUMBER,
         alt.NAME,
         alt.DESCRIPTION,
         ae.CREATED_DATETIME,
         ae.EVENT_DATETIME,
         ae.STAFF_ID.STAFF_FIRST_NAME,
         ae.STAFF_ID.STAFF_LAST_NAME,
         dea.deathDate,
         dep.eventDate
ORDER BY anm.ANIMAL_ID,alt.ALTERNATE_ID ASC
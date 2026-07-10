/*
 * Copyright (c) 2023-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT anmEvt.ANIMAL_EVENT_ID AS objectId,
anm.ANIMAL_ID_NUMBER AS Id,
(CASE
     WHEN (anmEvt.STAFF_ID.STAFF_FIRST_NAME IS NULL OR anmEvt.STAFF_ID.STAFF_LAST_NAME IS NULL) THEN 'unknown'
     ELSE (anmEvt.STAFF_ID.STAFF_FIRST_NAME
         || '|' || anmEvt.STAFF_ID.STAFF_LAST_NAME) END) AS performedby,
CAST(anmEvt.CREATED_DATETIME AS TIMESTAMP) AS modified,
CAST(anmEvt.EVENT_DATETIME AS TIMESTAMP) AS eventDate,
aCom.TEXT AS remarks,
anmEvt.EVENT_ID.NAME AS type
FROM ANIMAL_EVENT anmEvt
	LEFT JOIN ANIMAL_EVENT_COMMENT aCom ON aCom.ANIMAL_EVENT_ID = anmEvt.ANIMAL_EVENT_ID
	LEFT JOIN ANIMAL anm ON anmEvt.ANIMAL_ID = anm.ANIMAL_ID
WHERE anmEvt.EVENT_ID IN (1, 3, 159, 160, 883, 1432, 1619, 1731, 1852, 2175, 2221, 2332, 2356)
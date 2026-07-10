/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

SELECT anmEvt.ANIMAL_EVENT_ID AS objectId,
       anm.ANIMAL_ID_NUMBER AS Id,
       CAST(anmEvt.EVENT_DATETIME AS TIMESTAMP) AS transferDate,
       anmCmt.TEXT AS remark,
       cg.location AS cage,
       (CASE
            WHEN (trim(anmEvt.STAFF_ID.STAFF_FIRST_NAME) IS NULL OR trim(anmEvt.STAFF_ID.STAFF_LAST_NAME) IS NULL) THEN 'unknown'
            ELSE (trim(anmEvt.STAFF_ID.STAFF_FIRST_NAME)
                || '|' || trim(anmEvt.STAFF_ID.STAFF_LAST_NAME)) END)                  AS performedby,
       CAST(COALESCE(adt.modified, anmEvt.CREATED_DATETIME) AS TIMESTAMP)           AS modified
FROM ANIMAL_EVENT anmEvt
LEFT JOIN ANIMAL anm ON anmEvt.ANIMAL_ID = anm.ANIMAL_ID
LEFT JOIN ANIMAL_EVENT_COMMENT anmCmt ON anmEvt.ANIMAL_EVENT_ID = anmCmt.ANIMAL_EVENT_ID
LEFT JOIN q_cages cg ON cg.location = anmEvt.LOCATION_ID
LEFT JOIN q_modified_event adt ON anmEvt.ANIMAL_EVENT_ID = adt.event_id
WHERE anmEvt.EVENT_ID = 2 --Housing Transfer
AND anmCmt.TEXT LIKE '%Loc%'
ORDER BY anm.ANIMAL_ID_NUMBER,anmEvt.ANIMAL_EVENT_ID DESC
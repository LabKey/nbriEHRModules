/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT anmEvt.ANIMAL_EVENT_ID                                                    AS objectid,
       anmEvt.ANIMAL_ID.ANIMAL_ID_NUMBER                                         AS Id,
       CAST(anmEvt.EVENT_DATETIME AS TIMESTAMP)                                  AS bloodDate,
       (CASE
            WHEN (anmEvt.STAFF_ID.STAFF_FIRST_NAME IS NULL OR anmEvt.STAFF_ID.STAFF_LAST_NAME IS NULL) THEN 'unknown'
            ELSE (trim(anmEvt.STAFF_ID.STAFF_FIRST_NAME)
                || '|' || trim(anmEvt.STAFF_ID.STAFF_LAST_NAME)) END)                  AS performedby,
       anmEvt.EVENT_ID.NAME                                                      AS type,
       anmEvt.RESULT                                                             AS quantity,
       ev.NUMERIC_UNIT_ID                                                        AS Units,
       (CASE WHEN (anmEvt.DIAGNOSIS IS NOT NULL AND anmCmt.TEXT IS NOT NULL) THEN (anmEvt.DIAGNOSIS || ', ' || anmCmt.TEXT) WHEN (anmEvt.DIAGNOSIS IS NOT NULL AND anmCmt.TEXT IS NULL) THEN (anmEvt.DIAGNOSIS) ELSE (anmCmt.TEXT) END) AS remark,
       CAST(COALESCE(adt.modified, anmEvt.CREATED_DATETIME) AS TIMESTAMP) AS modified
FROM ANIMAL_EVENT anmEvt
         LEFT JOIN ANIMAL_EVENT_COMMENT anmCmt ON anmEvt.ANIMAL_EVENT_ID = anmCmt.ANIMAL_EVENT_ID
         LEFT JOIN q_modified_event adt ON anmEvt.ANIMAL_EVENT_ID = adt.event_id
         LEFT JOIN EVENT ev ON anmEvt.EVENT_ID = ev.EVENT_ID

WHERE anmEvt.EVENT_ID.EVENT_ID IN (1864, 1581, 2129, 219)
  AND anmEvt.CREATED_DATETIME < now() -- there are rows in ANIMAL_EVENT table with future dates
--     1864 Blood Sample Collection (with Volume)
--     1581 Blood Sample Collection
--     2129 Blood-Predose
--     219 SRV Draw
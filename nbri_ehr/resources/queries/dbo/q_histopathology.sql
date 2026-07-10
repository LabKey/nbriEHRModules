/*
 * Copyright (c) 2023-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT anmEvt.ANIMAL_EVENT_ID as objectid,
       anm.ANIMAL_ID_NUMBER AS Id,
       CAST(anmEvt.EVENT_DATETIME AS TIMESTAMP) AS eventDate,
       (CAST(COALESCE (adt.modified, anmEvt.CREATED_DATETIME) AS TIMESTAMP)) AS modified,
       (CASE
            WHEN (anmEvt.STAFF_ID.STAFF_FIRST_NAME IS NULL OR anmEvt.STAFF_ID.STAFF_LAST_NAME IS NULL) THEN 'unknown'
            ELSE (anmEvt.STAFF_ID.STAFF_FIRST_NAME
                || '|' || anmEvt.STAFF_ID.STAFF_LAST_NAME) END)                  AS performedby,
       anmCmt.TEXT AS remark,
       anmEvt.DIAGNOSIS as diagnosis,
       anmEvt.EVENT_ID.NAME AS category,
       CASE WHEN anmEvt.ATTACHMENT_PATH IS NOT NULL THEN
            ('C:\Program Files\Labkey\labkey\files\NBRI\EHR\@files\attachments'
                || substring(anmEvt.ATTACHMENT_PATH, LENGTH('N:\'), LENGTH(anmEvt.ATTACHMENT_PATH)))
       ELSE NULL END AS attachmentFile
FROM ANIMAL_EVENT anmEvt
         LEFT JOIN ANIMAL anm ON anmEvt.ANIMAL_ID = anm.ANIMAL_ID
         LEFT JOIN ANIMAL_EVENT_COMMENT anmCmt ON anmEvt.ANIMAL_EVENT_ID = anmCmt.ANIMAL_EVENT_ID
         LEFT JOIN q_modified_event adt ON anmEvt.ANIMAL_EVENT_ID = adt.event_id
WHERE anmEvt.EVENT_ID = 2236 -- 2236 Histopathology Report
  AND anmEvt.CREATED_DATETIME < now() -- there are rows in ANIMAL_EVENT table with future dates

/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT anmEvt.ANIMAL_EVENT_ID                                                    AS objectid,
       anmEvt.ANIMAL_ID.ANIMAL_ID_NUMBER                                         AS Id,
       CAST(anmEvt.EVENT_DATETIME AS TIMESTAMP)                                  AS administrationDate,
       (CASE
            WHEN (trim(anmEvt.STAFF_ID.STAFF_FIRST_NAME) IS NULL OR trim(anmEvt.STAFF_ID.STAFF_LAST_NAME) IS NULL) THEN 'unknown'
            ELSE (trim(anmEvt.STAFF_ID.STAFF_FIRST_NAME)
                || '|' || trim(anmEvt.STAFF_ID.STAFF_LAST_NAME)) END)                  AS performedby,
       ('Note: ' || COALESCE(anmCmt.TEXT, 'None') || '; Diagnosis: ' || COALESCE(anmEvt.DIAGNOSIS, 'None')) AS remark,
       'Clinical'                                                                AS category,
       CASE WHEN anmEvt.ATTACHMENT_PATH IS NOT NULL THEN
            ('C:\Program Files\Labkey\labkey\files\NBRI\EHR\@files\attachments'
                || substring(anmEvt.ATTACHMENT_PATH, LENGTH('N:\'), LENGTH(anmEvt.ATTACHMENT_PATH)))
       ELSE NULL END AS attachmentFile,
       CAST(COALESCE(adt.modified, anmEvt.CREATED_DATETIME) AS TIMESTAMP)        AS modified
FROM ANIMAL_EVENT anmEvt
         LEFT JOIN ANIMAL_EVENT_COMMENT anmCmt ON anmEvt.ANIMAL_EVENT_ID = anmCmt.ANIMAL_EVENT_ID
         LEFT JOIN EVENT_EVENT_GROUP evtEvtGrp ON evtEvtGrp.EVENT_ID = anmEvt.EVENT_ID
         LEFT JOIN q_modified_event adt ON anmEvt.ANIMAL_EVENT_ID = adt.event_id

WHERE evtEvtGrp.EVENT_GROUP_ID = 53
  AND anmEvt.CREATED_DATETIME < now() -- there are rows in ANIMAL_EVENT table with future dates
--     53 Comments
/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT anmEvt.ANIMAL_EVENT_ID   AS objectid,
       anm.ANIMAL_ID_NUMBER  AS Id,
       (CASE
            WHEN (anmEvt.STAFF_ID.STAFF_FIRST_NAME IS NULL OR anmEvt.STAFF_ID.STAFF_LAST_NAME IS NULL) THEN 'unknown'
            ELSE (trim(anmEvt.STAFF_ID.STAFF_FIRST_NAME)
                || '|' || trim(anmEvt.STAFF_ID.STAFF_LAST_NAME)) END)                  AS performedby,
       CAST(COALESCE(adt.modified, anmEvt.CREATED_DATETIME) AS TIMESTAMP) AS modified,
       CAST(anmEvt.EVENT_DATETIME AS TIMESTAMP) AS eventDate,
       NULL                  AS acqDateText,
       'Animal Event'        AS source,
       substring(Evt.NAME, length('Lab Transfer Fr ')) AS sourceFacility,
       CASE WHEN anmEvt.ATTACHMENT_PATH IS NOT NULL THEN
                   ('C:\Program Files\Labkey\labkey\files\NBRI\EHR\@files\attachments'
                       || substring(anmEvt.ATTACHMENT_PATH, LENGTH('N:\'), LENGTH(anmEvt.ATTACHMENT_PATH)))
              ELSE NULL END AS attachmentFile
FROM ANIMAL_EVENT anmEvt
         LEFT JOIN ANIMAL anm ON anmEvt.ANIMAL_ID = anm.ANIMAL_ID
         LEFT JOIN q_modified_event adt ON anmEvt.ANIMAL_EVENT_ID = adt.event_id
         LEFT JOIN EVENT Evt ON anmEvt.EVENT_ID = Evt.EVENT_ID
WHERE anmEvt.EVENT_ID IN (SELECT EVENT_ID FROM EVENT WHERE NAME LIKE 'Lab Transfer Fr%')
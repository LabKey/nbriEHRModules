/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT anmEvt.ANIMAL_EVENT_ID as objectid,
       anm.ANIMAL_ID_NUMBER AS Id,
       CAST(anmEvt.EVENT_DATETIME AS TIMESTAMP) AS physicalExamDate,
       CAST(COALESCE (adt.modified, anmEvt.CREATED_DATETIME) AS TIMESTAMP) AS modified,
       (CASE
            WHEN (trim(anmEvt.STAFF_ID.STAFF_FIRST_NAME) IS NULL OR trim(anmEvt.STAFF_ID.STAFF_LAST_NAME) IS NULL) THEN 'unknown'
            ELSE (trim(anmEvt.STAFF_ID.STAFF_FIRST_NAME)
                || '|' || trim(anmEvt.STAFF_ID.STAFF_LAST_NAME)) END)                  AS performedby,
       (CASE WHEN (anmEvt.DIAGNOSIS IS NOT NULL AND anmCmt.TEXT IS NOT NULL) THEN (anmEvt.DIAGNOSIS || ', ' || anmCmt.TEXT) WHEN (anmEvt.DIAGNOSIS IS NOT NULL AND anmCmt.TEXT IS NULL) THEN (anmEvt.DIAGNOSIS) ELSE (anmCmt.TEXT) END) AS remark,
       anmEvt.EVENT_ID.NAME AS exam,
       anmEvt.RESULT,
       ev.NUMERIC_UNIT_ID                                                        AS Units,
       CASE WHEN anmEvt.ATTACHMENT_PATH IS NOT NULL THEN
           ('C:\Program Files\Labkey\labkey\files\NBRI\EHR\@files\attachments'
                    || substring(anmEvt.ATTACHMENT_PATH, LENGTH('N:\'), LENGTH(anmEvt.ATTACHMENT_PATH)))
       ELSE NULL END AS attachmentFile
FROM ANIMAL_EVENT anmEvt
         LEFT JOIN ANIMAL anm ON anmEvt.ANIMAL_ID = anm.ANIMAL_ID
         LEFT JOIN ANIMAL_EVENT_COMMENT anmCmt ON anmEvt.ANIMAL_EVENT_ID = anmCmt.ANIMAL_EVENT_ID
         LEFT JOIN EVENT_EVENT_GROUP evtEvtGrp ON evtEvtGrp.EVENT_ID = anmEvt.EVENT_ID
         LEFT JOIN q_modified_event adt ON anmEvt.ANIMAL_EVENT_ID = adt.event_id
         LEFT JOIN EVENT ev on anmEvt.EVENT_ID = ev.EVENT_ID
WHERE evtEvtGrp.EVENT_GROUP_ID = 60 -- Physical Exam
  -- filter out vital signs events
  AND evtEvtGrp.EVENT_ID NOT IN (1349, 2165, 2166, 2167, 2168, 2169, 55, 56, 1643, 1647, 1652, 1653)
  -- 1349 Body Weight
  -- 2165 Blood Pressure-NHP
  -- 2166 Heart Rate-NHP
  -- 2167 Respiration Rate-NHP
  -- 2168 Temperature-NHP
  -- 2169 Pulse Rate-NHP
  -- 55 Blood Pressure
  -- 1652 Heart Rate
  -- 56, 1647 Respiration Rate
  -- 1643 Temperature
  -- 1653 Pulse
  AND anmEvt.CREATED_DATETIME < now() -- there are rows in ANIMAL_EVENT table with future dates
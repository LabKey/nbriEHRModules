/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT anmEvt.ANIMAL_EVENT_ID                                                    AS objectid,
       anmEvt.ANIMAL_ID.ANIMAL_ID_NUMBER                                         AS Id,
       CAST(anmEvt.EVENT_DATETIME AS TIMESTAMP)                                  AS biopsyDate,
       (CASE
            WHEN (trim(anmEvt.STAFF_ID.STAFF_FIRST_NAME) IS NULL OR trim(anmEvt.STAFF_ID.STAFF_LAST_NAME) IS NULL) THEN 'unknown'
            ELSE (trim(anmEvt.STAFF_ID.STAFF_FIRST_NAME)
                || '|' || trim(anmEvt.STAFF_ID.STAFF_LAST_NAME)) END)                  AS performedby,
       anmEvt.EVENT_ID.NAME                                                      AS type,
       CASE WHEN anmEvt.ATTACHMENT_PATH IS NOT NULL THEN
                ('C:\Program Files\Labkey\labkey\files\NBRI\EHR\@files\attachments'
                    || substring(anmEvt.ATTACHMENT_PATH, LENGTH('N:\'), LENGTH(anmEvt.ATTACHMENT_PATH)))
            ELSE NULL END AS attachmentFile,
       anmEvt.DIAGNOSIS                                                          AS description,
       anmCmt.TEXT                                                               AS remark,
       CAST(COALESCE(adt.modified, anmEvt.CREATED_DATETIME) AS TIMESTAMP) AS modified
FROM ANIMAL_EVENT anmEvt
         LEFT JOIN ANIMAL_EVENT_COMMENT anmCmt ON anmEvt.ANIMAL_EVENT_ID = anmCmt.ANIMAL_EVENT_ID
         LEFT JOIN q_modified_event adt ON anmEvt.ANIMAL_EVENT_ID = adt.event_id

WHERE anmEvt.EVENT_ID.EVENT_ID IN (
                                  1386,  -- Hepatic Biopsy-Tru Cut
                                  1388,  -- Liver precutaneous Biopsy
                                  1389,  -- Skin Biopsy
                                  1390,  -- Hepatic Biopsy-Menghini Technique
                                  1391,  -- Lymph node Biopsy
                                  1392,  -- Skeletal Muscle Biopsy
                                  1393,  -- Lung Biopsy
                                  1394,  -- White fat Biopsy
                                  1395,  -- Renal Biopsy
                                  1396,  -- Brown Fat Biopsy
                                  1397,  -- Gastric Biopsy
                                  1398,  -- Intestinal Biopsy
                                  1399,  -- Cervical Biopsy
                                  1400,  -- Rectal Biopsy
                                  1401,  -- Uterine Biopsy
                                  1402,  -- Vaginal Biopsy
                                  1403,  -- Testicular Biopsy
                                  1404,  -- Bone marrow Biopsy
                                  1603,  -- Biopsy Collection
                                  1641,  -- Biopsy Collection - Liver (punch)
                                  1642   -- Biopsy Collection - Lymph node
                                  )
  AND anmEvt.CREATED_DATETIME < now() -- there are rows in ANIMAL_EVENT table with future dates

/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT anmEvt.ANIMAL_EVENT_ID as objectid,
       anm.ANIMAL_ID_NUMBER AS Id,
       CAST(anmEvt.EVENT_DATETIME AS TIMESTAMP) AS prcDate,
       CAST(COALESCE (adt.modified, anmEvt.CREATED_DATETIME) AS TIMESTAMP) AS modified,
       (CASE WHEN (anmEvt.DIAGNOSIS IS NOT NULL AND anmCmt.TEXT IS NOT NULL) THEN (anmEvt.DIAGNOSIS || ', ' || anmCmt.TEXT) WHEN (anmEvt.DIAGNOSIS IS NOT NULL AND anmCmt.TEXT IS NULL) THEN (anmEvt.DIAGNOSIS) ELSE (anmCmt.TEXT) END) AS remark,
       (CASE
            WHEN (anmEvt.STAFF_ID.STAFF_FIRST_NAME IS NULL OR anmEvt.STAFF_ID.STAFF_LAST_NAME IS NULL) THEN 'unknown'
            ELSE (anmEvt.STAFF_ID.STAFF_FIRST_NAME
                || '|' || anmEvt.STAFF_ID.STAFF_LAST_NAME) END)                  AS performedby,
       anmEvt.EVENT_ID.NAME AS type
FROM ANIMAL_EVENT anmEvt
         LEFT JOIN ANIMAL anm ON anmEvt.ANIMAL_ID = anm.ANIMAL_ID
         LEFT JOIN ANIMAL_EVENT_COMMENT anmCmt ON anmEvt.ANIMAL_EVENT_ID = anmCmt.ANIMAL_EVENT_ID
         LEFT JOIN EVENT_EVENT_GROUP evtEvtGrp ON evtEvtGrp.EVENT_ID = anmEvt.EVENT_ID
         LEFT JOIN q_modified_event adt ON anmEvt.ANIMAL_EVENT_ID = adt.event_id
WHERE evtEvtGrp.EVENT_GROUP_ID IN (6,10,27,50,52)
   -- 6 Diagnostic Imaging
   -- 10 Wound Care
   -- 27 Dentistry Procedures
   -- 28 Surgical Procedures Major
   -- 29 Surgical Procedures Minor
   -- 35 Diagnostic Tissue Collection
   -- 50 Medical Treatment
   -- 52  Protocol Associated Events

OR -- Procedures from Diagnostic sample collection event group
(evtEvtGrp.EVENT_GROUP_ID = 36 AND (evtEvtGrp.EVENT_ID != 1581 AND evtEvtGrp.EVENT_ID != 1864))

OR -- Procedures from Timed sample collection event group minus blood draws and biopsies
    (evtEvtGrp.EVENT_GROUP_ID = 32 AND (evtEvtGrp.EVENT_ID != 1581 AND  evtEvtGrp.EVENT_ID != 1864 AND evtEvtGrp.EVENT_ID != 1641 AND  evtEvtGrp.EVENT_ID != 1642))

OR -- Procedures from Diagnostic Procedures minus Biopsies
    (evtEvtGrp.EVENT_GROUP_ID = 21 AND evtEvtGrp.EVENT_ID.Name NOT LIKE '%Biops%')

OR -- Diagnostic Tissue Collection minus Biopsies
    (evtEvtGrp.EVENT_GROUP_ID = 35 AND evtEvtGrp.EVENT_ID != 1603)

OR -- Surgical Procedures minus Biopsies
    (evtEvtGrp.EVENT_GROUP_ID = 29 AND (evtEvtGrp.EVENT_ID != 1641 AND evtEvtGrp.EVENT_ID != 1642))

OR -- Surgical Procedures minus Biopsies
    (evtEvtGrp.EVENT_GROUP_ID = 28 AND evtEvtGrp.EVENT_ID != 1669)

OR -- Procedures from Quarantine Period
    (evtEvtGrp.EVENT_GROUP_ID = 26 AND evtEvtGrp.EVENT_ID = 1806)

AND anmEvt.CREATED_DATETIME < now() -- there are rows in ANIMAL_EVENT table with future dates
GROUP BY anmEvt.ANIMAL_EVENT_ID, anm.ANIMAL_ID_NUMBER, anmEvt.EVENT_DATETIME, adt.modified, anmEvt.CREATED_DATETIME,
         anmEvt.DIAGNOSIS, anmCmt.TEXT, anmEvt.EVENT_ID.NAME, anmEvt.STAFF_ID.STAFF_FIRST_NAME, anmEvt.STAFF_ID.STAFF_LAST_NAME
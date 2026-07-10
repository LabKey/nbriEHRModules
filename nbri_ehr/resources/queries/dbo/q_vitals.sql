/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT anmEvt.ANIMAL_EVENT_ID as objectid,
       anm.ANIMAL_ID_NUMBER AS Id,
       CAST(anmEvt.EVENT_DATETIME AS TIMESTAMP) AS vitalDate,
       (CASE
            WHEN (anmEvt.STAFF_ID.STAFF_FIRST_NAME IS NULL OR anmEvt.STAFF_ID.STAFF_LAST_NAME IS NULL) THEN 'unknown'
            ELSE (anmEvt.STAFF_ID.STAFF_FIRST_NAME
                || '|' || anmEvt.STAFF_ID.STAFF_LAST_NAME) END)                  AS performedby,
       CAST(COALESCE (adt.modified, anmEvt.CREATED_DATETIME) AS TIMESTAMP) AS modified,
       CASE WHEN anmEvt.EVENT_ID.NAME LIKE '%Blood Pressure%' THEN anmEvt.TEXT_RESULT END AS bloodPressure,
       CASE WHEN anmEvt.EVENT_ID.NAME LIKE '%Heart Rate%' THEN anmEvt.RESULT END AS heartRate,
       CASE WHEN anmEvt.EVENT_ID.NAME LIKE '%Respiration Rate%' THEN anmEvt.RESULT END AS respRate,
       CASE WHEN anmEvt.EVENT_ID.NAME LIKE '%Temperature%' THEN anmEvt.RESULT END AS temp,
       CASE WHEN anmEvt.EVENT_ID.NAME LIKE '%Pulse Rate%' THEN anmEvt.RESULT END AS pulseRate,
       CASE WHEN anmEvt.EVENT_ID.NAME LIKE '%Pulse Oximetry%' THEN anmEvt.RESULT END AS pulseOximetry,
       CASE WHEN anmEvt.EVENT_ID.NAME LIKE '%EKG%' THEN anmEvt.RESULT END AS ekg,
       ev.NUMERIC_UNIT_ID AS Units,
       anmEvt.EVENT_ID.NAME AS type
FROM ANIMAL_EVENT anmEvt
         LEFT JOIN ANIMAL anm ON anmEvt.ANIMAL_ID = anm.ANIMAL_ID
         LEFT JOIN ANIMAL_EVENT_COMMENT anmCmt ON anmEvt.ANIMAL_EVENT_ID = anmCmt.ANIMAL_EVENT_ID
         LEFT JOIN q_modified_event adt ON anmEvt.ANIMAL_EVENT_ID = adt.event_id
         LEFT JOIN EVENT ev on anmEvt.EVENT_ID = ev.EVENT_ID
WHERE anmEvt.EVENT_ID.EVENT_ID IN (55, 56, 57, 1643, 1647, 1652, 1653, 1654, 1655, 2165, 2166, 2167, 2168, 2169)
  AND anmEvt.CREATED_DATETIME < now() -- there are rows in ANIMAL_EVENT table with future dates
        -- 55 Blood Pressure
        -- 57 EKG
        -- 1643 Temperature
        -- 56, 1647 Respiration Rate
        -- 1652 Heart Rate
        -- 1653 Pulse
        -- 1654 Pulse Rate
        -- 1655 Pulse Oximetry
        -- 2165 Blood Pressure-NHP
        -- 2166 Heart Rate-NHP
        -- 2167 Respiration Rate-NHP
        -- 2168 Temperature-NHP
        -- 2169 Pulse Rate-NHP

/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT * FROM
(
    SELECT
        anmEvt.ANIMAL_EVENT_ID                                   AS "objectId",
        anmEvt.ANIMAL_EVENT_ID                                   AS "animalEventId",
        anm.ANIMAL_ID_NUMBER                                     AS Id,
        CAST(anmEvt.EVENT_DATETIME AS TIMESTAMP)                 AS transferDate,
        COALESCE(dea.deathDate, dep.eventDate)                   AS enddate,
        (CASE
            WHEN (anmEvt.STAFF_ID.STAFF_FIRST_NAME IS NULL OR anmEvt.STAFF_ID.STAFF_LAST_NAME IS NULL)
                THEN 'unknown'
            ELSE (trim(anmEvt.STAFF_ID.STAFF_FIRST_NAME)
                || '|' || trim(anmEvt.STAFF_ID.STAFF_LAST_NAME)) END) AS performedby,
        REPLACE(anmCmt.TEXT, ';', ':')                           AS remark,
        anmEvt.LOCATION_ID                                       AS location
  FROM ANIMAL_EVENT anmEvt
       LEFT JOIN ANIMAL anm ON anmEvt.ANIMAL_ID = anm.ANIMAL_ID
       LEFT JOIN ANIMAL_EVENT_COMMENT anmCmt ON anmEvt.ANIMAL_EVENT_ID = anmCmt.ANIMAL_EVENT_ID
       LEFT JOIN q_deaths dea ON anm.ANIMAL_ID_NUMBER = dea.participantId
       LEFT JOIN q_finalDeparture dep ON anm.ANIMAL_ID_NUMBER = dep.Id
  WHERE anmEvt.EVENT_ID = 2 --Animal Transfer
    AND anmCmt.TEXT LIKE '%Pro%'

  UNION

  SELECT
      ae.ANIMAL_EVENT_ID                                   as objectId,
      ae.ANIMAL_EVENT_ID                                   as animalEventId,
      an.ANIMAL_ID_NUMBER                    as Id,
      CAST(ae.EVENT_DATETIME AS TIMESTAMP)                      as transferDate,
      COALESCE(dea.deathDate, dep.eventDate) AS enddate,
      (CASE
           WHEN (ae.STAFF_ID.STAFF_FIRST_NAME IS NULL OR ae.STAFF_ID.STAFF_LAST_NAME IS NULL)
               THEN 'unknown'
           ELSE (ae.STAFF_ID.STAFF_FIRST_NAME
               || '|' || ae.STAFF_ID.STAFF_LAST_NAME) END) AS performedby,
      'From Protocol: None: To Protocol: ' || pr.PROTOCOL_NUMBER            as remark,
      ae.LOCATION_ID                                   as location
  FROM ANIMAL an
       JOIN CAGE_CARD cc ON an.CAGE_CARD_ID = cc.CAGE_CARD_ID
       JOIN SEGMENT se ON cc.SEGMENT_ID = se.SEGMENT_ID
       JOIN PROTOCOL pr ON se.PROTOCOL_ID = pr.PROTOCOL_ID
       LEFT JOIN ANIMAL_EVENT ae ON ae.ANIMAL_ID = an.ANIMAL_ID AND EVENT_ID = 1 -- Received
       LEFT JOIN q_deaths dea ON an.ANIMAL_ID_NUMBER = dea.participantId
       LEFT JOIN q_finalDeparture dep ON an.ANIMAL_ID_NUMBER = dep.Id
) a
ORDER BY a.Id,a.transferDate DESC
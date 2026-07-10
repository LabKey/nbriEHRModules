/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    cc.CAGE_CARD_ID         AS CageCardId,
    s.PROTOCOL_ID           AS Protocol,
    s.SPECIES_ID            AS Species,
    cc.CARD_FORMAT_ID       AS CardFormat,
    cc.COST_CENTER_ID       AS CostCenter,
    cc.GENERATION_DATETIME  AS GenerationDate,
    cc.UPDATE_DATETIME      AS UpdateDate,
    cc.NUMBER_OF_ANIMALS    AS NumberOfAnimals,
    cg.location                    as cage,
    COALESCE(rm.room, cg.room)     as room,
    COALESCE(fl.floor, cg.floor, rm.floor)                      as floor,
    COALESCE(bu.Name, cg.building, rm.building, fl.building)    as building,
    COALESCE(ar.area, cg.area, rm.area, fl.area, bu.area)       as area,
    cc.STAFF_ACCOUNT_ACCOUNT_ID     AS Account,
    (CASE
         WHEN (cc.STAFF_ACCOUNT_STAFF_ID.STAFF_FIRST_NAME IS NULL OR cc.STAFF_ACCOUNT_STAFF_ID.STAFF_LAST_NAME IS NULL) THEN 'unknown'
         ELSE (trim(cc.STAFF_ACCOUNT_STAFF_ID.STAFF_FIRST_NAME)
             || '|' || trim(cc.STAFF_ACCOUNT_STAFF_ID.STAFF_LAST_NAME)) END)          AS AccountStaff,
    cc.CENSUS_ACTIVITY_STATUS_ID    AS CensusActivityStatus,
    cc.CENSUS_ACTIVITY_DATE         AS CensusActivityDate,
    cc.ANIMAL_DELIVERY_ID           AS AnimalDelivery,
    (CASE
         WHEN (cc.ANIMAL_REQUESTED_BY_STAFF_ID.STAFF_FIRST_NAME IS NULL OR cc.ANIMAL_REQUESTED_BY_STAFF_ID.STAFF_LAST_NAME IS NULL) THEN 'unknown'
         ELSE (trim(cc.ANIMAL_REQUESTED_BY_STAFF_ID.STAFF_FIRST_NAME)
             || '|' || trim(cc.ANIMAL_REQUESTED_BY_STAFF_ID.STAFF_LAST_NAME)) END)    AS AnimalRequestedByStaff
FROM CAGE_CARD cc
LEFT JOIN SEGMENT s ON cc.SEGMENT_ID = s.SEGMENT_ID
LEFT JOIN q_cages cg ON cg.location = cc.LOCATION_ID
LEFT JOIN q_rooms rm ON rm.room = cc.LOCATION_ID
LEFT JOIN q_floors fl ON fl.floor = cc.LOCATION_ID
LEFT JOIN q_buildings bu ON bu.name = cc.LOCATION_ID
LEFT JOIN q_areas ar ON ar.area = cc.LOCATION_ID
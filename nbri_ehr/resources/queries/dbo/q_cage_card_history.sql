/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    cc.CAGE_CARD_ID            AS CageCardId,
    cc.ANIMAL_DELIVERY_ID      AS AnimalDelivery,
    s.PROTOCOL_ID               AS Protocol,
    s.SPECIES_ID                AS Species,
    cc.CARD_FORMAT_ID           AS CardFormat,
    cc.COST_CENTER_ID           AS CostCenter,
    cc.GENERATION_DATETIME      AS GenerationDate,
    cc.UPDATE_DATETIME          AS UpdateDate,
    cc.NUMBER_OF_ANIMALS        AS NumberOfAnimals,
    cg.location                    as cage,
    COALESCE(rm.room, cg.room)     as room,
    COALESCE(fl.floor, cg.floor, rm.floor)                      as floor,
    COALESCE(bu.Name, cg.building, rm.building, fl.building)    as building,
    COALESCE(ar.area, cg.area, rm.area, fl.area, bu.area)       as area,
    cc.STAFF_ACCOUNT_ACCOUNT_ID     AS Account,
    (CASE
         WHEN (ast.STAFF_FIRST_NAME IS NULL OR ast.STAFF_LAST_NAME IS NULL) THEN 'unknown'
         ELSE (ast.STAFF_FIRST_NAME
             || '|' || ast.STAFF_LAST_NAME) END)          AS AccountStaff,
    cc.CENSUS_ACTIVITY_STATUS_ID    AS CensusActivityStatus,
    cc.CENSUS_ACTIVITY_DATE         AS CensusActivityDate,
    cc.CENSUS_START_DATE            AS CensusStartDate,
    cc.CENSUS_STOP_DATE             AS CensusStopDate,
    cc.TRANSFER_DATE                AS TransferDate,
    cc.ACTION_DATE                  AS ActionDate,
    cc.RETROSPECTIVE_STRESS_ID      AS RetrospectiveStress,
    (CASE
         WHEN (rst.STAFF_FIRST_NAME IS NULL OR rst.STAFF_LAST_NAME IS NULL) THEN 'unknown'
         ELSE (rst.STAFF_FIRST_NAME
             || '|' || rst.STAFF_LAST_NAME) END)    AS AnimalRequestedByStaff

FROM CAGE_CARD_HISTORY cc
    LEFT JOIN SEGMENT s ON s.SEGMENT_ID = cc.SEGMENT_ID
    LEFT JOIN STAFF ast ON ast.STAFF_ID = cc.STAFF_ACCOUNT_STAFF_ID
    LEFT JOIN STAFF rst ON rst.STAFF_ID = cc.ANIMAL_REQUESTED_BY_STAFF_ID
    LEFT JOIN q_cages cg ON cg.location = cc.LOCATION_ID
    LEFT JOIN q_rooms rm ON rm.room = cc.LOCATION_ID
    LEFT JOIN q_floors fl ON fl.floor = cc.LOCATION_ID
    LEFT JOIN q_buildings bu ON bu.name = cc.LOCATION_ID
    LEFT JOIN q_areas ar ON ar.area = cc.LOCATION_ID


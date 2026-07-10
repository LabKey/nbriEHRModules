/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    ad.ANIMAL_DELIVERY_ID          as AnimalDeliveryId,
    ad.ANIMAL_SHIPMENT_ID          as AnimalShipment,
    ad.SHIPTO_ID                   as ShipTo,
    cg.location                    as cage,
    COALESCE(rm.room, cg.room)     as room,
    COALESCE(fl.floor, cg.floor, rm.floor)                      as floor,
    COALESCE(bu.Name, cg.building, rm.building, fl.building)    as building,
    COALESCE(ar.area, cg.area, rm.area, fl.area, bu.area)       as area,
    ad.ANIMAL_REQ_ORDER_ID         as AnimalReqOrder,
    ad.DELIVERY_STATE_ID           as DeliveryState,
    ad.DELIVERY_NUMBER             as DeliveryNumber,
    ad.EXPECTED_DATE               as ExpectedDate,
    ad.PROJECT_CODE_ID             as Project,
    ad.BILL_TO_ACCOUNT_ID          as BillToAccount,
    ad.BILL_TO_STAFF_ID.STAFF_FIRST_NAME
        || '|' || ad.BILL_TO_STAFF_ID.STAFF_LAST_NAME as BillToStaff,
    ad.PER_DIEM_ACCOUNT_ID         as PerDiemAccount,
    ad.PER_DIEM_STAFF_ID.STAFF_FIRST_NAME
        || '|' || ad.PER_DIEM_STAFF_ID.STAFF_LAST_NAME as PerDiemStaff,
FROM ANIMAL_DELIVERY ad
    LEFT JOIN q_cages cg ON cg.location = ad.LOCATION_ID
    LEFT JOIN q_rooms rm ON rm.room = ad.LOCATION_ID
    LEFT JOIN q_floors fl ON fl.floor = ad.LOCATION_ID
    LEFT JOIN q_buildings bu ON bu.name = ad.LOCATION_ID
    LEFT JOIN q_areas ar ON ar.area = ad.LOCATION_ID
/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    ar.ANIMAL_REQ_ORDER_ID             as AnimalReqOrderId,
    ar.ANIMAL_VENDOR_ID                as AnimalVendor,
    ar.REQUISITIONER_STAFF_ID.STAFF_FIRST_NAME
        || '|' || ar.REQUISITIONER_STAFF_ID.STAFF_LAST_NAME as RequisitionerStaff,
    ar.REQUEST_NUMBER                  as RequestNumber,
    ar.REQUISITION_NUMBER              as RequisitionNumber,
    ar.BILL_YN                         as Bill,
    ar.SHIPPING_CONTACT                as ShippingContact,
    ar.CREATE_DATE                     as CreateDate,
    ar.REQ_ORDER_TYPE_ID               as ReqOrderType,
    ar.REQ_ORDER_STATE_ID              as ReqOrderState,
    ar.BILL_TO_ACCOUNT_ID              as BillToAccount,
    ar.BILL_TO_STAFF_ID.STAFF_FIRST_NAME
        || '|' || ar.BILL_TO_STAFF_ID.STAFF_LAST_NAME as BillToStaff,
    ar.PER_DIEM_ACCOUNT_ID             as PerDiemAccount,
    ar.PER_DIEM_STAFF_ID.STAFF_FIRST_NAME
        || '|' || ar.PER_DIEM_STAFF_ID.STAFF_LAST_NAME as PerDiemStaff,
    ar.SUBMITTED_BY_STAFF_ID.STAFF_FIRST_NAME
        || '|' || ar.SUBMITTED_BY_STAFF_ID.STAFF_LAST_NAME as SubmittedByStaff,
    ar.APPROVED_BY_STAFF_ID.STAFF_FIRST_NAME
        || '|' || ar.APPROVED_BY_STAFF_ID.STAFF_LAST_NAME as ApprovedByStaff,
    ar.SUBMITTED_DATE                  as SubmittedDate,
    ar.APPROVED_DATE                   as ApprovedDate,
    s.SPECIES_ID                       as Species,
    s.PROTOCOL_ID                      as Protocol,
    ar.PROJECT_CODE_ID                 as Project,
    cg.location                        as SiteCage,
    COALESCE(rm.room, cg.room)         as SiteRoom,
    COALESCE(fl.floor, cg.floor, rm.floor)                      as SiteFloor,
    COALESCE(bu.Name, cg.building, rm.building, fl.building)    as SiteBuilding,
    COALESCE(area.area, cg.area, rm.area, fl.area, bu.area)       as SiteArea,
    ar.CREATED_BY_STAFF_ID.STAFF_FIRST_NAME
        || '|' || ar.CREATED_BY_STAFF_ID.STAFF_LAST_NAME as CreatedByStaff
FROM ANIMAL_REQ_ORDER ar
    LEFT JOIN q_cages cg ON cg.location = ar.SITE_LOCATION_ID
    LEFT JOIN q_rooms rm ON rm.room = ar.SITE_LOCATION_ID
    LEFT JOIN q_floors fl ON fl.floor = ar.SITE_LOCATION_ID
    LEFT JOIN q_areas area ON area.area = ar.SITE_LOCATION_ID
    LEFT JOIN q_buildings bu ON bu.name = ar.SITE_LOCATION_ID
    LEFT JOIN SEGMENT s ON ar.SEGMENT_ID = s.SEGMENT_ID

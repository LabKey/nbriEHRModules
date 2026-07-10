/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    av.ANIMAL_VENDOR_ID            as AnimalVendorId,
    av.VENDOR_APPROVAL_CODE_ID     as VendorApprovalCode,
    av.VENDOR_Name                 as VendorName,
    av.STREET_ADDRESS1             as StreetAddress1,
    av.STREET_ADDRESS2             as StreetAddress2,
    av.CITY                        as City,
    av.STATE_PROV                  as StateProv,
    av.COUNTRY                     as Country,
    av.ZIP                         as Zip,
    av.ZIP_EXT                     as ZipExt,
    av.PHONE_NUMBER                as PhoneNumber,
    av.FAX_NUMBER                  as FaxNumber,
    av.COMMENTS                    as Comments,
    av.INTERNAL_VENDOR_YN          as InternalVendor,
    vpn.VENDOR_PRODUCTION_LOCATION_ID   as VendorProductionLocation
FROM ANIMAL_VENDOR av
LEFT JOIN VENDOR_PRODUCTION_LOCATION vpn ON av.ANIMAL_VENDOR_ID = vpn.ANIMAL_VENDOR_ID

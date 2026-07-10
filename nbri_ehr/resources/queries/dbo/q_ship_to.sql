/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    SHIPTO_ID               as ShipToId,
    SHIPTO_NICKNAME         as Name,
    COUNTRY                 as Country,
    City                    as City,
    STREET_ADDRESS1         as StreetAddress1,
    STREET_ADDRESS2         as StreetAddress2,
    STATE_PROV              as StateProv,
    ZIP                     as Zip,
    ZIP_EXT                 as ZipExt
FROM SHIPTO
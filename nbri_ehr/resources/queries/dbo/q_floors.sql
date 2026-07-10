/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    floor.LOCATION_ID        AS floor,
    floor.LOCATION_NAME      AS name,
    building.LOCATION_ID     AS building,
    area.LOCATION_ID         AS area
FROM LOCATION floor
JOIN LOCATION building ON floor.PARENT_LOCATION_ID = building.LOCATION_ID
JOIN LOCATION area ON building.PARENT_LOCATION_ID = area.LOCATION_ID
WHERE floor.LOCATION_TYPE_ID = 3 -- Floor
/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    building.LOCATION_ID        AS name,
    building.LOCATION_NAME      AS description,
    area.LOCATION_ID            AS area
FROM LOCATION building
JOIN LOCATION area ON building.PARENT_LOCATION_ID = area.LOCATION_ID
WHERE building.LOCATION_TYPE_ID = 2 -- building
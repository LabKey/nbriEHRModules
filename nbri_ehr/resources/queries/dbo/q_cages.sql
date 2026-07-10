/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    cage.LOCATION_ID        AS location,
    cage.LOCATION_NAME      AS cage,
    room.LOCATION_ID        AS room,
    floor.LOCATION_ID       as floor,
    building.LOCATION_ID    as building,
    area.LOCATION_ID        as area
FROM LOCATION cage
JOIN LOCATION room ON cage.PARENT_LOCATION_ID = room.LOCATION_ID
JOIN LOCATION floor ON room.PARENT_LOCATION_ID = floor.LOCATION_ID
JOIN LOCATION building ON floor.PARENT_LOCATION_ID = building.LOCATION_ID
JOIN LOCATION area ON building.PARENT_LOCATION_ID = area.LOCATION_ID
WHERE cage.LOCATION_TYPE_ID = 5 -- Sub Room
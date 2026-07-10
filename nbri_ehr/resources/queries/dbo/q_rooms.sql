/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    room.LOCATION_ID        AS room,
    room.LOCATION_NAME      AS name,
    floor.LOCATION_ID       AS floor,
    building.LOCATION_ID    AS building,
    area.LOCATION_ID        AS area
FROM LOCATION room
JOIN LOCATION floor ON room.PARENT_LOCATION_ID = floor.LOCATION_ID
JOIN LOCATION building ON floor.PARENT_LOCATION_ID = building.LOCATION_ID
JOIN LOCATION area ON building.PARENT_LOCATION_ID = area.LOCATION_ID
WHERE room.LOCATION_TYPE_ID = 4 -- Room
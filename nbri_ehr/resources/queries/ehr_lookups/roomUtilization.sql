/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

SELECT
    r.room,
    count(DISTINCT h.id) as TotalAnimals,
    r.building,
    r.area,
FROM ehr_lookups.rooms r
LEFT JOIN study.housing h
ON r.room = h.room

GROUP BY r.room, r.building, r.area
/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    pod.Id,
    pod.Id.demographics.species.common_name AS species,
    pod.Id.curLocation.room.name AS room,
    pod.Id.curLocation.cage.cage AS cage,
    pod.procedure.name AS procedure,
    pod.orderedby.displayName AS orderedBy,
    pod.windowStart,
    pod.windowEnd,
    pod.daysOverdue,
    pod.remark,
FROM prcOverdue pod
ORDER BY pod.daysOverdue DESC
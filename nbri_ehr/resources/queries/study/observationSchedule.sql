/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    g.id,
    g.scheduledDate,
    COUNT(g.caseid) cases,
    o.observations,
    SUM(g.obsCount) AS obsCount,
    o.orderIds,
    o.status,
    o.taskids,
    MAX(g.type) AS type,
    MAX(g.caseid) AS caseid
FROM
(
    SELECT
        sch.animalId as id,
        sch.date AS scheduledDate,
        sch.caseid,
        sch.type,
        COUNT(sch.category) AS obsCount
    FROM observationOrdersByDate sch
    GROUP BY
        sch.animalId,
        sch.date,
        sch.caseid,
        sch.type
) g
LEFT JOIN (
    SELECT
        obs.animalId AS id,
        obs.date AS scheduledDate,
        GROUP_CONCAT(DISTINCT obs.category, ';') AS observations,
        GROUP_CONCAT(obs.objectid, ';') AS orderIds,
        GROUP_CONCAT(obs.obsStatus, ';') AS status,
        GROUP_CONCAT(DISTINCT(obs.taskid), ';') AS taskids
    FROM observationOrdersByDate obs
    GROUP BY obs.animalId, obs.date
) o ON g.id = o.id AND g.scheduledDate = o.scheduledDate
GROUP BY
    g.id,
    g.scheduledDate,
    o.observations,
    o.orderIds,
    o.status,
    o.taskids

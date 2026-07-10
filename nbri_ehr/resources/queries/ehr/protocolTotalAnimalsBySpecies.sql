/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT i.* FROM (
    SELECT
    p.protocol                     as protocol,
    pa.species,
    group_concat(DISTINCT pa.id)   as Animals,
    CONVERT(Count(pa.id), INTEGER) AS TotalAnimals
    FROM ehr.protocol p
    LEFT JOIN ehr.protocolAnimals pa ON (p.protocol = pa.protocol)
    GROUP BY p.protocol, pa.species
) i
WHERE i.species IS NOT NULL



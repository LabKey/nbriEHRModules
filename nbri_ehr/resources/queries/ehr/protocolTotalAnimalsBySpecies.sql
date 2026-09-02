/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
  i.protocol,
  i.species,
  i.Animals,
  i.TotalAnimals,
  pc.allowed,
  CONVERT(pc.allowed - i.TotalAnimals, INTEGER) AS TotalRemaining,
  CASE
    WHEN (pc.allowed IS NULL OR pc.allowed = 0) THEN NULL
    ELSE round(CAST(i.TotalAnimals AS FLOAT) / CAST(pc.allowed AS FLOAT) * 100, 1)
  END AS PercentUsed

FROM (
    SELECT
    p.protocol                     as protocol,
    pa.species,
    group_concat(DISTINCT pa.id)   as Animals,
    CONVERT(Count(pa.id), INTEGER) AS TotalAnimals
    FROM ehr.protocol p
    LEFT JOIN ehr.protocolAnimals pa ON (p.protocol = pa.protocol)
    GROUP BY p.protocol, pa.species
) i

-- approved counts only, and only the row in effect today; see ehr.protocolCountsEffective
LEFT JOIN ehr.protocolCountsEffective pc ON (i.protocol = pc.protocol AND i.species = pc.species)

WHERE i.species IS NOT NULL

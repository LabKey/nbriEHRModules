/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
  i.protocol,
  i.species,
  i.animals AS Animals,
  i.totalAnimals AS TotalAnimals,
  pc.allowed,
  CONVERT(pc.allowed - i.TotalAnimals, INTEGER) AS TotalRemaining,
  CASE
    WHEN (pc.allowed IS NULL OR pc.allowed = 0) THEN NULL
    ELSE round(CAST(i.TotalAnimals AS FLOAT) / CAST(pc.allowed AS FLOAT) * 100, 1)
  END AS PercentUsed

FROM ehr.protocolHistoricalAnimalsBySpecies i

-- only the count in effect today; see ehr.protocolCountsEffective
LEFT JOIN ehr.protocolCountsEffective pc ON (i.protocol = pc.protocol AND i.species = pc.species)

WHERE i.species IS NOT NULL



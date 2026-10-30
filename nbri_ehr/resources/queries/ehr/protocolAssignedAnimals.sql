/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Per-protocol summary of distinct animals ever assigned; the lookup target of ehr.protocol.assignedAnimals
SELECT
  p.protocol,
  CONVERT(COALESCE(SUM(x.totalAnimals), 0), INTEGER) AS totalAssigned,
  -- LabKey SQL rejects a || expression as GROUP_CONCAT's first argument when a separator follows, so build it below
  CASE WHEN COUNT(x.label) = 0 THEN 'None' ELSE GROUP_CONCAT(x.label, ', ') END AS assignedBySpecies

FROM ehr.protocol p
LEFT JOIN (
  SELECT
    h.protocol,
    h.totalAnimals,
    COALESCE(s.common_name, h.species, 'Unknown') || ': ' || CAST(h.totalAnimals AS VARCHAR) AS label
  FROM ehr.protocolHistoricalAnimalsBySpecies h
    LEFT JOIN ehr_lookups.species_codes s ON (h.species = s.code)
) x ON (p.protocol = x.protocol)

GROUP BY p.protocol

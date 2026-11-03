/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Per-protocol summary of the counts in effect today; the lookup target of ehr.protocol.allowedAnimals
SELECT
  p.protocol,
  CONVERT(COALESCE(SUM(x.allowed), 0), INTEGER) AS totalAllowed,
  -- LabKey SQL rejects a || expression as GROUP_CONCAT's first argument when a separator follows, so build it below
  CASE WHEN COUNT(x.label) = 0 THEN 'None' ELSE GROUP_CONCAT(x.label, ', ') END AS allowedBySpecies

FROM ehr.protocol p
LEFT JOIN (
  SELECT
    c.protocol,
    c.allowed,
    COALESCE(s.common_name, c.species) || ': ' || CAST(c.allowed AS VARCHAR) AS label
  FROM ehr.protocolCountsEffective c
    LEFT JOIN ehr_lookups.species_codes s ON (c.species = s.code)
) x ON (p.protocol = x.protocol)

GROUP BY p.protocol

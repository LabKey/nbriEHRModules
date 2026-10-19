/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Per-protocol summary of the counts in effect today; the lookup target of ehr.protocol.allowedAnimals
SELECT
  x.protocol,
  SUM(x.allowed) AS totalAllowed,
  -- LabKey SQL rejects a || expression as GROUP_CONCAT's first argument when a separator follows, so build it below
  GROUP_CONCAT(x.label, ', ') AS allowedBySpecies

FROM (
  SELECT
    c.protocol,
    c.allowed,
    COALESCE(s.common_name, c.species) || ': ' || CAST(c.allowed AS VARCHAR) AS label
  FROM ehr.protocolCountsEffective c
    LEFT JOIN ehr_lookups.species_codes s ON (c.species = s.code)
) x

GROUP BY x.protocol

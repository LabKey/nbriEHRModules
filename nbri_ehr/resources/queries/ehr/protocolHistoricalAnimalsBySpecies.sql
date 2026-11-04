/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Distinct animals ever assigned to each protocol, by species; ended and repeated assignments still count once
SELECT
  a.protocol,
  a.Id.dataset.demographics.species AS species,
  CONVERT(COUNT(DISTINCT a.Id), INTEGER) AS totalAnimals,
  GROUP_CONCAT(DISTINCT a.Id) AS animals

FROM study.protocolAssignment a

WHERE a.qcstate.publicdata = true

GROUP BY a.protocol, a.Id.dataset.demographics.species

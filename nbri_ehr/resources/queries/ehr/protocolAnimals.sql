/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
  p.protocol,
  p.approve,
  a.id,
  a.species,

  COALESCE(a.Total, 0) AS TotalAssignments

FROM ehr.protocol p

--we find total distinct animals ever assigned to this protocol, since the last approval date
LEFT JOIN
  (SELECT a.protocol.displayName as protocol, a.id, a.id.dataset.demographics.Species AS Species, count(*) AS Total, max(a.date) as LatestStart,
  max(a.enddateCoalesced) as latestEnd
  FROM study.protocolAssignment a
  GROUP BY a.protocol.displayName, a.id, a.id.dataset.demographics.species) a ON (p.displayName = a.protocol)


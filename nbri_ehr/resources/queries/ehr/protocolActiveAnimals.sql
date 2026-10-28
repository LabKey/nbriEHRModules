/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
  p.protocol,
  p.title,
  count(distinct a.Id) AS TotalActiveAnimals,
  group_concat(distinct a.Id) AS ActiveAnimals

FROM ehr.protocol p

--we find distinct animals currently assigned to this protocol
LEFT JOIN
  (SELECT a.protocol, a.id, count(*) AS TotalAssignments, max(a.date) as LatestStart,
  max(a.enddateCoalesced) as latestEnd
  FROM study.protocolAssignment a
  WHERE a.isActive = true
  GROUP BY a.protocol, a.id) a ON (p.protocol = a.protocol.protocol)

group by p.protocol, p.title

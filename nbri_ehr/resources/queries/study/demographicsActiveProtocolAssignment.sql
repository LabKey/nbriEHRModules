/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- One row per animal: datasets join this on Id, so a row per assignment would repeat theirs.
-- The casts keep group_concat from becoming a multi-valued column, which the API returns as an array.
SELECT
  d.id,
  cast(group_concat(DISTINCT a.protocol.displayName, ', ') as varchar) as protocolDisplayName,
  cast(group_concat(DISTINCT (initcap(a.protocol.investigatorId.firstName) ||' '|| initcap(a.protocol.investigatorId.lastName)), ', ') as varchar) as investigator,
  cast(group_concat(DISTINCT a.protocol.title, ', ') as varchar) as protocolTitle,
  cast(group_concat(DISTINCT
       CASE
           WHEN a.protocol.investigatorId.lastName IS NOT NULL THEN (a.protocol.investigatorId.lastName || ' - ' || a.protocol.displayName)
           ELSE a.protocol.displayName
       END, ', ') as varchar) as protocolString

FROM study.demographics d
LEFT JOIN study.protocolAssignment a ON (a.id = d.id AND a.enddate IS NULL AND a.qcstate.publicdata = true)
GROUP BY d.id

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Counts proposed by an amendment that is with the IACUC but not yet decided. Reported alongside an over-count
-- warning so the person who filed the amendment does not read the warning as stale.
SELECT
  pc.protocol,
  pc.species,
  pc.allowed,
  a.rowid AS amendmentRowId,
  CAST(a.rowid AS VARCHAR) AS amendmentLabel

FROM ehr.protocol_counts pc
  JOIN nbri_ehr.ProtocolAmendment a ON (pc.amendmentId = a.objectid)

WHERE a.status.title = 'Submitted'

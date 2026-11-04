/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- The sanctioned read path for animal counts. Every amendment states the protocol's complete per-species counts, so a
-- count row is in effect exactly when today falls inside its amendment's date..enddate window. Consumers must come
-- through here rather than reading ehr.protocol_counts directly.
SELECT
  pc.protocol,
  pc.species,
  pc.allowed,
  a.date AS effectiveDate,
  a.enddate,
  a.rowid AS amendmentRowId

FROM ehr.protocol_counts pc
  JOIN nbri_ehr.ProtocolAmendment a ON (pc.amendmentId = a.objectid)

WHERE (a.date IS NULL OR CAST(a.date AS DATE) <= curdate())
  AND (a.enddate IS NULL OR CAST(a.enddate AS DATE) >= curdate())

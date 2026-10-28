/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Amendments on one protocol whose windows overlap. The amendment trigger rejects this, so a row here means a write
-- bypassed it, and "how many animals are allowed right now" has two answers.
SELECT
  a1.protocol,
  a1.rowid            AS rowid,
  a1.date             AS effectiveDate,
  a1.enddate          AS enddate,
  a2.rowid            AS overlappingRowId,
  a2.date             AS overlappingEffectiveDate,
  a2.enddate          AS overlappingEnddate

FROM nbri_ehr.ProtocolAmendment a1
  JOIN nbri_ehr.ProtocolAmendment a2
    ON (a1.protocol = a2.protocol AND a1.rowid < a2.rowid)

WHERE (a1.enddate IS NULL OR a2.date IS NULL OR CAST(a1.enddate AS DATE) >= CAST(a2.date AS DATE))
  AND (a2.enddate IS NULL OR a1.date IS NULL OR CAST(a2.enddate AS DATE) >= CAST(a1.date AS DATE))

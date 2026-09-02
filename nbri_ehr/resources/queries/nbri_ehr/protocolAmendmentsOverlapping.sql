/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Approved amendments on one protocol whose effective windows overlap. Approval closes the prior amendment, so the
-- workflow cannot produce this; a hand edit of an amendment's dates can, and two current amendments give "how many
-- animals are approved right now" two answers.
SELECT
  a1.protocol,
  a1.rowid            AS rowid,
  a1.effectiveDate    AS effectiveDate,
  a1.enddate          AS enddate,
  a2.rowid            AS overlappingRowId,
  a2.effectiveDate    AS overlappingEffectiveDate,
  a2.enddate          AS overlappingEnddate

FROM nbri_ehr.ProtocolAmendment a1
  JOIN nbri_ehr.ProtocolAmendment a2
    ON (a1.protocol = a2.protocol AND a1.rowid < a2.rowid)

WHERE a1.status.title = 'Approved' AND a2.status.title = 'Approved'
  AND (a1.enddate IS NULL OR a2.effectiveDate IS NULL OR CAST(a1.enddate AS DATE) >= CAST(a2.effectiveDate AS DATE))
  AND (a2.enddate IS NULL OR a1.effectiveDate IS NULL OR CAST(a2.enddate AS DATE) >= CAST(a1.effectiveDate AS DATE))

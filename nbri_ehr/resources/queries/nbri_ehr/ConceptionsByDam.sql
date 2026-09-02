/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    c.Dam AS Id,
    c.ConceptId,
    c.ConceptDate,
    c.Estimated,
    c.Sire,
    CASE
        WHEN c.isActive = true THEN 'Unknown'
        WHEN b.conceptId IS NOT NULL THEN 'Live Birth'
        ELSE COALESCE(po.result, 'Unknown')
    END AS conceptionOutcome,
    b.offspring,
    c.Remark,
    c.QCState AS qcstate
FROM Conception c
-- Both joins match isActive: a record claims its conception unless its QC state is explicitly non-public, so a null state counts as public.
-- The birth trigger blocks a duplicate conceptId, but ETL imports skip that check, so the aggregate guards against one.
LEFT JOIN (SELECT b.conceptId, MAX(b.Id) AS offspring FROM study.birth b WHERE b.conceptId IS NOT NULL AND (b.qcstate IS NULL OR b.qcstate.publicdata = true) GROUP BY b.conceptId) b
    ON b.conceptId = c.ConceptId
LEFT JOIN (SELECT p.conceptId, MAX(p.result.title) AS result FROM study.pregnancy p WHERE p.conceptId IS NOT NULL AND (p.qcstate IS NULL OR p.qcstate.publicdata = true) GROUP BY p.conceptId) po
    ON po.conceptId = c.ConceptId

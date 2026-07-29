/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    c.Dam AS Id,
    c.ConceptId,
    c.ConceptDate,
    c.ConceptTermDate,
    c.Sire,
    CASE
        WHEN b.conceptId IS NOT NULL THEN 'Live Birth'
        WHEN po.conceptId IS NOT NULL THEN COALESCE(po.result, 'Unknown')
        ELSE 'Unknown'
    END AS conceptionOutcome,
    c.Remark,
    c.QCState AS qcstate
FROM Conception c
LEFT JOIN (SELECT DISTINCT b.conceptId FROM study.birth b WHERE b.conceptId IS NOT NULL) b
    ON b.conceptId = c.ConceptId
LEFT JOIN (SELECT p.conceptId, MAX(p.result.title) AS result FROM study.pregnancy p WHERE p.conceptId IS NOT NULL GROUP BY p.conceptId) po
    ON po.conceptId = c.ConceptId

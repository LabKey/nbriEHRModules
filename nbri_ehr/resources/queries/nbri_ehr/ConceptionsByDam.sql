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
    c.Estimated,
    c.Sire,
    CASE
        WHEN b.conceptId IS NOT NULL THEN 'Live Birth'
        WHEN po.conceptId IS NOT NULL THEN COALESCE(po.result, 'Unknown')
        ELSE 'Unknown'
    END AS conceptionOutcome,
    b.offspring,
    c.Remark,
    c.QCState AS qcstate
FROM Conception c
LEFT JOIN (SELECT b.conceptId, GROUP_CONCAT(DISTINCT b.Id, ', ') AS offspring FROM study.birth b WHERE b.conceptId IS NOT NULL GROUP BY b.conceptId) b
    ON b.conceptId = c.ConceptId
LEFT JOIN (SELECT p.conceptId, MAX(p.result.title) AS result FROM study.pregnancy p WHERE p.conceptId IS NOT NULL GROUP BY p.conceptId) po
    ON po.conceptId = c.ConceptId

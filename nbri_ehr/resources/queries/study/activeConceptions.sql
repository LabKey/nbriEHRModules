/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    c.Dam AS Id,
    c.ConceptId,
    c.ConceptDate
FROM nbri_ehr.Conception c
WHERE c.isActive = true AND c.Dam IS NOT NULL

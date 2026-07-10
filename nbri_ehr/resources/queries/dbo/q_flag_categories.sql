/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    NAME AS category,
    'FALSE' AS enforceUnique,
    'FALSE' AS omitFromOverview,
    'TRUE' AS doHighlight
FROM EVENT_GROUP
WHERE EVENT_GROUP_ID IN (26, 28, 67)
-- 26 Quarantine
-- 28 Surgical Procedures - Major
-- 67 Adverse Reaction to Medication

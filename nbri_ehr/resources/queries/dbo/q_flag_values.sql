/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    CASE WHEN EVENT_ID = 1673 OR EVENT_ID = 1674 THEN 'Quarantine'
        ELSE EVENT_ID.NAME
        END AS value,
    EVENT_ID AS code,
    EVENT_ID AS objectid,
    EVENT_ID.DESCRIPTION,
    EVENT_GROUP_ID.NAME AS category
FROM EVENT_EVENT_GROUP
WHERE EVENT_GROUP_ID IN (26, 28, 67)
-- 26 Quarantine
-- 28 Surgical Procedures - Major
-- 67 Adverse Reaction to Medication

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    c.Id,
    c.conceptId,
    c.date,
    c.conceptionDays
FROM study.conception c
WHERE c.isActive = true AND c.Id IS NOT NULL

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- Add more here to make a single query for animal details in notifications
SELECT
    Id,
    date,
    project.name AS project,
    enddate
FROM study.assignment
WHERE enddate IS NULL
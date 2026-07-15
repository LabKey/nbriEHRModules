/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- This query is used to find the current project

SELECT DISTINCT assignment.Id,
assignment.project
FROM assignment
WHERE assignment.enddate is null
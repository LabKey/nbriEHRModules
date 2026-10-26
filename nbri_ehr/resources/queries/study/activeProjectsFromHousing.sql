/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- Joins assignment directly: Id.activeProjectAssignments.project lists all of an animal's projects in one value.
SELECT
    a.project.name as project,
    COUNT(*) as Total
FROM housing h
LEFT JOIN study.assignment a ON (a.id = h.id AND a.enddate IS NULL AND a.qcstate.publicdata = true)
GROUP BY a.project.name

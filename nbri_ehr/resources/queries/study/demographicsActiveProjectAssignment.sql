/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
  d.id,
  a.project.name as project
FROM study.demographics d
LEFT JOIN study.assignment a ON (a.id = d.id AND a.enddate IS NULL)
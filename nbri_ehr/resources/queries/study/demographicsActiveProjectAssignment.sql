/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- One row per animal: datasets join this on Id, so a row per assignment would repeat theirs.
-- The cast keeps group_concat from becoming a multi-valued column, which the API returns as an array.
SELECT
  d.id,
  cast(group_concat(DISTINCT a.project.name, ', ') as varchar) as project
FROM study.demographics d
LEFT JOIN study.assignment a ON (a.id = d.id AND a.enddate IS NULL AND a.qcstate.publicdata = true)
GROUP BY d.id

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

SELECT
    Id,
    date,
    taskid,
    performedBy.DisplayName AS performedBy,
    reason.title AS reason
FROM study.deaths
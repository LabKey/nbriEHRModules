/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    Id,
    date,
    category,
    area,
    observation,
    remark,
    performedBy,
    scheduledDate,
    QCState,
    type
FROM clinical_observations
WHERE type = 'Clinical'
GROUP BY
    Id,
    date,
    category,
    area,
    observation,
    remark,
    performedBy,
    scheduledDate,
    QCState,
    type
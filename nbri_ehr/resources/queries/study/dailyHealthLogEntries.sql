/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- One row per log entry; entryCodes keeps the codes in sort order, which GROUP_CONCAT would not.
SELECT
    d.logEntryId,
    d.Id,
    d.date,
    d.performedby,
    MAX(d.entryCodes) AS codes,
    d.taskid,
    d.QCState
FROM study.dailyHealthLog d
GROUP BY d.logEntryId, d.Id, d.date, d.performedby, d.taskid, d.QCState

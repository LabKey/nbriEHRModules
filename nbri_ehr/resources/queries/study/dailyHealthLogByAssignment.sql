/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- One row per code and per project and protocol assignment active on the observation date.
SELECT
    d.lsid,
    d.Id,
    d.date,
    a.project,
    pa.protocol,
    d.logEntryId,
    d.sortOrder,
    d.code,
    d.performedby,
    d.remark,
    d.QCState
FROM study.dailyHealthLog d
LEFT JOIN study.assignment a ON (
    a.Id = d.Id
    AND CAST(a.date AS DATE) <= CAST(d.date AS DATE)
    AND (a.enddate IS NULL OR CAST(a.enddate AS DATE) >= CAST(d.date AS DATE))
    AND a.qcstate.publicdata = true
)
LEFT JOIN study.protocolAssignment pa ON (
    pa.Id = d.Id
    AND CAST(pa.date AS DATE) <= CAST(d.date AS DATE)
    AND (pa.enddate IS NULL OR CAST(pa.enddate AS DATE) >= CAST(d.date AS DATE))
    AND pa.qcstate.publicdata = true
)

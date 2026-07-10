/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

SELECT
    d2.id,
    d2.protocol,
    h.enddate as enddate

FROM study.protocolAssignment d2
         JOIN (SELECT id, max(date) as maxDate, max(enddate) as enddate FROM study.protocolAssignment h GROUP BY id) h
              ON (h.id = d2.id and d2.date = h.maxdate)
WHERE d2.qcstate.publicdata = true
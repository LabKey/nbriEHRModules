/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

SELECT
    d2.id,
    d2.project,
    h.enddate as enddate

FROM study.assignment d2
         JOIN (SELECT id, max(date) as maxDate, max(enddate) as enddate FROM study.assignment h GROUP BY id) h
              ON (h.id = d2.id and d2.date = h.maxdate)
WHERE d2.qcstate.publicdata = true
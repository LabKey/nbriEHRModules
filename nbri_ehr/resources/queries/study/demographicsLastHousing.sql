/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT

d2.id,

-- NBRI housing stores the ehr_lookups.cage location key in cage, so it already carries the room.
d2.cage AS Location,

d2.room.area,

d2.room,

d2.cage
,

h.enddate as enddate

FROM study.housing d2
JOIN (SELECT id, max(date) as maxDate, max(enddate) as enddate FROM study.housing h WHERE h.qcstate.publicdata = true GROUP BY id) h
ON (h.id = d2.id and d2.date = h.maxdate)
WHERE d2.qcstate.publicdata = true
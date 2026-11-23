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

d2.enddate

FROM study.housing d2
WHERE d2.qcstate.publicdata = true
-- Don't self-join on max(date): Postgres badly underestimates that join and picks an unindexed nested loop.
AND NOT EXISTS (SELECT 1 FROM study.housing h WHERE h.id = d2.id AND h.date > d2.date AND h.qcstate.publicdata = true)

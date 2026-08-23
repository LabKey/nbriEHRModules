/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
  d.id,
  t.cage,
  t.total,
  cast(t.animals as varchar(4000)) as animals

FROM study.demographics d
LEFT JOIN (
SELECT
  h.id,
  h.cage,
  count(distinct h2.id) as total,
  group_concat(distinct h2.id, ', ') as animals

FROM study.housing h

JOIN study.housing h2
-- cage is the location id, and it is the only location housing stores: for a caged animal it is the room-and-cage key,
-- for a group pen it is the room key alone. Animals sharing that id are in the same place, which is what makes them
-- cagemates. Room is not consulted, since it is derived from this same id and so can never distinguish two rows.
ON (h.cage = h2.cage
        AND h2.Id.demographics.calculated_status = 'Alive'
        AND h2.enddateTimeCoalesced >= now()
        AND h2.qcstate.publicdata = true)

WHERE h.enddateTimeCoalesced >= now()
AND h.qcstate.publicdata = true
GROUP BY h.id, h.cage

) t ON (t.id = d.id)

WHERE d.calculated_status = 'Alive'

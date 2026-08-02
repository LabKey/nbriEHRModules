/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
  d.id,
--   t.room,
  t.cage,
  t.total,
  cast(t.animals as varchar(4000)) as animals

FROM study.demographics d
LEFT JOIN (
SELECT
  h.id,
--   h.room,
  h.cage,
  count(distinct h2.id) as total,
  group_concat(distinct h2.id, ', ') as animals

FROM study.housing h

JOIN study.housing h2
-- cage holds a location key that already encodes the room, so caged animals match on cage alone. Group/pen rooms have
-- no cage, so those fall back to the room, which is only consulted when neither side has a cage.
ON ((h.cage = h2.cage OR (h.cage IS NULL AND h2.cage IS NULL AND h.room = h2.room))
        AND h2.Id.demographics.calculated_status = 'Alive'
        AND h2.enddateTimeCoalesced >= now()
        AND h2.qcstate.publicdata = true)

WHERE h.enddateTimeCoalesced >= now()
AND h.qcstate.publicdata = true
GROUP BY h.id, h.room, h.cage

) t ON (t.id = d.id)

WHERE d.calculated_status = 'Alive'
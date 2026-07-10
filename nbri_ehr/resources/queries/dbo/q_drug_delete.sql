/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT ade.objectid,
       ade.modified,
       ade.REFERENCE,
       drugEvents.event_id,
       ade.event_name
FROM auditDeleteEvent ade

LEFT JOIN -- on drug events

    (SELECT e.name, e.event_id, eeg.event_group_id
      FROM event_event_group eeg LEFT JOIN event e ON eeg.event_id = e.event_id
      WHERE (eeg.EVENT_GROUP_ID = 31 AND eeg.EVENT_ID = 2261) -- Sedation from Timed Procedures
            OR eeg.EVENT_GROUP_ID IN (3, 25, 34, 39, 51, 54, 65)
      GROUP BY eeg.event_group_id, e.event_id, e.name) drugEvents

ON ade.event_name = drugEvents.name WHERE drugEvents.name IS NOT NULL

GROUP BY
    ade.objectid,
    ade.modified,
    ade.REFERENCE,
    drugEvents.event_id,
    ade.event_name
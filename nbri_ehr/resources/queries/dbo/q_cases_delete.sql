/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT ade.objectid,
       ade.modified,
       ade.REFERENCE,
       ct.event_id,
       ade.event_name
FROM auditDeleteEvent ade

LEFT JOIN -- on clinical treatments

(SELECT e.name, e.event_id, eeg.event_group_id
 FROM event_event_group eeg
          LEFT JOIN event e ON eeg.event_id = e.event_id
 WHERE (eeg.EVENT_GROUP_ID = 37) -- Clinical Treatments
 GROUP BY eeg.event_group_id, e.event_id, e.name) ct

ON ade.event_name = ct.name

WHERE ct.name IS NOT NULL

GROUP BY ade.objectid,
         ade.modified,
         ade.REFERENCE,
         ct.event_id,
         ade.event_name
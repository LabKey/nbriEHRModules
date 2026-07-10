/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
       adt.modified,
       substring(adt.PRIMARY_KEY_VALUES, length('ANIMAL_EVENT_ID = ')) as event_id FROM
    (SELECT
         MAX(AUDIT_TRAIL.CHANGE_DATETIME) as modified,
         AUDIT_TRAIL.PRIMARY_KEY_VALUES
     FROM AUDIT_TRAIL
     WHERE AUDIT_TRAIL.PRIMARY_KEY_VALUES like '%ANIMAL_EVENT_ID =%' AND AUDIT_TRAIL.COLUMN_NAME != 'DELETE'
     GROUP BY AUDIT_TRAIL.PRIMARY_KEY_VALUES
    ) adt
/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- subquery to extract an animal event from AUDIT_TRAIL.REFERENCE to identify delete event
SELECT
    auditSubQuery.objectid,
    auditSubQuery.modified,
    auditSubQuery.REFERENCE,
    trim(substring(auditSubQuery.event_partial, 1, (length(auditSubQuery.event_partial) -
                                                      (length('Event Datetime:') + length(event_datetime))))) as event_name --extract event name from REFERENCE
FROM (
         SELECT substring(adt.PRIMARY_KEY_VALUES, length('ANIMAL_EVENT_ID = '))             AS objectid,
                CAST(adt.CHANGE_DATETIME AS TIMESTAMP)                                      AS modified,
                adt.REFERENCE,
                substring(adt.reference,
                       locate('Event:', adt.reference) + length('Event:'))                   AS event_partial,
                substring(adt.reference,
                       locate('Event Datetime:', adt.reference) + length('Event Datetime:')) AS event_datetime
         FROM AUDIT_TRAIL adt
         WHERE adt.PRIMARY_KEY_VALUES LIKE '%ANIMAL_EVENT_ID%'
           AND adt.COLUMN_NAME = 'DELETE') auditSubQuery
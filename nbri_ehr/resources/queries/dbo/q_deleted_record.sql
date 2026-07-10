/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    dr.ID                      AS Id,
    dr.PROTOCOL_NUMBER         AS Protocol,
    dr.USER_REFERENCE_NUMBER   AS UserReferenceNumber,
    dr.PI_NAME                 AS Investigator,
    dr.TITLE                   AS Title,
    dre.ESIGNATURE_EVENT_ID    AS EsigEvent,
    dre.USER_PROFILE_ID.STAFF_ID.STAFF_FIRST_NAME
        || '|' || dre.USER_PROFILE_ID.STAFF_ID.STAFF_LAST_NAME as EsigUser,
    dre.ESIG_DATETIME          AS EsigDate
FROM DELETED_RECORD dr
LEFT JOIN DELETED_RECORD_ESIG dre ON dr.ID = dre.DELETED_RECORD_ID
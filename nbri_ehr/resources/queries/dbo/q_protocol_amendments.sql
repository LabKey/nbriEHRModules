/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT PROTOCOL_ID AS "protocolId",
       CASE WHEN PROTOCOL_NUMBER IS NULL THEN 'missing'
            ELSE PROTOCOL_NUMBER END AS "protocol",
       RENEWAL_DATE AS "amendmentDate",
       SUBMISSION_DATE AS "submitted",
       APPROVAL_DATE AS "approved",
       X.modified
FROM PROTOCOL p
         LEFT JOIN (
    SELECT substring(PRIMARY_KEY_VALUES, length('PROTOCOL_ID = ')) AS protocolId,
           COALESCE(MAX(CAST(CHANGE_DATETIME AS TIMESTAMP)), to_date('01/01/1970' ,'MM/DD/YYYY')) AS modified
    FROM AUDIT_TRAIL
    WHERE TABLE_NAME = 'PROTOCOL'
    GROUP BY substring(PRIMARY_KEY_VALUES, length('PROTOCOL_ID = '))
) X ON X.protocolId = p.PROTOCOL_ID
WHERE PROTOCOL_TYPE_ID IN (2,3,4)
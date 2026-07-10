/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT 'DISP_CD_' || ID AS objectid,
'disposition_codes' AS set_name,
ID AS "value",
DESCRIPTION AS description,
NAME AS title,
ID AS sort_order,
CASE ACTIVE_YN
WHEN 'Y' THEN NULL
WHEN 'N' THEN '1/1/1970'
END AS date_disabled,
NULL AS category
FROM ANIMAL_DISPOSITION

UNION

SELECT 'TERMINATION_RSN_CD_' || ID AS objectid,
'termination_reason_codes' AS set_name,
ID AS "value",
DESCRIPTION AS description,
NAME AS title,
ID AS sort_order,
CASE ACTIVE_YN
WHEN 'Y' THEN NULL
WHEN 'N' THEN '1/1/1970'
END AS date_disabled,
NULL AS category
FROM TERMINATION_REASON

UNION

SELECT 'KEYWORD_' || KEY_WORD_ID AS objectid,
'keyword' AS set_name,
KEY_WORD_ID AS "value",
NULL AS description,
KEY_WORD_NAME AS title,
KEY_WORD_ID AS sort_order,
NULL AS date_disabled,
NULL AS category
FROM KEY_WORD

UNION

SELECT
'CLINREMARKS_CATEGORY' || EVENT_ID.EVENT_ID as objectid,
'clinremarks_category' AS set_name,
EVENT_ID.EVENT_ID AS "value",
EVENT_ID.DESCRIPTION AS description,
EVENT_ID.NAME AS title,
EVENT_ID.EVENT_ID AS sort_order,
CASE
    WHEN EVENT_ID.ACTIVE_YN = 'Y' AND EVENT_GROUP_ID.ACTIVE_YN = 'Y' THEN NULL
    ELSE '1/1/1970'
    END AS date_disabled,
NULL AS category,
FROM EVENT_EVENT_GROUP
WHERE EVENT_GROUP_ID.EVENT_GROUP_ID = 53

UNION

SELECT
    'COST_CENTER' || COST_CENTER_ID      as objectid,
    'cost_center'       as set_name,
    COST_CENTER_ID      as "value",
    COST_CENTER_DESC    as description,
    COST_CENTER_NAME    as title,
    COST_CENTER_ID      as sort_order,
    NULL AS date_disabled,
    NULL AS category
FROM COST_CENTER


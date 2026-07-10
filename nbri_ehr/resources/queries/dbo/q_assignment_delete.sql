/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT substring(adt.PRIMARY_KEY_VALUES, length('Alternate_ID = ')) AS objectid,
       CAST(adt.CHANGE_DATETIME AS TIMESTAMP)                            AS modified,
       adt.REFERENCE
FROM AUDIT_TRAIL adt
WHERE adt.TABLE_NAME = 'ALTERNATE' AND
        adt.COLUMN_NAME = 'DELETE'
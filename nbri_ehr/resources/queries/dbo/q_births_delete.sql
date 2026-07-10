/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT an.ANIMAL_ID_NUMBER AS Id,
       CAST(adt.CHANGE_DATETIME AS TIMESTAMP)                    AS modified,
       adt.REFERENCE
FROM AUDIT_TRAIL adt
LEFT JOIN ANIMAL an ON an.ANIMAL_ID = substring(adt.PRIMARY_KEY_VALUES, length('ANIMAL_ID = '))
WHERE adt.TABLE_NAME = 'ANIMAL' AND adt.COLUMN_NAME = 'DELETE'
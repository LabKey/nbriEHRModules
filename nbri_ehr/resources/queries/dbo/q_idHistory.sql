/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    ANIMAL_ID.ANIMAL_ID_NUMBER           AS Id,
    ALTERNATE_TYPE_ID                    AS Type,
    NAME
FROM ALTERNATE
WHERE ALTERNATE_TYPE_ID IN (1, 2, 4, 5)
GROUP BY ANIMAL_ID.ANIMAL_ID_NUMBER, NAME, ALTERNATE_TYPE_ID
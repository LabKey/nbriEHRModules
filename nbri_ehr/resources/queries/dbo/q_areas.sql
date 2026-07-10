/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT LOCATION_ID      AS area,
       LOCATION_NAME    AS description,
FROM LOCATION
WHERE LOCATION_TYPE_ID = 1 -- SITE
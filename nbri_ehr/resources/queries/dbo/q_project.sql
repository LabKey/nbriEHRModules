/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

SELECT min(alt.ALTERNATE_ID) as project,
       alt.project as name
FROM
(
    SELECT
        CASE WHEN (SUBSTRING(trim(NAME),3,1) = '-' OR SUBSTRING(trim(NAME),5,1) = '-')
            THEN trim(SUBSTRING(trim(NAME),0,11))
            ELSE trim(SUBSTRING(trim(NAME), 0, 8))
        END as project,
        ALTERNATE_ID
    FROM ALTERNATE
    WHERE ALTERNATE_TYPE_ID = 6
) alt
GROUP BY alt.project
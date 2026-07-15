/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    f1.Id,
    group_concat(DISTINCT f1.flag, chr(10)) as flags

FROM (
         SELECT
             f.Id,
             CASE
                 WHEN f.flag is null THEN null
                 WHEN f.flag.category IS NULL THEN f.flag.value
                 ELSE cast((CAST(f.flag.category as varchar(100)) || CAST(': ' as varchar(2)) || CAST(f.flag.value as varchar(100))) as varchar(202))
                 END as flag

         FROM study.flags f
         WHERE f.isActive = true

     ) f1

GROUP BY f1.Id
/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT Id,
       GROUP_CONCAT(alias, ', ') alias,
       category.title as aliasType
FROM alias
GROUP BY Id, category.title
    PIVOT alias BY aliasType IN (SELECT title FROM ehr_lookups.alias_category)
/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- This query is used to match upper and lowercase names in animal history

SELECT Id,
       Id as alias
FROM study.Animal where Dataset.Demographics.calculated_status != 'Alive - In Progress'
UNION
SELECT Id,
    Alias as alias
FROM study.alias where Id.demographics.calculated_status != 'Alive - In Progress'
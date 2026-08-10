/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
g.id,
g.groupId.title as name,
cast('yes' as varchar) as valueField

FROM study.animal_group_members g

WHERE (g.enddate IS NULL OR COALESCE(g.enddate, curdate()) >= curdate())

GROUP BY g.id, g.groupId.title

PIVOT valueField by name IN (select title FROM ehr_lookups.breeding_type)

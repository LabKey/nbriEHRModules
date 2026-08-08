/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    m.Id,
    count(distinct m.objectid) as totalGroups,
    group_concat(distinct m.name, chr(10)) as groups

FROM (SELECT Id,
             objectid,
             groupId.title as name
      FROM study.animal_group_members
      WHERE enddate is NULL AND qcstate.publicdata = true) m
GROUP BY m.Id

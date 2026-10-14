/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT

    d.id as Id,
    d2.id  AS Offspring,
    d2.birth AS date,
d2.id.parents.sire AS sire,
d2.id.parents.dam AS dam,
d2.gender AS sex,
d.qcstate

FROM study.Demographics d

    -- parents rather than the raw columns, which drops the UNKNOWN placeholder parent
    INNER JOIN study.Demographics d2
ON ((d2.id.parents.sire = d.id OR d2.id.parents.dam = d.id) AND d.id != d2.id)

group by d.id, d2.id, d2.birth, d2.id.parents.sire, d2.id.parents.dam, d2.gender, d.qcstate

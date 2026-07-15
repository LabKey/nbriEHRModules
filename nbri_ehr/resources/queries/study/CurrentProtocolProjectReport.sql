/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

SELECT DISTINCT Id, protocol.title as protocol, project FROM
    (
        SELECT DISTINCT d.Id,
                        a.project.name as project, a.enddate AS projectenddate,
                        pa.protocol, pa.enddate AS protocolenddate
        FROM study.demographics d
                 JOIN study.assignment a ON a.Id = d.Id
                 JOIN study.protocolAssignment pa ON pa.Id = d.Id
        WHERE a.enddate IS NULL
    ) sub
WHERE sub.protocolenddate IS NULL
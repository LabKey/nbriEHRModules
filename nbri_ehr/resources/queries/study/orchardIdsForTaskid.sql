/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
PARAMETERS (MYTASKID VARCHAR)

SELECT Id FROM (SELECT Id, taskid
                FROM study.demographics
                UNION
                SELECT Id, taskid
                FROM study.housing
                UNION
                SELECT Id, taskid
                FROM study.protocolAssignment
                ) u
WHERE u.taskid IS NOT NULL AND u.taskid = MYTASKID;
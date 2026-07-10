/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    DEPARTMENT_ID           as DepartmentId,
    DEPARTMENT_NAME         as Name,
    DEPARTMENT_DESC         as Description,
    PARENT_DEPARTMENT_ID    as ParentDepartment,
    STAFF_ID.STAFF_FIRST_NAME
        || '|' || STAFF_ID.STAFF_LAST_NAME as Staff
FROM DEPARTMENT
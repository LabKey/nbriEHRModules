/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    ac.ACCOUNT_ID                  as AccountId,
    ac.ACCOUNT_NUMBER              as AccountNumber,
    ac.DEPARTMENT_ID               as department,
    ac.COST_TYPE_ID                as costType,
    ac.EXPENSE_CLASS_ID            as expenseClass,
    ac.PROJECT_CODE_ID             as projectCode,
    ac.ACCOUNT_DESC                as description,
    ac.ACTIVE_YN                   as active,
    ar.area                        as area
FROM ACCOUNT ac
         LEFT JOIN q_areas ar ON ar.area = ac.LOCATION_ID
/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    STRESS_ID               AS StressId,
    STRESS_NAME             AS Name,
    STRESS_DESCRIPTION      AS Description,
    STRESS_RANKING          AS Ranking,
    REGULATORY_STRESS_LEVEL_ID  AS RegulatoryStressLevel,
    ACTIVE_YN               AS Active
FROM STRESS_NAME
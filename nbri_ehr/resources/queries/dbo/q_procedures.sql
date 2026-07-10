/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    PROCEDURE_ID        as procedureId,
    ACTIVE_YN           as active,
    PROCEDURE_NAME      as name,
    PROCEDURE_DESC      as description
FROM "PROCEDURE"
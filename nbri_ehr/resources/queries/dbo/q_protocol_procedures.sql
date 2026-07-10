/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    SEGMENT_ID.PROTOCOL_ID.PROTOCOL_ID      AS Protocol,
    SEGMENT_ID.SPECIES_ID                   AS Species,
    PROCEDURE_ID.PROCEDURE_NAME             AS Procedure
FROM SEGMENT_PROCEDURE
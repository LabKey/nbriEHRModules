/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    SPEC_NAME AS common,
    SPEC_DESC AS scientific_name,
    3 AS blood_per_kg,
    1 AS max_draw_pct,
    7 blood_draw_interval
FROM SPECIES

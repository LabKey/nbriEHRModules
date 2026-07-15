/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    demo.Id,
    CASE WHEN d.QCState.Label = 'Request: Pending' OR d.QCState.Label = 'Review Required' THEN 'Necropsy Pending' ELSE demo.calculated_status END AS necropsy_status
    FROM study.demographics demo
LEFT JOIN study.deaths d ON d.Id = demo.Id
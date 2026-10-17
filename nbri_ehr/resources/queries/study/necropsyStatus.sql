/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- 'Necropsy Pending' from the submitted request until the final results are submitted with nothing pending
SELECT
    demo.Id,
    CASE WHEN t.status IN ('Request Pending', 'Preliminary Results Submitted', 'Final Results Submitted: Pending Addendum')
         THEN 'Necropsy Pending'
         ELSE demo.calculated_status
    END AS necropsy_status
FROM study.demographics demo
LEFT JOIN study.necropsyRequest r ON r.Id = demo.Id
LEFT JOIN nbri_ehr.necropsyTask t ON t.taskid = r.taskid

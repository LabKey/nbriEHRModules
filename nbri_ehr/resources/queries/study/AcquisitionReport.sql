/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT d.Id,
       d.gender,
       d.species,
       d.Birth,
       d.Id.MostRecentArrival.Center_Arrival,
       cpp.project
FROM study.demographics d
         LEFT JOIN CurrentProtocolProjectReport cpp ON cpp.Id = d.Id
ORDER BY d.Id ASC
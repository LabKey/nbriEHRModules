/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
PARAMETERS(StartDate TIMESTAMP, EndDate TIMESTAMP, Project VARCHAR)

SELECT s.species, s.project, s.speciesName, COUNT(*) as Total FROM
    (
        SELECT
            species,
            species.scientific_name AS speciesName,
            Center_Arrival,
            project
        FROM study.AcquisitionReport ar
        WHERE CAST(COALESCE(StartDate, '1900-01-01') as date) <= ar.Center_Arrival AND CAST(COALESCE(EndDate, curdate()) as date) >= CAST(ar.Center_Arrival as date) AND Project = ar.project
    ) s
GROUP BY s.species, s.project, s.speciesName
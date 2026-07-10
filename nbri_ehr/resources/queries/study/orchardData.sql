/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    d.Id,
    d.gender,
    d.birth,
    d.species.scientific_name as species,
    pa.protocol.title as protocol,
	InitCap(pa.protocol.InvestigatorId.LastName) || ', ' || InitCap(pa.protocol.InvestigatorId.FirstName) as PI,
	InitCap(pa.protocol.AuthorId.LastName) || ', ' || InitCap(pa.protocol.AuthorId.FirstName) as Vet,
    h.cage.cage as cage,
    d.calculated_status as alive
FROM demographics d
LEFT JOIN housing h ON d.Id = h.Id
LEFT JOIN protocolAssignment pa ON d.Id = pa.Id AND pa.endDate is null
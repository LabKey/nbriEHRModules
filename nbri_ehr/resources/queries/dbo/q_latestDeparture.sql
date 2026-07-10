/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Used in finalDeparture
SELECT Id,
MAX(eventDate) as latestDep,
FROM q_departure
GROUP BY Id
/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
  p.protocol,
  p.displayName,
  p.title,
  CASE WHEN p.investigatorId IS NULL THEN NULL
       ELSE LTRIM(RTRIM(COALESCE(p.investigatorId.firstName, '') || ' ' || COALESCE(p.investigatorId.lastName, '')))
  END AS pi,
  CAST(p.enddate AS DATE) AS expirationDate,
  TIMESTAMPDIFF('SQL_TSI_DAY', CAST(curdate() AS TIMESTAMP), CAST(CAST(p.enddate AS DATE) AS TIMESTAMP)) AS daysUntilExpiration,
  p.activeAnimals.TotalActiveAnimals AS activeAnimals

FROM ehr.protocol p

-- a protocol ending today is still usable today, so compare whole days rather than timestamps
WHERE CAST(p.enddate AS DATE) >= curdate()
  AND CAST(p.enddate AS DATE) <= timestampadd('SQL_TSI_DAY', 30, curdate())

ORDER BY p.enddate, p.protocol

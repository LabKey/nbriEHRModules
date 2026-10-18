/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
  co.Id,
  co.date AS mostRecentBCSDate,
  -- Lists every score recorded at the latest timestamp; earlier scores from the same day are excluded
  group_concat(DISTINCT co.observation, ', ') AS mostRecentBCS
FROM study.clinical_observations co
-- latest: each animal's most recent BCS date
JOIN (
  SELECT Id, max(date) AS maxDate
  FROM study.clinical_observations
  WHERE qcstate.publicdata = true AND category = 'BCS' AND observation IS NOT NULL
  GROUP BY Id
) latest ON (co.Id = latest.Id AND co.date = latest.maxDate)
WHERE co.qcstate.publicdata = true AND co.category = 'BCS' AND co.observation IS NOT NULL
GROUP BY co.Id, co.date

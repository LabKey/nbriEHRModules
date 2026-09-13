/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
  pr.protocol,
  pr.description,
  CASE WHEN pr.investigatorId.lastName IS NULL THEN pr.displayName ELSE pr.displayName || ' - ' || pr.investigatorId.lastName END AS displayText
FROM ehr.protocol pr
WHERE pr.inactiveDate IS NULL OR pr.inactiveDate > now()
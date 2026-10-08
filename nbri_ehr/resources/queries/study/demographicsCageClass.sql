/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT

d.id,
d.id.MostRecentWeight.MostRecentWeight as MostRecentWeight,

c.sqft as ReqSqFt,

c.height as ReqHeight

from study.demographics d

-- No requirementset filter: NBRI's sets split the weight range between them rather than overlap, so each weight matches one row.
LEFT JOIN ehr_lookups.cageclass c

ON (c.low < d.id.MostRecentWeight.MostRecentWeight AND d.id.MostRecentWeight.MostRecentWeight <= c.high)

WHERE
d.calculated_status = 'Alive'

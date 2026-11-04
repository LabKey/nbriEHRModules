/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Give every existing protocol its amendment 0, spanning approve..enddate. Its counts are left for users to enter.
-- ehr.protocol is keyed by objectid, so a protocol number can repeat within a container; take the latest approval.
INSERT INTO nbri_ehr.ProtocolAmendment (protocol, amendmentNumber, date, enddate, QCState, objectid, Container, Created, Modified)
SELECT DISTINCT ON (p.container, p.protocol)
    p.protocol,
    0,
    p.approve,
    p.enddate,
    (SELECT ds.RowId FROM core.DataStates ds WHERE ds.Container = p.container AND ds.Label = 'Completed'),
    UPPER(CAST(gen_random_uuid() AS VARCHAR)),
    p.container,
    now(),
    now()
FROM ehr.protocol p
WHERE p.container IS NOT NULL
  AND p.protocol IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM nbri_ehr.ProtocolAmendment a WHERE a.Container = p.container AND a.protocol = p.protocol)
ORDER BY p.container, p.protocol, p.approve DESC NULLS LAST;

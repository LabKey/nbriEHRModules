/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- subquery to get staff info
-- email_prefix below (used for display name) and transformEmailDomain '@louisiana.edu' (defined in etl xml)
-- during user column transform creates a deactivated labkey user during an ETL run
SELECT
s.staff_id,
(s.staff_last_name ||', '|| s.staff_first_name) as staffName,
s.email_address,
(CASE
    WHEN s.email_address IS NULL THEN 'unknown'
    ELSE substring(s.email_address, 1,
                locate('@', s.email_address) - 1) END) as email_prefix
FROM STAFF s
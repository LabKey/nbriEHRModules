/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT protocol, title FROM ehr.protocol pr
WHERE pr.inactiveDate IS NULL OR pr.inactiveDate > now()
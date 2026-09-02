/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- These reference ehr_lookups sets, whose key column is varchar. An integer column against a varchar key resolves
-- fine in SQL but the data entry store's reader drops the value on load, so the form reopened a saved amendment
-- with its type and status blank.
ALTER TABLE nbri_ehr.ProtocolAmendment ALTER COLUMN amendmentType TYPE VARCHAR(200) USING amendmentType::VARCHAR;
ALTER TABLE nbri_ehr.ProtocolAmendment ALTER COLUMN status TYPE VARCHAR(200) USING status::VARCHAR;

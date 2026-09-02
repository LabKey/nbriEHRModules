/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Every approved amendment states the protocol's complete per-species counts, so the window in which those counts
-- are in effect belongs to the amendment: effectiveDate through enddate, where enddate is the day before the next
-- approved amendment on the protocol takes effect and null while this one is current.
ALTER TABLE nbri_ehr.ProtocolAmendment ADD enddate TIMESTAMP;

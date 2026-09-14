/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- Conception is now the study.conception dataset, filed against the dam on the conception date. Existing rows are not carried over.
DROP TABLE IF EXISTS nbri_ehr.Conception;


/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT *, CASE WHEN qcstate.label = 'Completed' THEN 'Completed' ELSE '' END as status FROM study.prc_order
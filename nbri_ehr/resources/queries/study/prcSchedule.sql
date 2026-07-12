/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

SELECT
    po.procedureRecord,
    po.Id,
    po.procedure,
    po.windowStart,
    po.windowEnd,
    po.orderedby,
    po.remark,
    po.caseid,
    po.objectid,
    po.lsid,
    CASE WHEN po.qcstate.label = 'Completed' THEN 'Completed' ELSE '' END as status
FROM prc_order po
WHERE now() >= windowStart AND now() <= windowEnd
/*
 * Copyright (c) 2025-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT

taskid,
rowid,
updateTitle,
category,
title,
formtype,
qcstate,
assignedto,
duedate,
requestid,
datecompleted,
modifiedby,
modified,
createdby,
created,
description


FROM ehr.tasks t

WHERE ISMEMBEROF(t.assignedto)
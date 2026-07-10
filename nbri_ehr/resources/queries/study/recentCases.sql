/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

PARAMETERS(SubjectId VARCHAR, CaseCategory VARCHAR)

SELECT * FROM study.cases
WHERE Id = SubjectId AND category = CaseCategory
ORDER BY date DESC LIMIT 20
/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    QUESTION_ID             AS QuestionId,
    QUESTION_TYPE_ID.QUESTION_TYPE_NAME AS Type,
    QUESTION_TEXT           AS Text,
    QUESTION_CREATION_DATE  as CreationDate
FROM QUESTION
/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    qr.QUESTION_RESPONSE_ID         AS QuestionResponseId,
    qr.PROTOCOL_ID                  AS Protocol,
    rt.TEXT                         AS Response,
    qq.QUESTION_ID                  AS Question,
    qq.QUESTIONNAIRE_ID             AS Questionnaire
FROM QUESTION_RESPONSE qr
LEFT JOIN RESPONSE_TEXT rt ON qr.QUESTION_RESPONSE_ID = rt.QUESTION_RESPONSE_ID
LEFT JOIN QUESTIONNAIRE_QUESTION qq ON qr.QUESTION_INSTANCE_ID = qq.QUESTION_INSTANCE_ID
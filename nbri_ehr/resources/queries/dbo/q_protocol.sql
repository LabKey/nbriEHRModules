/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
       p.PROTOCOL_ID            AS objectid,
       p.PROTOCOL_NUMBER        AS title,
       trim(p.PI_ID.STAFF_FIRST_NAME)
           || '|' || trim(p.PI_ID.STAFF_LAST_NAME) as investigatorId,
       trim(p.AUTHOR_ID.STAFF_FIRST_NAME)
           || '|' || trim(p.AUTHOR_ID.STAFF_LAST_NAME) as authorId,
       trim(p.OWNER_ID.STAFF_FIRST_NAME)
           || '|' || trim(p.OWNER_ID.STAFF_LAST_NAME) as ownerId,
       p.PROTOCOL_NUMBER        AS external_id,
       p.PROTOCOL_ID            AS protocol,
       p.DEPARTMENT_ID          AS departmentId,
       p.PROTOCOL_ID_PREFIX     AS prefix,
       p.PROTOCOL_ID_SUFFIX     AS suffix,
       p.PROTOCOL_SHORT_TITLE   AS shortDescription,
       p.APPROVAL_DATE          AS approve,
       p.EXPIRATION_DATE        AS endDate,
       p.RENEWAL_DATE           AS renewalDate,
       p.EMERGENCY_PHONE_NUM    AS emergencyPhoneNumber,
       p.INACTIVE_DATE          AS inactiveDate,
       p.NEXT_REVIEW_DATE       AS nextReviewDate,
       p.EFFECTIVE_DATE         AS effectiveDate,
       p.CURRENT_STATE          AS currentState,
       p.USER_REFERENCE_NUMBER  AS userReferenceNumber,
       p.UNRESTRICTED_VIEW_YN   AS isUnrestrictedView,
       p.AUDIT_YN               AS isAudit,
       p.AUDIT_YN_MODIFIED_YN   AS isAuditModified,
       p.PROTOCOL_TYPE_ID       AS protocolTypeId,
       p.PARENT_PROTOCOL_ID     AS parentProtocolId,
       p.QUESTIONNAIRE_ID       AS questionnaireId,
       p.AMENDMENT_REASON       AS amendmentReason,
       p.REQUESTED_EFFECTIVE_DATE AS requestedEffectiveDate,
       p.PROTOCOL_CATEGORY_ID   AS protocolCategoryId,
       p.OBJECTIVE              AS objective,
       p.RATIONALE              AS rationale,
       p.TISSUE_HARVEST_YN      AS isTissueHarvest,
       p.ORDER_TRACKING_YN      AS isOrderTracking,
       p.LONG_TERM_YN           AS isLongTerm,
       p.IN_VIVO_YN             AS isInVivo,
       p.STOCK_YN               AS isStock,
       p.SUBMISSION_DATE        AS submissionDate,
       p.REVIEW_COMPLETION_DATE AS reviewCompletionDate,
       p.HONOR_RESET_YN         AS isHonorReset,
       area.area                AS area,
       pd.text                  AS description,
       COALESCE(X.modified, to_date('01/01/1970' ,'MM/DD/YYYY')) AS modified
FROM PROTOCOL p
    LEFT JOIN (
        SELECT substring(PRIMARY_KEY_VALUES, length('PROTOCOL_ID = ')) AS protocolId,
               MAX(CAST(CHANGE_DATETIME AS TIMESTAMP)) AS modified
        FROM AUDIT_TRAIL
        WHERE TABLE_NAME = 'PROTOCOL'
        GROUP BY substring(PRIMARY_KEY_VALUES, length('PROTOCOL_ID = '))
        ) X ON X.protocolId = p.PROTOCOL_ID
    LEFT JOIN q_areas area ON area.area = p.LOCATION_ID
    LEFT JOIN PROTOCOL_DESCRIPTION pd ON pd.PROTOCOL_ID = p.PROTOCOL_ID

/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    put.PROTOCOL_NUMBER         as Protocol,
    put.USER_REFERENCE_NUMBER   as UserReferenceNumber,
    put.SHORT_TITLE             as ShortTitle,
    put.PI_NAME                 as Investigator,
    put.PROTOCOL_TYPE           as ProtocolType,
    put.CURRENT_STATE           as CurrentState,
    put.SUBMIT_DATE             as SubmitDate,
    put.APPROVAL_DATE           as ApprovalDate,
    put.EXPIRATION_DATE         as ExpirationDate,
    put.EFFECTIVE_DATE          as EffectiveDate,
    put.RENEWAL_DATE            as RenewalDate,
    put.AUTHORIZED_AMT          as AuthorizedAmt,
    put.ON_ORDER_AMT            as OnOrderAmt,
    put.USED_AMT                as UsedAmt,
    put.AVAILABLE_AMT           as AvailableAmt,
    put.PERCENT_LEFT            as PercentLeft,
    s.SPECIES_ID                as Species,
FROM PROTOCOL_USAGE_TEMP put
LEFT JOIN SEGMENT s ON put.SEGMENT_ID = s.SEGMENT_ID
/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT
    ID                      as EsigId,
    ANIMAL_DELIVERY_ID      as AnimalDelivery,
    ESIGNATURE_EVENT_ID     as EsigEvent,
    USER_PROFILE_ID.STAFF_ID.STAFF_FIRST_NAME
        || '|' || USER_PROFILE_ID.STAFF_ID.STAFF_LAST_NAME as UserProfile,
    ESIG_DATETIME           as EsigDateTime
FROM ANIMAL_DELIVERY_ESIG
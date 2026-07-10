/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

SELECT anm.ANIMAL_ID_NUMBER AS participantId,
       anm.BIRTH_DATE,
       anm.GENDER_ID AS gender,
       anm.SSB_ID.SPECIES_ID AS species,
       anm.BIRTH_DATE AS birth,
       anm.DATE_WEANED AS weanedDate,
       anm.DEATH_DATE AS death,
       anm.ANIMAL_DISPOSITION_ID AS disposition,
       anm.DESCRIPTION AS description,
       anm.TERMINATION_REASON_ID AS termination,
       anm.VENDOR_ANIMAL_NUMBER AS vendorNumber,
       anm.ACTIVE_YN AS activeStatus,
       anm.LOT_NUMBER_ID AS lotId,
       st.status AS calculated_status,
       alt.NAME AS damSire,
       altImplant.Name AS implantNumber,
       altOrigin.Name AS geographic_origin,
       altSrc.Name AS source,
       altCites.Name AS CITES,
       (CASE
            WHEN (ae.STAFF_ID.STAFF_FIRST_NAME IS NULL OR ae.STAFF_ID.STAFF_LAST_NAME IS NULL) THEN 'unknown'
            ELSE (trim(ae.STAFF_ID.STAFF_FIRST_NAME)
                || '|' || trim(ae.STAFF_ID.STAFF_LAST_NAME)) END)                  AS performedby,
       -- audit timestamp for modifications or animal event received for created
       COALESCE(MAX(CAST(adt.CHANGE_DATETIME AS TIMESTAMP)), ae.CREATED_DATETIME) AS modified
FROM Animal anm
LEFT JOIN AUDIT_TRAIL adt ON anm.ANIMAL_ID = substring(PRIMARY_KEY_VALUES, length('ANIMAL_ID = '))
AND adt.TABLE_NAME = 'ANIMAL'
LEFT JOIN q_status st ON st.Id = anm.ANIMAL_ID_NUMBER
LEFT JOIN ANIMAL_EVENT ae ON ae.ANIMAL_ID = anm.ANIMAL_ID AND EVENT_ID = 1  -- Received
LEFT JOIN ALTERNATE alt ON alt.ANIMAL_ID = anm.ANIMAL_ID AND alt.ALTERNATE_TYPE_ID = 7  -- Dam/sire
LEFT JOIN ALTERNATE altImplant ON altImplant.ANIMAL_ID = anm.ANIMAL_ID AND altImplant.ALTERNATE_TYPE_ID = 5 --Transponder number
LEFT JOIN ALTERNATE altOrigin ON altOrigin.ANIMAL_ID = anm.ANIMAL_ID AND altOrigin.ALTERNATE_TYPE_ID = 10  -- Country of origin
LEFT JOIN ALTERNATE altSrc ON altSrc.ANIMAL_ID = anm.ANIMAL_ID AND altSrc.ALTERNATE_TYPE_ID = 11  -- Source
LEFT JOIN ALTERNATE altCites ON altCites.ANIMAL_ID = anm.ANIMAL_ID AND altCites.ALTERNATE_TYPE_ID = 3  -- CITES number
WHERE anm.ANIMAL_DISPOSITION_ID IS NULL OR anm.ANIMAL_DISPOSITION_ID != 110  -- Invalid Id
GROUP BY anm.ANIMAL_ID_NUMBER,
    anm.BIRTH_DATE,
    anm.GENDER_ID,
    anm.SSB_ID.SPECIES_ID,
    anm.BIRTH_DATE,
    anm.DATE_WEANED,
    anm.DEATH_DATE,
    anm.ANIMAL_DISPOSITION_ID,
    anm.DESCRIPTION,
    anm.TERMINATION_REASON_ID,
    anm.VENDOR_ANIMAL_NUMBER,
    anm.ACTIVE_YN,
    anm.LOT_NUMBER_ID,
    ae.STAFF_ID.STAFF_FIRST_NAME,
    ae.STAFF_ID.STAFF_LAST_NAME,
    st.status,
    alt.NAME,
    altImplant.Name,
    altOrigin.Name,
    altSrc.Name,
    altCites.Name,
    ae.CREATED_DATETIME

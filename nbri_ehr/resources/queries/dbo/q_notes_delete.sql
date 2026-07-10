/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT substring(adt.PRIMARY_KEY_VALUES, 18) AS objectid,
       CAST(adt.CHANGE_DATETIME AS TIMESTAMP) AS modified
FROM AUDIT_TRAIL adt
WHERE adt.PRIMARY_KEY_VALUES LIKE '%ANIMAL_EVENT_ID%' AND
        adt.COLUMN_NAME = 'DELETE' AND
        (
            adt.REFERENCE LIKE '%CRL Shipment%' OR
            adt.REFERENCE LIKE '%Chimp Physical Exam%' OR
            adt.REFERENCE LIKE '%Pregnancy Examination%' OR
            adt.REFERENCE LIKE '%SALAR WP Shipment Staging%' OR
            adt.REFERENCE LIKE '%Vendor Record Attached%' OR
            adt.REFERENCE LIKE '%Medroxyprogesterone acetate tab%'
        )
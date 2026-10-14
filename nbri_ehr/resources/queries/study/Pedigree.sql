/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT

    d.id as Id,
    -- UNKNOWN is a placeholder parent; passed through, kinship would treat every animal recorded against it as siblings
    CASE WHEN UPPER(d.dam) = 'UNKNOWN' THEN NULL ELSE d.dam END as Dam,
    CASE WHEN UPPER(d.sire) = 'UNKNOWN' THEN NULL ELSE d.sire END as Sire,
    d.modified,

    CASE (d.id.demographics.gender.meaning)
        WHEN 'male' THEN 1
        WHEN 'female' THEN 2
        ELSE 3
        END AS gender,
    d.id.demographics.gender.meaning as gender_code,
    CASE (d.id.demographics.calculated_status)
        WHEN 'Alive' THEN 0
        ELSE 1
        END
        AS status,
    d.id.demographics.calculated_status as status_code,
    d.id.demographics.species.common_name as species,
    '' as Display,
    'Demographics' as source,
    d.modified

FROM study.demographics d
WHERE (d.Dam IS NOT NULL AND UPPER(d.Dam) <> 'UNKNOWN') OR (d.Sire IS NOT NULL AND UPPER(d.Sire) <> 'UNKNOWN')

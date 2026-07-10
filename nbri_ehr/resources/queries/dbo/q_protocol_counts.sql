/*
 * Copyright (c) 2022-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT pc.Protocol, pc.Species, COALESCE(pc.allowed, 0) as allowed, ssb.SSB_ID as ssb, eu.EUTHANASIA_TYPE_ID as Euthanasia FROM (
SELECT
       sub.Protocol,
       sub.Species,
       SUM(sub.Amt) as allowed,
       --LISTAGG(sub.Stress, ', ') as StressLevels
FROM
(
    SELECT sub1.Protocol, sub1.Species, sub2.STRESS_NAME AS Stress, sub2.AUTHORIZED_AMT AS Amt FROM
    (SELECT
        PROTOCOL_ID.PROTOCOL_ID     AS Protocol,
        SPECIES_ID.SPECIES_ID       AS Species
    FROM SEGMENT
    GROUP BY
        PROTOCOL_ID.PROTOCOL_ID,
        SPECIES_ID.SPECIES_ID
    ) sub1
    LEFT JOIN
        (SELECT s.SEGMENT_ID, s.SPECIES_ID.SPECIES_ID, s.PROTOCOL_ID.PROTOCOL_ID, ss.STRESS_ID.STRESS_NAME, ss.AUTHORIZED_AMT FROM SEGMENT s
            LEFT JOIN SEGMENT_STRESS ss ON ss.SEGMENT_ID = s.SEGMENT_ID) sub2
        ON sub2.PROTOCOL_ID = sub1.Protocol AND sub2.SPECIES_ID = sub1.Species AND sub2.AUTHORIZED_AMT > 0
) sub
GROUP BY
    sub.Protocol, sub.Species
) pc
LEFT JOIN SEGMENT_SSB ssb ON pc.Species = ssb.SEGMENT_ID.SPECIES_ID.SPECIES_ID AND pc.Protocol = ssb.SEGMENT_ID.PROTOCOL_ID.PROTOCOL_ID
LEFT JOIN SEGMENT_EUTHANASIA eu ON pc.Species = eu.SEGMENT_ID.SPECIES_ID.SPECIES_ID AND pc.Protocol = eu.SEGMENT_ID.PROTOCOL_ID.PROTOCOL_ID
WHERE Protocol IS NOT NULL AND Species IS NOT NULL
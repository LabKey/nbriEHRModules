/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
SELECT
    p.id,
    p.dam,
    'Observed' as damType,
    p.sire,
    'Observed' as sireType,
    CASE
        WHEN p.dam IS NOT NULL AND p.sire IS NOT NULL THEN 2
        WHEN p.dam IS NOT NULL OR p.sire IS NOT NULL THEN 1
        ELSE 0
        END as numParents
FROM (
    -- UNKNOWN is a placeholder parent; left in, every query joining on Id/parents would treat its offspring as siblings
    SELECT
        d.id,
        CASE WHEN UPPER(d.dam) = 'UNKNOWN' THEN NULL ELSE d.dam END as dam,
        CASE WHEN UPPER(d.sire) = 'UNKNOWN' THEN NULL ELSE d.sire END as sire
    FROM study.demographics d
) p

-- TODO: Incorporate fostering? Genetic testing?


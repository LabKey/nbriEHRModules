/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- Used in q_demographics
SELECT anm.ANIMAL_ID_NUMBER AS Id,
       anm.ANIMAL_DISPOSITION_ID.NAME,
       CASE WHEN anm.TERMINATION_REASON_ID.ID = 10 THEN 'Invalid'   -- Invalid Id
            WHEN anm.ANIMAL_DISPOSITION_ID IS NULL THEN 'Alive'
            WHEN anm.ANIMAL_DISPOSITION_ID IN (2, 3, 4) THEN 'Dead'  -- Died, Project Sacrifice, Moribund Sacrifice
            WHEN anm.ANIMAL_DISPOSITION_ID IN (5, 6) THEN 'Other'   -- Escaped, Misc
            ELSE 'Shipped' END AS status
FROM Animal anm
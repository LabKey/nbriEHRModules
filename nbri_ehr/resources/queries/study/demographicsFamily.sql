/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- parents rather than the raw columns, which drops the UNKNOWN placeholder parent
SELECT

d1.id,

d1.Id.parents.dam AS dam,

d1.Id.parents.sire AS sire,

dam.Id.parents.dam AS MaternalGranddam,

dam.Id.parents.sire AS MaternalGrandsire,

sire.Id.parents.dam AS PaternalGranddam,

sire.Id.parents.sire AS PaternalGrandsire,

d1.qcstate

FROM study.Demographics d1

LEFT JOIN study.Demographics dam
  ON (d1.Id.parents.dam = dam.id)

LEFT JOIN study.Demographics sire
  ON (d1.Id.parents.sire = sire.id)

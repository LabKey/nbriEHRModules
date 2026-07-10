/*
 * Copyright (c) 2021-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
SELECT substring(adt.PRIMARY_KEY_VALUES, length('ANIMAL_EVENT_ID = ')) AS objectid,
       CAST(adt.CHANGE_DATETIME AS TIMESTAMP) AS modified,
       adt.REFERENCE
FROM AUDIT_TRAIL adt
WHERE adt.PRIMARY_KEY_VALUES LIKE '%ANIMAL_EVENT_ID%' AND
    (adt.REFERENCE LIKE '%SRV -%' OR
    adt.REFERENCE LIKE '%CMV Draw%' OR
    adt.REFERENCE LIKE '%Hep B Draw%' OR
    adt.REFERENCE LIKE '%B Virus%' OR
    adt.REFERENCE LIKE '%Measles Positive%' OR
    adt.REFERENCE LIKE '%Measles Negative%' OR
    adt.REFERENCE LIKE '%Measles Equivocal%' OR
    (adt.REFERENCE LIKE '%SIV%' AND adt.REFERENCE NOT LIKE '%SIV Draw%') OR
    (adt.REFERENCE LIKE '%SRV%' AND adt.REFERENCE NOT LIKE '%SRV Draw%') OR
    (adt.REFERENCE LIKE '%STLV-1%' AND adt.REFERENCE NOT LIKE '%STLV-1 Draw%') OR
    adt.REFERENCE LIKE '%SA8%' OR
    adt.REFERENCE LIKE '%SA11%' OR
    (adt.REFERENCE LIKE '%Filovirus%' AND adt.REFERENCE NOT LIKE '%Filovirus Draw%') OR
    (adt.REFERENCE LIKE '%CMV%' AND adt.REFERENCE NOT LIKE '%CMV Draw%') OR
    adt.REFERENCE LIKE '%Foamy Virus%' OR
    adt.REFERENCE LIKE '%Rabies Positive%' OR
    adt.REFERENCE LIKE '%Rabies Negative%' OR
    adt.REFERENCE LIKE '%Rabies Equivocal%' OR
    adt.REFERENCE LIKE '%HIV AB Negative%' OR
    adt.REFERENCE LIKE '%HEP A%' OR
    adt.REFERENCE LIKE '%RSV AB%' OR
    adt.REFERENCE LIKE '%SV40%' OR
    adt.REFERENCE LIKE '%HIV PCR Qualitative%' OR
    adt.REFERENCE LIKE '%Rheumatoid Arthritis%' OR
    adt.REFERENCE LIKE '%HIV-1/HIV-2 AB%' OR
    adt.REFERENCE LIKE '%Para Influenza Virus%' OR
    adt.REFERENCE LIKE '%Ebola Virus%' OR
    adt.REFERENCE LIKE '%Marburg Virus%' OR
    adt.REFERENCE LIKE '%Polio Titer%' OR
    adt.REFERENCE LIKE '%HEP B%' OR
    adt.REFERENCE LIKE '%HEP C%' OR
    adt.REFERENCE LIKE '%Herpes Simplex Virus%' OR
    adt.REFERENCE LIKE '%HCV Genotyping%' OR
    adt.REFERENCE LIKE '%HEP E%' OR
    adt.REFERENCE LIKE '%NBRI Lab%' OR
    adt.REFERENCE LIKE '%Enterovirus%' OR
    adt.REFERENCE LIKE '%EBV%' OR
    adt.REFERENCE LIKE '%WNV%' OR
    adt.REFERENCE LIKE '%EMC AB%' OR
    adt.REFERENCE LIKE '%LCM AB%' OR
    adt.REFERENCE LIKE '%HCV Viral Load%' OR
    adt.REFERENCE LIKE '%HBV Viral Load%' OR
    adt.REFERENCE LIKE '%HIV 1/0/2 Abs-Reactive%' OR
    adt.REFERENCE LIKE '%HIV 1/0/2 Abs-NonReactive%' OR
    adt.REFERENCE LIKE '%HIV AB%' OR
    adt.REFERENCE LIKE '%Plasmodium%' OR
    adt.REFERENCE LIKE '%Anti-Noro Virus%' OR
    adt.REFERENCE LIKE '%HBV Genotyping%' OR
    adt.REFERENCE LIKE '%T. Cruzi%' OR
    adt.REFERENCE LIKE '%SVV%' OR
    adt.REFERENCE LIKE '%LCV%' OR
    adt.REFERENCE LIKE '%MRV%' OR
    adt.REFERENCE LIKE '%Introduction%' OR
    adt.REFERENCE LIKE '%SIV Indeterminate%' OR
    adt.REFERENCE LIKE '%Zika Virus%' OR
    adt.REFERENCE LIKE '%Tuberculosis (TB) Multiplex Negative%' OR
    adt.REFERENCE LIKE '%SHF Negative%' OR
    adt.REFERENCE LIKE '%SA6%' OR
    adt.REFERENCE LIKE '%K. PNEUMO Negative%' OR
    adt.REFERENCE LIKE '%STLV-2 Negative%' OR
    adt.REFERENCE LIKE '%MRSA Negative%' OR
    adt.REFERENCE LIKE '%KLEBSIELLA CULTURE%' OR
    adt.REFERENCE LIKE '%SRV-2%' OR
    adt.REFERENCE LIKE '%Malaria%' OR
    adt.REFERENCE LIKE '%SARS-CoV-2%' OR
    adt.REFERENCE LIKE '%Spike-FL%' OR
    adt.REFERENCE LIKE '%NP Equivocal%' OR
    adt.REFERENCE LIKE '%NP Negative%' OR
    adt.REFERENCE LIKE '%NP Positive%' OR
    adt.REFERENCE LIKE '%SCMV Positive%' OR
    adt.REFERENCE LIKE '%MTC Negative%' OR
    adt.REFERENCE LIKE '%Y. pseudo tubercul Negative%' ) AND
    adt.COLUMN_NAME = 'DELETE'
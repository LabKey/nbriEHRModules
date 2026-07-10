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
      adt.REFERENCE NOT LIKE '%Blood Sample Collection%' AND
    (
     adt.REFERENCE LIKE '%Procedure%' OR
     adt.REFERENCE LIKE '%Radiology%' OR
     adt.REFERENCE LIKE '%Ultrasonography%' OR
     adt.REFERENCE LIKE '%Endoscopy%' OR
     adt.REFERENCE LIKE '%Laparoscopy%' OR
     adt.REFERENCE LIKE '%Colonoscopy%' OR
     adt.REFERENCE LIKE '%Cleaning%' OR
     adt.REFERENCE LIKE '%Bandage%' OR
     adt.REFERENCE LIKE '%bandage%' OR
     adt.REFERENCE LIKE '%Debridement%' OR
     adt.REFERENCE LIKE '%Cast%' OR
     adt.REFERENCE LIKE '%Clip and clean%' OR
     adt.REFERENCE LIKE '%Flush wound%' OR
     adt.REFERENCE LIKE '%Furazolidone%' OR
     adt.REFERENCE LIKE '%Hydrotherapy%' OR
     adt.REFERENCE LIKE '%Preparation H application%' OR
     adt.REFERENCE LIKE '%Purse string suture%' OR
     adt.REFERENCE LIKE '%Rectal prolapse replacement%' OR
     adt.REFERENCE LIKE '%Splint%' OR
     adt.REFERENCE LIKE '%Suture%' OR
     adt.REFERENCE LIKE '%TAO application%' OR
     adt.REFERENCE LIKE '%Trypzyme application%' OR
     adt.REFERENCE LIKE '%Hepatic Biopsy-Tru Cut%' OR
     adt.REFERENCE LIKE '%Biopsy%' OR
     adt.REFERENCE LIKE '%Bone Marrow Aspirates%' OR
     adt.REFERENCE LIKE '%Splenectomy%' OR
     adt.REFERENCE LIKE '%Thoracocentesis%' OR
     adt.REFERENCE LIKE '%Abdominocentesis%' OR
     adt.REFERENCE LIKE '%Catheter/Canula Placement%' OR
     adt.REFERENCE LIKE '%Aspirate-Specify%' OR
     adt.REFERENCE LIKE '%Ultrasound%' OR
     adt.REFERENCE LIKE '%Embryo Transfer%' OR
     adt.REFERENCE LIKE '%Oocyte Collection%' OR
     adt.REFERENCE LIKE '%Electroejaculation%' OR
     adt.REFERENCE LIKE '%Cesarian section%' OR
     adt.REFERENCE LIKE '%Vasectomy%' OR
     adt.REFERENCE LIKE '%Pulpectomy%' OR
     adt.REFERENCE LIKE '%Pulpotomy%' OR
     adt.REFERENCE LIKE '%Stool Analysis by Baermann Technique%' OR
     adt.REFERENCE LIKE '%Pinworm test (Cellophane tape method)%' OR
     adt.REFERENCE LIKE '%Root Canal%' OR
     adt.REFERENCE LIKE '%Canine Blunting%' OR
     adt.REFERENCE LIKE '%Prophylactic Cleaning%' OR
     adt.REFERENCE LIKE '%Extraction%' OR
     adt.REFERENCE LIKE '%Oophorectomy%' OR
     adt.REFERENCE LIKE '%Transponder Implantation%' OR
     adt.REFERENCE LIKE '%Tissue Culture%' OR
     adt.REFERENCE LIKE '%Histopathology Tissue Collection%' OR
     adt.REFERENCE LIKE '%IUD insertion%' OR
     adt.REFERENCE LIKE '%Chair Training%' OR
     adt.REFERENCE LIKE '%Vascular access port placement%' OR
     adt.REFERENCE LIKE '%Vascular access port removal%' OR
     adt.REFERENCE LIKE '%Disqualified from chair study%' OR
     adt.REFERENCE LIKE '%Urine Collection (Cystocentesis)%' OR
     adt.REFERENCE LIKE '%Sample Collection%' OR
     adt.REFERENCE LIKE '%Specimen Collection%' OR
     adt.REFERENCE LIKE '%Biopsy Collection%' OR
     adt.REFERENCE LIKE '%Bronchial Lavage Sample Collection%' OR
     adt.REFERENCE LIKE '%Stool (Pan) Collection%' OR
     adt.REFERENCE LIKE '%Stool (Rectal Swab) Collection%' OR
     adt.REFERENCE LIKE '%CSF tap%' OR
     adt.REFERENCE LIKE '%Cervicovaginal Sample%' OR
     adt.REFERENCE LIKE '%Nasal swab%' OR
     adt.REFERENCE LIKE '%Quarantine - arrival%' OR
     adt.REFERENCE LIKE '%Quarantine - release%' ) AND
    adt.COLUMN_NAME = 'DELETE'
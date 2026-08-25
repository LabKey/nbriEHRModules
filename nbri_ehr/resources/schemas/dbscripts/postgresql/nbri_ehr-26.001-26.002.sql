/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
ALTER TABLE nbri_ehr.Conception DROP COLUMN ConceptTermDate;

-- Drop the tables carried over from the legacy system that nothing in the module reads or writes. Conception is the
-- only table left in the schema afterward. Each DROP also removes that table's primary key and its
-- IX_..._Container index, so no separate DROP INDEX is needed.

DROP TABLE IF EXISTS nbri_ehr.CageCardHistory;
DROP TABLE IF EXISTS nbri_ehr.CageCard;
DROP TABLE IF EXISTS nbri_ehr.AnimalDeliveryEsig;
DROP TABLE IF EXISTS nbri_ehr.AnimalReqOrderEsig;
DROP TABLE IF EXISTS nbri_ehr.AnimalDelivery;
DROP TABLE IF EXISTS nbri_ehr.AnimalReqOrder;
DROP TABLE IF EXISTS nbri_ehr.Lot;
DROP TABLE IF EXISTS nbri_ehr.AnimalShipment;
DROP TABLE IF EXISTS nbri_ehr.AnimalVendor;
DROP TABLE IF EXISTS nbri_ehr.ShipTo;

DROP TABLE IF EXISTS nbri_ehr.Account;
DROP TABLE IF EXISTS nbri_ehr.Department;

DROP TABLE IF EXISTS nbri_ehr.ProtocolStress;
DROP TABLE IF EXISTS nbri_ehr.Stress;
DROP TABLE IF EXISTS nbri_ehr.ProtocolProcedures;
DROP TABLE IF EXISTS nbri_ehr.ProtocolEsig;
DROP TABLE IF EXISTS nbri_ehr.ProtocolUsage;

DROP TABLE IF EXISTS nbri_ehr.LocationsMapping;
DROP TABLE IF EXISTS nbri_ehr.Locations;
DROP TABLE IF EXISTS nbri_ehr.LocationTypes;

DROP TABLE IF EXISTS nbri_ehr.QuestionResponse;
DROP TABLE IF EXISTS nbri_ehr.Question;
DROP TABLE IF EXISTS nbri_ehr.DeletedRecord;
DROP TABLE IF EXISTS nbri_ehr.Staff;
DROP TABLE IF EXISTS nbri_ehr.IdHistory;

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- UQ_PROTOCOLAMENDMENT_Number's index leads with Container, so it already serves Container lookups.
DROP INDEX IF EXISTS nbri_ehr.IX_Nbri_Ehr_ProtocolAmendment_Container;

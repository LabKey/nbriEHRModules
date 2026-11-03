/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- An IACUC protocol amendment; its ehr.protocol_counts rows are in effect from date through enddate.
CREATE TABLE nbri_ehr.ProtocolAmendment
(
    rowid               SERIAL NOT NULL,
    protocol            VARCHAR(200),
    amendmentNumber     INTEGER,
    -- Named 'date' so the shared EHR triggers apply their date normalization and future-date guards.
    date                TIMESTAMP,
    enddate             TIMESTAMP,
    remark              TEXT,
    taskid              ENTITYID,
    QCState             INTEGER,
    objectid            ENTITYID,
    Container           entityId NOT NULL,
    Created             TIMESTAMP,
    CreatedBy           USERID,
    Modified            TIMESTAMP,
    ModifiedBy          USERID,
    CONSTRAINT PK_PROTOCOLAMENDMENT PRIMARY KEY (rowid),
    CONSTRAINT UQ_PROTOCOLAMENDMENT_Number UNIQUE (Container, protocol, amendmentNumber),
    CONSTRAINT FK_PROTOCOLAMENDMENT_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_ProtocolAmendment_Container ON nbri_ehr.ProtocolAmendment (Container);
CREATE INDEX IX_Nbri_Ehr_ProtocolAmendment_ObjectId ON nbri_ehr.ProtocolAmendment (objectid);

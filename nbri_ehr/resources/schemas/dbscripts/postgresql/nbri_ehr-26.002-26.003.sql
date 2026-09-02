/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- IACUC protocol amendments and renewals. An amendment is an append-only record; approving one rewrites the protocol's
-- date span and closes out the ehr.protocol_counts rows it supersedes.

CREATE TABLE nbri_ehr.ProtocolAmendment
(
    rowid               SERIAL NOT NULL,
    protocol            VARCHAR(200),
    amendmentType       INTEGER,
    status              INTEGER,
    -- Cycle counts renewals of the protocol; revision counts amendments within a cycle.
    cycle               INTEGER,
    revision            INTEGER,
    -- Named 'date' so the shared EHR triggers apply their date normalization and future-date guards.
    date                TIMESTAMP,
    submittedDate       TIMESTAMP,
    approvedDate        TIMESTAMP,
    effectiveDate       TIMESTAMP,
    newExpirationDate   TIMESTAMP,
    amendmentReason     TEXT,
    remark              TEXT,
    taskid              ENTITYID,
    QCState             INTEGER,
    -- Stable key for the record; the link to its count rows is taskid.
    objectid            ENTITYID,
    Container           entityId NOT NULL,
    Created             TIMESTAMP,
    CreatedBy           USERID,
    Modified            TIMESTAMP,
    ModifiedBy          USERID,
    CONSTRAINT PK_PROTOCOLAMENDMENT PRIMARY KEY (rowid),
    CONSTRAINT FK_PROTOCOLAMENDMENT_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_ProtocolAmendment_Container ON nbri_ehr.ProtocolAmendment (Container);
CREATE INDEX IX_Nbri_Ehr_ProtocolAmendment_Protocol ON nbri_ehr.ProtocolAmendment (Container, protocol);
CREATE INDEX IX_Nbri_Ehr_ProtocolAmendment_ObjectId ON nbri_ehr.ProtocolAmendment (objectid);

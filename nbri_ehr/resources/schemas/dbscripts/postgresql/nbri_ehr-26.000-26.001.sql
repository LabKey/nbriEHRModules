/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
CREATE TABLE nbri_ehr.Conception
(
    RowId               SERIAL NOT NULL,
    ConceptId           VARCHAR(100),
    ConceptDate         TIMESTAMP,
    ConceptTermDate     TIMESTAMP,
    Remark              TEXT,
    Dam                 VARCHAR(100),
    Sire                VARCHAR(100),
    BreedingType        VARCHAR(100),
    TaskId              ENTITYID,
    QCState             INTEGER,
    Container           entityId NOT NULL,
    Created             TIMESTAMP,
    CreatedBy           USERID,
    Modified            TIMESTAMP,
    ModifiedBy          USERID,
    CONSTRAINT PK_CONCEPTION PRIMARY KEY (RowId),
    CONSTRAINT UQ_CONCEPTION_ConceptId UNIQUE (ConceptId),
    CONSTRAINT FK_CONCEPTION_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Conception_Container ON nbri_ehr.Conception (Container);

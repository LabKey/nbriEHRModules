/*
 * Copyright (c) 2024-2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
CREATE SCHEMA nbri_ehr;

CREATE TABLE nbri_ehr.LocationTypes
(
    RowId           SERIAL NOT NULL,
    Created         TIMESTAMP,
    CreatedBy       USERID,
    Modified        TIMESTAMP,
    ModifiedBy      USERID,
    Container       entityId NOT NULL,

    NAME            TEXT,
    LocationTypeId      INTEGER,

    CONSTRAINT PK_LocationTypes PRIMARY KEY (RowId),
    CONSTRAINT FK_LocationTypes_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_nbri_ehr_LocationTypes_Container ON nbri_ehr.LocationTypes (Container);

CREATE TABLE nbri_ehr.Locations
(
    RowId           SERIAL NOT NULL,
    Created         TIMESTAMP,
    CreatedBy       USERID,
    Modified        TIMESTAMP,
    ModifiedBy      USERID,
    Container       entityId NOT NULL,

    NAME            TEXT,
    LocationId      INTEGER,
    LocationType    INTEGER,

    CONSTRAINT PK_Locations PRIMARY KEY (RowId),
    CONSTRAINT FK_Locations_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_nbri_ehr_Locations_Container ON nbri_ehr.Locations (Container);

CREATE TABLE nbri_ehr.LocationsMapping
(
    RowId           SERIAL NOT NULL,
    Created         TIMESTAMP,
    CreatedBy       USERID,
    Modified        TIMESTAMP,
    ModifiedBy      USERID,
    Container       entityId NOT NULL,

    ParentLocation  INTEGER,
    ChildLocation   INTEGER,
    NAME            TEXT,

    CONSTRAINT PK_LocationsMapping PRIMARY KEY (RowId),
    CONSTRAINT FK_LocationsMapping_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_nbri_ehr_LocationsMapping_Container ON nbri_ehr.LocationsMapping (Container);

CREATE TABLE nbri_ehr.Staff
(
    staffId             INTEGER NOT NULL,
    email               TEXT,
    firstName           TEXT,
    lastName            TEXT,
    middleName          TEXT,
    displayName         TEXT,
    hireDate            TIMESTAMP,
    lastEmployDate      TIMESTAMP,
    officePhone         TEXT,
    officePhoneExt      TEXT,
    officeFax           TEXT,
    homePhone           TEXT,
    beeperPhone         TEXT,
    cellPhone           TEXT,
    emergencyContact    TEXT,
    emergencyPhone      TEXT,
    homeAddress1        TEXT,
    homeAddress2        TEXT,
    homeCity            TEXT,
    homeState           TEXT,
    homeZip             TEXT,
    homeZipExt          TEXT,
    homeCountry         TEXT,
    officeAddress1      TEXT,
    officeAddress2      TEXT,
    officeCity          TEXT,
    officeState         TEXT,
    officeZip           TEXT,
    officeZipExt        TEXT,
    officeCountry       TEXT,
    birthDate           TIMESTAMP,
    hsName              TEXT,
    collegeGradDate     TEXT,
    collegeDegree       TEXT,
    collegeMajor        TEXT,
    collegeName         TEXT,
    supervisor          INTEGER,
    positionName        TEXT,
    positionDesc        TEXT,
    active              BOOLEAN,
    Container           entityId NOT NULL,
    CONSTRAINT PK_STAFF PRIMARY KEY (staffId),
    CONSTRAINT FK_STAFF_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_nbri_ehr_Staff_Container ON nbri_ehr.Staff (Container);

CREATE TABLE nbri_ehr.Lot
(
    LotId               INTEGER,
    AnimalShipment      INTEGER,
    Date                TIMESTAMP,
    LotSequence         INTEGER,
    Container           entityId NOT NULL,
    Created             TIMESTAMP,
    CreatedBy           USERID,
    Modified            TIMESTAMP,
    ModifiedBy          USERID,
    CONSTRAINT PK_LOT PRIMARY KEY (LotId),
    CONSTRAINT FK_LOT_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Lot_Container ON nbri_ehr.Lot (Container);

CREATE TABLE nbri_ehr.AnimalShipment
(
    AnimalShipmentId    INTEGER,
    ReceivedByStaff     USERID,
    Cage                TEXT,
    Room                TEXT,
    Building            TEXT,
    Area                TEXT,
    AnimalDelivery      INTEGER,
    LotNumber           INTEGER,
    ReceivedDate        TIMESTAMP,
    BirthDate           TIMESTAMP,
    MalesReceived       INTEGER,
    FemalesReceived     INTEGER,
    EitherReceived      INTEGER,
    CanUnreceive        BOOLEAN,
    CostCenter          INTEGER,
    Dam                 TEXT,
    Sire                TEXT,
    RecordedDate        TIMESTAMP,
    RecordedBy          TEXT,
    Container           entityId NOT NULL,
    Created             TIMESTAMP,
    CreatedBy           USERID,
    Modified            TIMESTAMP,
    ModifiedBy          USERID,
    CONSTRAINT PK_ANIMALSHIPMENT PRIMARY KEY (AnimalShipmentId),
    CONSTRAINT FK_ANIMALSHIPMENT_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Animal_Shipment_Container ON nbri_ehr.AnimalShipment (Container);

CREATE TABLE nbri_ehr.AnimalDelivery
(
    AnimalDeliveryId        INTEGER,
    AnimalShipment          INTEGER,
    ShipTo                  INTEGER,
    cage                    TEXT,
    room                    TEXT,
    building                TEXT,
    area                    TEXT,
    AnimalReqOrder          INTEGER,
    DeliveryState           INTEGER,
    DeliveryNumber          INTEGER,
    ExpectedDate            TIMESTAMP,
    Project                 INTEGER,
    BillToAccount           INTEGER,
    BillToStaff             USERID,
    PerDiemAccount          INTEGER,
    PerDiemStaff            USERID,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_ANIMALDELIVERY PRIMARY KEY (AnimalDeliveryId),
    CONSTRAINT FK_ANIMALDELIVER_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Animal_Delivery_Container ON nbri_ehr.AnimalDelivery (Container);

CREATE TABLE nbri_ehr.AnimalDeliveryEsig
(
    EsigId                  INTEGER,
    AnimalDelivery          INTEGER,
    EsigEvent               INTEGER,
    UserProfile             USERID,
    EsigDateTime            TIMESTAMP,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_ANIMALDELIVERYESIG PRIMARY KEY (EsigId),
    CONSTRAINT FK_ANIMALDELIVERESIG_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Animal_Delivery_Esig_Container ON nbri_ehr.AnimalDeliveryEsig (Container);

CREATE TABLE nbri_ehr.AnimalReqOrder
(
    AnimalReqOrderId        INTEGER,
    AnimalVendor            INTEGER,
    RequisitionerStaff      USERID,
    RequestNumber           TEXT,
    RequisitionNumber       TEXT,
    Bill                    BOOLEAN,
    ShippingContact         TEXT,
    CreateDate              TIMESTAMP,
    ReqOrderType            INTEGER,
    ReqOrderState           INTEGER,
    BillToAccount           INTEGER,
    BillToStaff             USERID,
    PerDiemAccount          INTEGER,
    PerDiemStaff            USERID,
    SubmittedByStaff        USERID,
    ApprovedByStaff         USERID,
    SubmittedDate           TIMESTAMP,
    ApprovedDate            TIMESTAMP,
    Segment                 INTEGER,
    Project                 INTEGER,
    SiteCage                TEXT,
    SiteRoom                TEXT,
    SiteBuilding            TEXT,
    SiteArea                TEXT,
    CreatedByStaff          USERID,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_ANIMALREQORDER PRIMARY KEY (AnimalReqOrderId),
    CONSTRAINT FK_ANIMALREQORDER_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Animal_Req_Order_Container ON nbri_ehr.AnimalReqOrder (Container);

CREATE TABLE nbri_ehr.AnimalReqOrderEsig
(
    EsigId                  INTEGER,
    AnimalReqOrder          INTEGER,
    EsigEvent               INTEGER,
    UserProfile             USERID,
    EsigDateTime            TIMESTAMP,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_ANIMALREQORDERESIG PRIMARY KEY (EsigId),
    CONSTRAINT FK_ANIMALREQORDERESIG_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Animal_Req_Order_Esig_Container ON nbri_ehr.AnimalReqOrderEsig (Container);

CREATE TABLE nbri_ehr.AnimalVendor
(
    AnimalVendorId          INTEGER,
    VendorApprovalCode      INTEGER,
    VendorName              TEXT,
    StreetAddress1          TEXT,
    StreetAddress2          TEXT,
    City                    TEXT,
    StateProv               TEXT,
    Country                 TEXT,
    Zip                     TEXT,
    ZipExt                  TEXT,
    PhoneNumber             TEXT,
    FaxNumber               TEXT,
    Comments                TEXT,
    InternalVendor          TEXT,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_ANIMALVENDOR PRIMARY KEY (AnimalVendorId),
    CONSTRAINT FK_ANIMALVENDOR_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Animal_Vendor_Container ON nbri_ehr.AnimalVendor (Container);

CREATE TABLE nbri_ehr.ShipTo
(
    ShipToId                INTEGER,
    Name                    TEXT,
    Country                 TEXT,
    City                    TEXT,
    StreetAddress1          TEXT,
    StreetAddress2          TEXT,
    StateProv               TEXT,
    Zip                     TEXT,
    ZipExt                  TEXT,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_SHIPTO PRIMARY KEY (ShipToId),
    CONSTRAINT FK_SHIPTO_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Ship_To_Container ON nbri_ehr.ShipTo (Container);

ALTER TABLE nbri_ehr.Staff
    ADD COLUMN Created TIMESTAMP,
    ADD COLUMN CreatedBy USERID,
    ADD COLUMN Modified TIMESTAMP,
    ADD COLUMN ModifiedBy USERID;

CREATE TABLE nbri_ehr.ProtocolEsig
(
    EsigId                  INTEGER,
    Protocol                INTEGER,
    EsigEvent               INTEGER,
    UserProfile             USERID,
    EsigDateTime            TIMESTAMP,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_PROTOCOLESIG PRIMARY KEY (EsigId),
    CONSTRAINT FK_PROTOCOLESIG_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Protocol_Esig_Container ON nbri_ehr.ProtocolEsig (Container);

CREATE TABLE nbri_ehr.ProtocolUsage
(
    Protocol                TEXT,
    UserReferenceNumber     TEXT,
    ShortTitle              TEXT,
    Investigator            TEXT,
    ProtocolType            TEXT,
    CurrentState            TEXT,
    SubmitDate              TIMESTAMP,
    ApprovalDate            TIMESTAMP,
    ExpirationDate          TIMESTAMP,
    EffectiveDate           TIMESTAMP,
    RenewalDate             TIMESTAMP,
    AuthorizedAmt           FLOAT,
    OnOrderAmt              FLOAT,
    UsedAmt                 FLOAT,
    AvailableAmt            FLOAT,
    PercentLeft             FLOAT,
    SegmentId               INTEGER,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT FK_PROTOCOLUSAGE_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Protocol_Usage_Container ON nbri_ehr.ProtocolUsage (Container);

CREATE TABLE nbri_ehr.Account
(
    AccountId               INTEGER,
    AccountNumber           TEXT,
    Department              INTEGER,
    CostType                INTEGER,
    ExpenseClass            INTEGER,
    ProjectCode             INTEGER,
    Description             TEXT,
    Active                  BOOLEAN,
    Cage                    TEXT,
    Room                    TEXT,
    Building                TEXT,
    Area                    TEXT,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_ACCOUNT PRIMARY KEY (AccountId),
    CONSTRAINT FK_ACCOUNT_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Account_Container ON nbri_ehr.Account (Container);


CREATE TABLE nbri_ehr.Department
(
    DepartmentId            INTEGER,
    Name                    TEXT,
    Description             TEXT,
    ParentDepartment        INTEGER,
    Staff                   USERID,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_DEPARTMENT PRIMARY KEY (DepartmentId),
    CONSTRAINT FK_DEPARTMENT_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Department_Container ON nbri_ehr.Department (Container);

ALTER TABLE nbri_ehr.AnimalShipment ADD COLUMN Floor VARCHAR;
ALTER TABLE nbri_ehr.AnimalDelivery ADD COLUMN Floor VARCHAR;

CREATE TABLE nbri_ehr.IdHistory
(
    Id                      VARCHAR,
    Name                    VARCHAR,
    Type                    INTEGER,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_IDHISTORY PRIMARY KEY (Id, Name),
    CONSTRAINT FK_IDHISTORY_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_ID_History_Container ON nbri_ehr.IdHistory (Container);

CREATE TABLE nbri_ehr.DeletedRecord
(
    Id                      INTEGER,
    Protocol                VARCHAR,
    UserReferenceNumber     VARCHAR,
    Investigator            VARCHAR,
    Title                   VARCHAR,
    EsigEvent               INTEGER,
    EsigUser                USERID,
    EsigDate                TIMESTAMP,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_DELETEDRECORD PRIMARY KEY (Id),
    CONSTRAINT FK_DELETEDRECORD_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Deleted_Record_Container ON nbri_ehr.DeletedRecord (Container);

CREATE TABLE nbri_ehr.Question
(
    QuestionId              INTEGER,
    Type                    VARCHAR,
    Text                    VARCHAR,
    CreationDate            TIMESTAMP,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_QUESTION PRIMARY KEY (QuestionId),
    CONSTRAINT FK_QUESTION_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Question_Container ON nbri_ehr.Question (Container);

CREATE TABLE nbri_ehr.QuestionResponse
(
    QuestionResponseId      INTEGER,
    Protocol                INTEGER,
    Response                VARCHAR,
    Question                INTEGER,
    Questionnaire           INTEGER,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_QUESTIONRESPONSE PRIMARY KEY (QuestionResponseId),
    CONSTRAINT FK_QUESTIONRESPONSE_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Question_Response_Container ON nbri_ehr.QuestionResponse (Container);

ALTER TABLE nbri_ehr.AnimalVendor ADD COLUMN VendorProductionLocation INTEGER;

ALTER TABLE nbri_ehr.ProtocolUsage DROP COLUMN SegmentId;
ALTER TABLE nbri_ehr.ProtocolUsage ADD COLUMN Species integer;

ALTER TABLE nbri_ehr.AnimalReqOrder DROP COLUMN Segment;
ALTER TABLE nbri_ehr.AnimalReqOrder ADD COLUMN Protocol integer;
ALTER TABLE nbri_ehr.AnimalReqOrder ADD COLUMN Species integer;

ALTER TABLE nbri_ehr.Account DROP COLUMN cage;
ALTER TABLE nbri_ehr.Account DROP COLUMN room;
ALTER TABLE nbri_ehr.Account DROP COLUMN building;

CREATE TABLE nbri_ehr.ProtocolStress
(
    RowId                   SERIAL NOT NULL,
    Protocol                INTEGER,
    Species                 VARCHAR,
    Stress                  INTEGER,
    Allowed                 INTEGER,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_PROTOCOLSTRESS PRIMARY KEY (RowId),
    CONSTRAINT FK_PROTOCOLSTRESS_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_ProtocolStress_Container ON nbri_ehr.ProtocolStress (Container);

CREATE TABLE nbri_ehr.Stress
(
    StressId                INTEGER,
    Name                    VARCHAR,
    Description             VARCHAR,
    Ranking                 INTEGER,
    RegulatoryStressLevel   INTEGER,
    Active                  BOOLEAN,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_STRESS PRIMARY KEY (StressId),
    CONSTRAINT FK_STRESS_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Stress_Container ON nbri_ehr.Stress (Container);

CREATE TABLE nbri_ehr.ProtocolProcedures
(
    RowId                   SERIAL NOT NULL,
    Protocol                INTEGER,
    Species                 VARCHAR,
    Procedure               VARCHAR,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_PROTOCOLPROCEDURES PRIMARY KEY (RowId),
    CONSTRAINT FK_PROTOCOLPROCEDURES_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_ProtocolProcedures_Container ON nbri_ehr.ProtocolProcedures (Container);

CREATE TABLE nbri_ehr.CageCard
(
    CageCardId              INTEGER,
    Protocol                INTEGER,
    Species                 INTEGER,
    CardFormat              INTEGER,
    CostCenter              INTEGER,
    GenerationDate          TIMESTAMP,
    UpdateDate              TIMESTAMP,
    NumberOfAnimals         INTEGER,
    Cage                    INTEGER,
    Room                    INTEGER,
    Floor                   INTEGER,
    Building                INTEGER,
    Area                    INTEGER,
    Account                 INTEGER,
    AccountStaff            USERID,
    CensusActivityStatus    VARCHAR,
    CensusActivityDate      TIMESTAMP,
    AnimalDelivery          INTEGER,
    AnimalRequestedByStaff  USERID,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_CAGECARD PRIMARY KEY (CageCardId),
    CONSTRAINT FK_CAGECARD_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Cage_Card_Container ON nbri_ehr.CageCard (Container);

CREATE TABLE nbri_ehr.CageCardHistory
(
    RowId                   SERIAL NOT NULL,
    CageCardId              INTEGER,
    AnimalDelivery          INTEGER,
    Protocol                INTEGER,
    Species                 INTEGER,
    CardFormat              INTEGER,
    CostCenter              INTEGER,
    GenerationDate          TIMESTAMP,
    UpdateDate              TIMESTAMP,
    NumberOfAnimals         INTEGER,
    Cage                    INTEGER,
    Room                    INTEGER,
    Floor                   INTEGER,
    Building                INTEGER,
    Area                    INTEGER,
    Account                 INTEGER,
    AccountStaff            USERID,
    CensusActivityStatus    VARCHAR,
    CensusActivityDate      TIMESTAMP,
    CensusStartDate         TIMESTAMP,
    CensusStopDate          TIMESTAMP,
    TransferDate            TIMESTAMP,
    ActionDate              TIMESTAMP,
    RetrospectiveStress     INTEGER,
    AnimalRequestedByStaff  USERID,
    Container               entityId NOT NULL,
    Created                 TIMESTAMP,
    CreatedBy               USERID,
    Modified                TIMESTAMP,
    ModifiedBy              USERID,
    CONSTRAINT PK_CAGECARDHISTORY PRIMARY KEY (RowId),
    CONSTRAINT FK_CAGECARDHISTORY_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_Cage_Card_History_Container ON nbri_ehr.CageCardHistory (Container);

CREATE TABLE nbri_ehr.CasesTemp
(
    Id                  TEXT,
    date                TIMESTAMP,
    enddate             TIMESTAMP,
    objectid            TEXT,
    category            TEXT,
    openDiagnosis       TEXT,
    closeDiagnosis      TEXT,
    openRemark          TEXT,
    closeRemark         TEXT,
    attachmentFile      TEXT,
    performedby         USERID
);

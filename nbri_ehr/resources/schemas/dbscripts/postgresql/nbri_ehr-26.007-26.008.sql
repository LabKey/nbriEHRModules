/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- Barcode codes for study.dailyHealthLog. observationCategory/observationScore, when set, make the code create that clinical observation.
CREATE TABLE nbri_ehr.dailyHealthCode
(
    rowid               SERIAL NOT NULL,
    code                VARCHAR(7) NOT NULL,
    name                VARCHAR(200),
    description         VARCHAR(4000),
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    observationCategory VARCHAR(200),
    observationScore    VARCHAR(200),
    Container           entityId NOT NULL,
    Created             TIMESTAMP,
    CreatedBy           USERID,
    Modified            TIMESTAMP,
    ModifiedBy          USERID,
    CONSTRAINT PK_DAILYHEALTHCODE PRIMARY KEY (rowid),
    CONSTRAINT UQ_DAILYHEALTHCODE_Code UNIQUE (Container, code),
    CONSTRAINT FK_DAILYHEALTHCODE_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);

CREATE TABLE nbri_ehr.dailyHealthUrgentGroup
(
    rowid               SERIAL NOT NULL,
    name                VARCHAR(200) NOT NULL,
    description         VARCHAR(4000) NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    Container           entityId NOT NULL,
    Created             TIMESTAMP,
    CreatedBy           USERID,
    Modified            TIMESTAMP,
    ModifiedBy          USERID,
    CONSTRAINT PK_DAILYHEALTHURGENTGROUP PRIMARY KEY (rowid),
    CONSTRAINT UQ_DAILYHEALTHURGENTGROUP_Name UNIQUE (Container, name),
    CONSTRAINT FK_DAILYHEALTHURGENTGROUP_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);

CREATE TABLE nbri_ehr.dailyHealthUrgentGroupCode
(
    rowid               SERIAL NOT NULL,
    groupId             INTEGER NOT NULL,
    sortOrder           INTEGER NOT NULL,
    code                VARCHAR(7) NOT NULL,
    Container           entityId NOT NULL,
    Created             TIMESTAMP,
    CreatedBy           USERID,
    Modified            TIMESTAMP,
    ModifiedBy          USERID,
    CONSTRAINT PK_DAILYHEALTHURGENTGROUPCODE PRIMARY KEY (rowid),
    CONSTRAINT UQ_DAILYHEALTHURGENTGROUPCODE_Code UNIQUE (groupId, code),
    CONSTRAINT FK_DAILYHEALTHURGENTGROUPCODE_Group FOREIGN KEY (groupId) REFERENCES nbri_ehr.dailyHealthUrgentGroup (rowid),
    -- Keyed on Container too, so a group can only hold codes from its own folder.
    CONSTRAINT FK_DAILYHEALTHURGENTGROUPCODE_Code FOREIGN KEY (Container, code) REFERENCES nbri_ehr.dailyHealthCode (Container, code),
    CONSTRAINT FK_DAILYHEALTHURGENTGROUPCODE_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_DailyHealthUrgentGroupCode_Code ON nbri_ehr.dailyHealthUrgentGroupCode (Container, code);

-- Loads into the EHR study container after startup; nbri_ehr-populateDailyHealthCodes.view fills folders set up later.
SELECT core.executeJavaUpgradeCode('importFromTsv;nbri_ehr;dailyHealthCode;data/dailyHealthCode.tsv');

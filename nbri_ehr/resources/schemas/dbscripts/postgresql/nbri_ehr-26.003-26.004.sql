/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
-- One row per necropsy: the request details and the workflow status shared by all three necropsy forms.
CREATE TABLE nbri_ehr.necropsyTask
(
    rowId               SERIAL   NOT NULL,
    objectid            entityid NOT NULL,
    Task                TEXT,
    Date                TIMESTAMP,
    AssignedTo          USERID,
    SubmittingPI        VARCHAR(100),
    SubmittingVet       USERID,
    SubmittingVetCell   VARCHAR(100),
    PriorityCode        VARCHAR(100),
    Sensitive           BOOLEAN,
    SensitiveReason     VARCHAR(1000),
    status              VARCHAR(200),
    taskid              ENTITYID,
    LSID                LSIDtype,
    QCState             INT,
    Created             TIMESTAMP,
    CreatedBy           USERID,
    Modified            TIMESTAMP,
    ModifiedBy          USERID,
    Container           entityId NOT NULL,

    CONSTRAINT PK_NBRI_EHR_NECROPSY_TASK PRIMARY KEY (objectid),
    CONSTRAINT FK_NBRI_EHR_NECROPSY_TASK_CONTAINER FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_NBRI_EHR_NECROPSY_TASK_CONTAINER ON nbri_ehr.necropsyTask (Container);
CREATE INDEX IX_NBRI_EHR_NECROPSY_TASK_TASKID ON nbri_ehr.necropsyTask (taskid);

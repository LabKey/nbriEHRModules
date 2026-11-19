/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */

-- A group's codes must appear in sortOrder, so renumber any group with tied positions, keeping their entry order.
UPDATE nbri_ehr.dailyHealthUrgentGroupCode c
SET sortOrder = r.position
FROM (
    SELECT rowid, ROW_NUMBER() OVER (PARTITION BY groupId ORDER BY sortOrder, rowid) AS position
    FROM nbri_ehr.dailyHealthUrgentGroupCode
    WHERE groupId IN (SELECT groupId FROM nbri_ehr.dailyHealthUrgentGroupCode GROUP BY groupId, sortOrder HAVING COUNT(*) > 1)
) r
WHERE c.rowid = r.rowid;

ALTER TABLE nbri_ehr.dailyHealthUrgentGroupCode ADD CONSTRAINT UQ_DAILYHEALTHURGENTGROUPCODE_SortOrder UNIQUE (groupId, sortOrder);

-- Keyed on Container too, so a group can only hold rows from its own folder.
ALTER TABLE nbri_ehr.dailyHealthUrgentGroup ADD CONSTRAINT UQ_DAILYHEALTHURGENTGROUP_ContainerRowId UNIQUE (Container, rowid);
ALTER TABLE nbri_ehr.dailyHealthUrgentGroupCode DROP CONSTRAINT FK_DAILYHEALTHURGENTGROUPCODE_Group;
ALTER TABLE nbri_ehr.dailyHealthUrgentGroupCode ADD CONSTRAINT FK_DAILYHEALTHURGENTGROUPCODE_Group FOREIGN KEY (Container, groupId) REFERENCES nbri_ehr.dailyHealthUrgentGroup (Container, rowid);

-- One row per log entry and urgent group that has alerted; its insert is what claims the alert.
CREATE TABLE nbri_ehr.dailyHealthUrgentNotification
(
    rowid               SERIAL NOT NULL,
    logEntryId          VARCHAR(200) NOT NULL,
    groupId             INTEGER NOT NULL,
    Container           entityId NOT NULL,
    Created             TIMESTAMP,
    CreatedBy           USERID,
    CONSTRAINT PK_DAILYHEALTHURGENTNOTIFICATION PRIMARY KEY (rowid),
    CONSTRAINT UQ_DAILYHEALTHURGENTNOTIFICATION_EntryGroup UNIQUE (Container, logEntryId, groupId),
    CONSTRAINT FK_DAILYHEALTHURGENTNOTIFICATION_Group FOREIGN KEY (Container, groupId) REFERENCES nbri_ehr.dailyHealthUrgentGroup (Container, rowid) ON DELETE CASCADE,
    CONSTRAINT FK_DAILYHEALTHURGENTNOTIFICATION_Container FOREIGN KEY (Container) REFERENCES core.Containers (EntityId)
);
CREATE INDEX IX_Nbri_Ehr_DailyHealthUrgentNotification_Group ON nbri_ehr.dailyHealthUrgentNotification (Container, groupId);

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.notification;

import org.labkey.api.data.Container;
import org.labkey.api.ehr.notification.AbstractEHRNotification;
import org.labkey.api.module.Module;
import org.labkey.api.security.User;

public class NBRINecropsyPreliminaryNotification extends AbstractEHRNotification
{
    public NBRINecropsyPreliminaryNotification(Module owner)
    {
        super(owner);
    }

    public NBRINecropsyPreliminaryNotification()
    {
        super();
    }

    @Override
    public String getName()
    {
        return "NBRI Necropsy Preliminary Report Notification";
    }

    @Override
    public String getEmailSubject(Container c)
    {
        return "Necropsy Preliminary Report";
    }

    @Override
    public String getScheduleDescription()
    {
        return "Sent immediately when preliminary necropsy results are submitted";
    }

    @Override
    public String getDescription()
    {
        return "The report sends the preliminary necropsy report when it is submitted.";
    }

    // the email is built and sent by NBRI_EHRTriggerHelper when the stage is submitted
    @Override
    public String getMessageBodyHTML(Container c, User u)
    {
        return null;
    }
}

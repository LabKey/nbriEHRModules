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

public class NBRINecropsyRequestNotification extends AbstractEHRNotification
{
    public NBRINecropsyRequestNotification(Module owner)
    {
        super(owner);
    }

    public NBRINecropsyRequestNotification()
    {
        super();
    }

    @Override
    public String getName()
    {
        return "NBRI Necropsy Request Notification";
    }

    @Override
    public String getEmailSubject(Container c)
    {
        return "Necropsy Request";
    }

    @Override
    public String getScheduleDescription()
    {
        return "Sent immediately when a necropsy request is submitted";
    }

    @Override
    public String getDescription()
    {
        return "The report sends an alert when a necropsy is requested so that pathology can schedule it.";
    }

    // the email is built and sent by NBRI_EHRTriggerHelper when the stage is submitted
    @Override
    public String getMessageBodyHTML(Container c, User u)
    {
        return null;
    }
}

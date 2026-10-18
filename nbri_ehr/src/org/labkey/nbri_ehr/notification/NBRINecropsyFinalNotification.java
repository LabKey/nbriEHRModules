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

public class NBRINecropsyFinalNotification extends AbstractEHRNotification
{
    public NBRINecropsyFinalNotification(Module owner)
    {
        super(owner);
    }

    public NBRINecropsyFinalNotification()
    {
        super();
    }

    @Override
    public String getName()
    {
        return "NBRI Necropsy Final Report Notification";
    }

    @Override
    public String getEmailSubject(Container c)
    {
        return "Necropsy Final Report";
    }

    @Override
    public String getScheduleDescription()
    {
        return "Sent immediately when final necropsy results are submitted";
    }

    @Override
    public String getDescription()
    {
        return "The report sends the final necropsy report when it is submitted.";
    }

    // the email is built and sent by NBRI_EHRTriggerHelper when the stage is submitted
    @Override
    public String getMessageBodyHTML(Container c, User u)
    {
        return null;
    }
}

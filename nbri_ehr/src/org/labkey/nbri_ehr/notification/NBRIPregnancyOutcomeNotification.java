/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.nbri_ehr.notification;

import org.labkey.api.data.Container;
import org.labkey.api.ehr.notification.AbstractEHRNotification;
import org.labkey.api.module.Module;
import org.labkey.api.security.User;

import java.util.Date;

public class NBRIPregnancyOutcomeNotification extends AbstractEHRNotification
{
    public NBRIPregnancyOutcomeNotification(Module owner)
    {
        super(owner);
    }

    public NBRIPregnancyOutcomeNotification()
    {
        super();
    }

    @Override
    public String getName()
    {
        return "Pregnancy Outcome Notification";
    }

    @Override
    public String getEmailSubject(Container c)
    {
        return "Pregnancy Outcome Alert " + getDateTimeFormat(c).format(new Date());
    }

    @Override
    public String getScheduleDescription()
    {
        return "Sent immediately upon Pregnancy Outcome form submission";
    }

    @Override
    public String getDescription()
    {
        return "The report sends an alert whenever a Pregnancy Outcome is reported.";
    }

    @Override
    public String getMessageBodyHTML(Container c, User u)
    {
        return null;
    }
}

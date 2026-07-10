/*
 * Copyright (c) 2024-2026 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.labkey.api.data.Container;
import org.labkey.api.ldk.notification.NotificationService;
import org.labkey.api.security.User;
import org.labkey.api.security.UserPrincipal;
import org.labkey.api.util.MailHelper;

import jakarta.mail.Address;
import jakarta.mail.Message;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

//a class to send notification message based on an animal event (Ex. death, birth, etc.) from a trigger script helper, essentially from a trigger script.
public class TriggerScriptNotification
{
    private static final Logger _log = LogManager.getLogger(TriggerScriptNotification.class);

    public static void sendMessage(String subject, String bodyHtml, Collection<UserPrincipal> recipients, Container container, User user)
    {
        try
        {
            Address from = NotificationService.get().getReturnEmail(container);
            if (from == null)
            {
                _log.warn("No return email set for EHR NotificationService.");
                return;
            }

            MailHelper.MultipartMessage msg = MailHelper.createMultipartMessage();
            msg.setFrom(from);
            msg.setSubject(subject);

            List<String> emails = new ArrayList<>();
            for (UserPrincipal u : recipients)
            {
                List<Address> addresses = NotificationService.get().getEmailsForPrincipal(u);
                if (addresses != null)
                {
                    for (Address a : addresses)
                    {
                        if (a.toString() != null)
                            emails.add(a.toString());
                    }
                }
            }

            if (emails.isEmpty())
            {
                _log.warn("No emails, unable to send EHR trigger script email");
                return;
            }

            msg.setRecipients(Message.RecipientType.TO, StringUtils.join(emails, ","));
            msg.setEncodedHtmlContent(bodyHtml);

            MailHelper.send(msg, user, container);
        }
        catch (Exception e)
        {
            _log.error("Unable to send email from EHR trigger script", e);
        }
    }
}

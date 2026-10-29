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

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.Sort;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.ehr.notification.AbstractEHRNotification;
import org.labkey.api.module.Module;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.settings.AppProps;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class NBRIProtocolExpirationNotification extends AbstractEHRNotification
{
    public static final int DAYS_AHEAD = 30;

    public NBRIProtocolExpirationNotification(Module owner)
    {
        super(owner);
    }

    @Override
    public String getName()
    {
        return "Protocol Expiration Notification";
    }

    @Override
    public String getScheduleDescription()
    {
        return "Daily at 7:00 AM";
    }

    @Override
    public String getCronString()
    {
        return "0 0 7 * * ?";
    }

    @Override
    public @Nullable String getMessageBodyHTML(Container c, User u)
    {
        List<ExpiringProtocol> expiringProtocols = getExpiringProtocols(c, u);
        StringBuilder html = new StringBuilder();
        html.append("<html>");

        if (!expiringProtocols.isEmpty())
        {
            html.append("<h4>Protocols expiring in the next ").append(DAYS_AHEAD).append(" days</h4>");
            appendTableHtml(c, html, expiringProtocols);
            html.append("<br/>");

            String reportLink = AppProps.getInstance().getBaseServerUrl() + AppProps.getInstance().getContextPath() + c.getPath() + "/query-executeQuery.view?schemaName=ehr&query.queryName=protocolExpirationNotification";
            html.append("<a href='").append(reportLink).append("'>");
            html.append("Click here to view these protocols in LabKey.</a>");
        }
        else
        {
            html.append("<h4>No protocols expiring in the next ").append(DAYS_AHEAD).append(" days</h4>");
        }

        html.append("</html>");
        return html.toString();
    }

    private void appendTableHtml(Container c, StringBuilder html, List<ExpiringProtocol> expiringProtocols)
    {
        html.append("<table style=\"border-collapse: collapse;\">");
        html.append("<tr>");
        html.append(NotificationHelper.getNotificationGridCellHeader(c, "Protocol"));
        html.append(NotificationHelper.getNotificationGridCellHeader(c, "Title"));
        html.append(NotificationHelper.getNotificationGridCellHeader(c, "PI"));
        html.append(NotificationHelper.getNotificationGridCellHeader(c, "Expiration Date"));
        html.append(NotificationHelper.getNotificationGridCellHeader(c, "Days Until Expiration"));
        html.append(NotificationHelper.getNotificationGridCellHeader(c, "Active Animals"));
        html.append("</tr>");

        for (int i = 0; i < expiringProtocols.size(); i++)
        {
            ExpiringProtocol protocol = expiringProtocols.get(i);
            String bgColor = i % 2 == 0 ? "#f2f2f2" : "#fff";
            html.append("<tr style=\"background-color:").append(bgColor).append("\">");

            html.append(NotificationHelper.getNotificationGridCell(c, protocol.getDisplayName()));
            html.append(NotificationHelper.getNotificationGridCell(c, protocol.getTitle()));
            html.append(NotificationHelper.getNotificationGridCell(c, protocol.getPi()));
            html.append(NotificationHelper.getNotificationGridCell(c, NotificationHelper.getFormattedDate(c, protocol.getExpirationDate())));
            html.append(NotificationHelper.getNotificationGridCell(c, String.valueOf(protocol.getDaysUntilExpiration())));
            html.append(NotificationHelper.getNotificationGridCell(c, String.valueOf(protocol.getActiveAnimals())));
            html.append("</tr>");
        }

        html.append("</table>");
    }

    private List<ExpiringProtocol> getExpiringProtocols(Container c, User u) throws IllegalStateException
    {
        UserSchema userSchema = QueryService.get().getUserSchema(u, c, "ehr");
        TableInfo tableInfo = userSchema.getTable("protocolExpirationNotification", null);

        if (null == tableInfo)
        {
            throw new IllegalStateException("Expected 'protocolExpirationNotification' query for the 'Protocol Expiration' notification");
        }

        TableSelector tableSelector = new TableSelector(tableInfo, Set.of("protocol", "displayName", "title", "pi", "expirationDate", "daysUntilExpiration", "activeAnimals"),
                null, new Sort("expirationDate,protocol"));
        return tableSelector.getArrayList(ExpiringProtocol.class);
    }

    @Override
    public String getDescription()
    {
        return "Daily notification listing protocols that expire within the next " + DAYS_AHEAD + " days";
    }

    @Override
    public String getEmailSubject(Container c)
    {
        return "Protocols expiring in the next " + DAYS_AHEAD + " days";
    }

    public static class ExpiringProtocol
    {
        private String _protocol;
        private String _displayName;
        private String _title;
        private String _pi;
        private Date _expirationDate;
        private int _daysUntilExpiration;
        private int _activeAnimals;

        public String getProtocol()
        {
            return _protocol;
        }

        public void setProtocol(String protocol)
        {
            _protocol = protocol;
        }

        public String getDisplayName()
        {
            return _displayName;
        }

        public void setDisplayName(String displayName)
        {
            _displayName = displayName;
        }

        public String getTitle()
        {
            return _title;
        }

        public void setTitle(String title)
        {
            _title = title;
        }

        public String getPi()
        {
            return _pi;
        }

        public void setPi(String pi)
        {
            _pi = pi;
        }

        public Date getExpirationDate()
        {
            return _expirationDate;
        }

        public void setExpirationDate(Date expirationDate)
        {
            _expirationDate = expirationDate;
        }

        public int getDaysUntilExpiration()
        {
            return _daysUntilExpiration;
        }

        public void setDaysUntilExpiration(int daysUntilExpiration)
        {
            _daysUntilExpiration = daysUntilExpiration;
        }

        public int getActiveAnimals()
        {
            return _activeAnimals;
        }

        public void setActiveAnimals(int activeAnimals)
        {
            _activeAnimals = activeAnimals;
        }
    }
}

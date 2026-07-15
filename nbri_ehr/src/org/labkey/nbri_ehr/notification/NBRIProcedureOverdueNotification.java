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


public class NBRIProcedureOverdueNotification extends AbstractEHRNotification
{
    public NBRIProcedureOverdueNotification(Module owner)
    {
        super(owner);
    }

    @Override
    public String getName()
    {
        return "Procedure Overdue Notification";
    }

    @Override
    public String getScheduleDescription()
    {
        return "Weekly on Friday at 8:00 AM";
    }

    @Override
    public String getCronString()
    {
        return "0 0 8 ? * 6";
    }

    @Override
    public @Nullable String getMessageBodyHTML(Container c, User u)
    {
        List<ProcedureOverdue> procsOverdueList = getOverDueProcedures(c, u);
        StringBuilder html = new StringBuilder();
        html.append("<html>");

        if (!procsOverdueList.isEmpty())
        {
            html.append("<h4>Animals with overdue procedures</h4>");
            appendTableHtml(c, html, procsOverdueList);
        }
        else
        {
            html.append("<h4>No overdue procedures</h4>");
        }

        html.append("<br/>");

        String procOverdueReportLink = AppProps.getInstance().getBaseServerUrl() + AppProps.getInstance().getContextPath() + c.getPath() + "/query-executeQuery.view?schemaName=study&query.queryName=prcOverdue";
        html.append("<a href='").append(procOverdueReportLink).append("'>");
        html.append("Click here to view Overdue Procedures report with additional actions.</a>");

        html.append("</html>");
        return html.toString();
    }

    private void appendTableHtml(Container c, StringBuilder html, List<ProcedureOverdue> procedureOverdueList)
    {
        html.append("<table style=\"border-collapse: collapse;\">");
        html.append("<tr>");
        html.append(NotificationHelper.getNotificationGridCellHeader(c, ("Id")));
        html.append(NotificationHelper.getNotificationGridCellHeader(c, ("Species")));
        html.append(NotificationHelper.getNotificationGridCellHeader(c,"Room"));
        html.append(NotificationHelper.getNotificationGridCellHeader(c,"Cage"));
        html.append(NotificationHelper.getNotificationGridCellHeader(c,"Procedure"));
        html.append(NotificationHelper.getNotificationGridCellHeader(c,"Ordered By"));
        html.append(NotificationHelper.getNotificationGridCellHeader(c,"Window Start"));
        html.append(NotificationHelper.getNotificationGridCellHeader(c,"Window End"));
        html.append(NotificationHelper.getNotificationGridCellHeader(c,"Days Overdue"));
        html.append(NotificationHelper.getNotificationGridCellHeader(c,"Remark"));

        html.append("</tr>");

        for (int i = 0; i < procedureOverdueList.size(); i++)
        {
            ProcedureOverdue pod = procedureOverdueList.get(i);
            String bgColor = i % 2 == 0 ? "#f2f2f2" : "#fff";
            html.append("<tr style=\"background-color:").append(bgColor).append("\">");

            html.append(NotificationHelper.getNotificationGridCell(c, pod.getId(), null, false, true, false));
            html.append(NotificationHelper.getNotificationGridCell(c, pod.getSpecies()));
            html.append(NotificationHelper.getNotificationGridCell(c, pod.getRoom()));
            html.append(NotificationHelper.getNotificationGridCell(c, pod.getCage()));
            html.append(NotificationHelper.getNotificationGridCell(c, pod.getProcedure()));
            html.append(NotificationHelper.getNotificationGridCell(c, pod.getOrderedBy()));
            html.append(NotificationHelper.getNotificationGridCell(c, NotificationHelper.getFormattedDate(c, pod.getWindowStart())));
            html.append(NotificationHelper.getNotificationGridCell(c, NotificationHelper.getFormattedDate(c, pod.getWindowEnd())));
            html.append(NotificationHelper.getNotificationGridCell(c, String.valueOf(pod.getDaysOverdue())));
            html.append(NotificationHelper.getNotificationGridCell(c, pod.getRemark()));
            html.append("</tr>");
        }

        html.append("</table>");
    }

    private List<ProcedureOverdue> getOverDueProcedures(Container c, User u) throws IllegalStateException
    {
        UserSchema userSchema = QueryService.get().getUserSchema(u, c, "study");
        TableInfo tableInfo = userSchema.getTable("prcOverdueNotification", null);

        if (null == tableInfo)
        {
            throw new IllegalStateException("Expected 'prcOverdueNotification' query for the 'NBRI Procedure Overdue' notification");
        }

        TableSelector tableSelector = new TableSelector(tableInfo, Set.of("Id", "species", "room", "cage", "procedure", "orderedBy", "windowStart", "windowEnd", "daysOverdue", "remark"));
        return tableSelector.getArrayList(ProcedureOverdue.class);
    }

    @Override
    public String getDescription()
    {
        return "Weekly notification sent Friday morning for animals with procedures past the scheduled window";
    }

    @Override
    public String getEmailSubject(Container c)
    {
        return "Procedure overdue notification";
    }

    public static class ProcedureOverdue
    {
        public String Id;
        public String species;
        public String room;
        public String cage;
        public String cageObjectId;
        public String procedure;
        public String orderedBy;
        public Date windowStart;
        public Date windowEnd;
        public int daysOverdue;
        public String remark;

        public String getId()
        {
            return Id;
        }

        public void setId(String id)
        {
            Id = id;
        }

        public String getSpecies()
        {
            return species;
        }

        public void setSpecies(String species)
        {
            this.species = species;
        }

        public String getRoom()
        {
            return room;
        }

        public void setRoom(String room)
        {
            this.room = room;
        }

        public String getCage()
        {
            return cage;
        }

        public void setCage(String cage)
        {
            this.cage = cage;
        }

        public String getCageObjectId()
        {
            return cageObjectId;
        }

        public void setCageObjectId(String cageObjectId)
        {
            this.cageObjectId = cageObjectId;
        }

        public String getProcedure()
        {
            return procedure;
        }

        public void setProcedure(String procedure)
        {
            this.procedure = procedure;
        }

        public String getOrderedBy()
        {
            return orderedBy;
        }

        public void setOrderedBy(String orderedBy)
        {
            this.orderedBy = orderedBy;
        }

        public Date getWindowStart()
        {
            return windowStart;
        }

        public void setWindowStart(Date windowStart)
        {
            this.windowStart = windowStart;
        }

        public Date getWindowEnd()
        {
            return windowEnd;
        }

        public void setWindowEnd(Date windowEnd)
        {
            this.windowEnd = windowEnd;
        }

        public int getDaysOverdue()
        {
            return daysOverdue;
        }

        public void setDaysOverdue(int daysOverdue)
        {
            this.daysOverdue = daysOverdue;
        }

        public String getRemark()
        {
            return remark;
        }

        public void setRemark(String remark)
        {
            this.remark = remark;
        }
    }
}

/*
 * Copyright (c) 2025-2026 LabKey Corporation
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

import jakarta.annotation.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.settings.AppProps;
import org.labkey.api.settings.LookAndFeelProperties;
import org.labkey.api.util.PageFlowUtil;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NotificationHelper
{
    private static final String NOTIFICATION_GRID_TD_STYLE = "padding: 5px; vertical-align: top; border: 1px solid #000000;";

    public static String getNotificationGridCell(Container c, String value, @Nullable String additionalStyle, boolean isHeader, boolean isId, boolean unsafePassThru)
    {
        String formattedValue = PageFlowUtil.filter(value);
        if (unsafePassThru)
        {
            formattedValue = value;
        }

        if (isId)
        {
            String idLink = AppProps.getInstance().getBaseServerUrl() +
                    AppProps.getInstance().getContextPath() +
                    c.getPath() +
                    "/ehr-participantView.view?participantId=" + formattedValue +
                    "&inputType:singleSubject&showReport:0&activeReport:snapshot";

            formattedValue = "<a href=\"" + idLink + "\">" + formattedValue + "</a>";
        }

        if (isHeader)
        {
            return "<th style=\"" + NOTIFICATION_GRID_TD_STYLE + (additionalStyle == null ? "" : additionalStyle) + "\"><b>" + formattedValue + "</b></th>";
        }
        else
        {
            return "<td style=\"" + NOTIFICATION_GRID_TD_STYLE + (additionalStyle == null ? "" : additionalStyle) + "\">" + formattedValue + "</td>";
        }
    }

    public static String getNotificationGridCell(Container c, String value)
    {
        return getNotificationGridCell(c, value, null, false, false, false);
    }

    public static String getNotificationGridCellHeader(Container c, String value)
    {
        return getNotificationGridCell(c, value, null, true, false, false);
    }

    public static String getFormattedDate(Container c, Date date)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat(LookAndFeelProperties.getInstance(c).getDefaultDateFormat());
        return dateFormat.format(date);
    }
}
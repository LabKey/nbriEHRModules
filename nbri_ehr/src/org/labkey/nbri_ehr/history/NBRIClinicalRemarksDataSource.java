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
package org.labkey.nbri_ehr.history;

import org.labkey.api.data.Container;
import org.labkey.api.data.Results;
import org.labkey.api.ehr.history.AbstractDataSource;
import org.labkey.api.module.Module;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;

import java.sql.SQLException;
import java.util.Set;

public class NBRIClinicalRemarksDataSource extends AbstractDataSource
{
    public NBRIClinicalRemarksDataSource(Module module)
    {
        super("study", "Clinical Remarks", "Clinical Remark", "Clinical", module);
        setShowTime(true);
    }

    @Override
    protected String getCategoryText(Results rs) throws SQLException
    {
        String category = rs.getString("category");

        FieldKey titleFk = FieldKey.fromString("category/title");
        if (rs.hasColumn(titleFk))
            category = rs.getString(titleFk);

        return (category == null ?  "Clinical" : category) + " - Remark";
    }

    @Override
    protected String getPrimaryGroup(Results rs) throws SQLException
    {
        String category = rs.getString("category");
        return (category == null ?  "Clinical" : category);
    }

    @Override
    protected String getHtml(Container c, Results rs, boolean redacted) throws SQLException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<table>");

        /*if (!redacted && rs.getObject(FieldKey.fromString("performedby/displayName")) != null)
        {
            String label = "Entered By";
            appendNote(rs, "performedby/displayName", "<span style='white-space:nowrap'>" + label + "</span>", sb);
        }*/

        appendNote(rs, "hx", "Hx", sb);
        appendNote(rs, "so", "S/O", sb);
        appendNote(rs, "s", "S", sb);
        appendNote(rs, "o", "O", sb);
        appendNote(rs, "a", "A", sb);
        appendNote(rs, "p", "P", sb);
        appendNote(rs, "p2", "P2", sb);
        appendNote(rs, "remark", "Other Remark", sb);

        sb.append("</table>");

        return sb.toString();
    }

    @Override
    protected Set<String> getColumnNames()
    {
        return PageFlowUtil.set("Id", "date", "enddate", "category", "hx", "so", "s", "o", "a", "p", "p2", "remark", "performedby/displayName");
    }

    private void appendNote(Results rs, String field, String label, StringBuilder sb) throws SQLException
    {
        if (rs.hasColumn(FieldKey.fromString(field)) && rs.getObject(FieldKey.fromString(field)) != null)
        {
            sb.append("<tr style='vertical-align:top;margin-bottom: 5px;'><td style='padding-right: 5px;'>" + label + ":</td><td>");
            sb.append(PageFlowUtil.filter(rs.getString(FieldKey.fromString(field))));
            sb.append("</td></tr>");
        }
    }
}

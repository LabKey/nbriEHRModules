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
package org.labkey.nbri_ehr.history;

import org.labkey.api.data.Container;
import org.labkey.api.data.Results;
import org.labkey.api.ehr.history.AbstractDataSource;
import org.labkey.api.module.Module;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.PageFlowUtil;

import java.sql.SQLException;
import java.util.Set;

public class NBRIHousingDataSource extends AbstractDataSource
{
    public NBRIHousingDataSource(Module module)
    {
        super("study", "Housing", "Housing Transfer", "Housing Transfers", module);
        setShowTime(true);
    }

    @Override
    protected Set<String> getColumnNames()
    {
        return PageFlowUtil.set("Id", "date", "cage/cage", "room/fullRoom", "reason", "remark");
    }

    @Override
    protected String getHtml(Container c, Results rs, boolean redacted) throws SQLException
    {
        StringBuilder sb = new StringBuilder();

        FieldKey room = FieldKey.fromString("room/fullRoom");
        FieldKey cage = FieldKey.fromString("cage/cage");
        String value = "Unknown";
        if (rs.hasColumn(cage) && rs.getObject(cage) != null)
        {
            value = rs.getString(cage);
        }
        else if(rs.hasColumn(room) && rs.getObject(room) != null)
        {
            value = rs.getString(room);
        }

        sb.append(PageFlowUtil.filter("Moved to : " + value));
        sb.append("\n");
        sb.append(safeAppend(rs, "Reason", "reason"));
        sb.append(safeAppend(rs, "Remark", "remark"));

        return sb.toString();
    }
}

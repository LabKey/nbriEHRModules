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

public class SerologyDataSource extends AbstractDataSource
{

    public SerologyDataSource(Module module)
    {
        super("study", "Serology", "Serology", "Serology", module);
    }

    @Override
    protected Set<String> getColumnNames()
    {
        return PageFlowUtil.set("Id", "date", "type", "lab");
    }

    @Override
    protected String getHtml(Container c, Results rs, boolean redacted) throws SQLException
    {
        StringBuilder sb = new StringBuilder();

        if (rs.hasColumn(FieldKey.fromString("type")) && rs.getObject("type") != null)
        {
            addRow(sb, "Type", rs.getString("type"));
        }

        if (rs.hasColumn(FieldKey.fromString("lab")) && rs.getObject("lab") != null)
        {
            addRow(sb, "Lab", rs.getString("lab"));
        }

        return sb.toString();
    }

    private void addRow(StringBuilder sb, String displayLabel, String value)
    {
        sb.append(displayLabel);
        sb.append(": ");
        sb.append(PageFlowUtil.filter(value));
        sb.append("\n");
    }
}

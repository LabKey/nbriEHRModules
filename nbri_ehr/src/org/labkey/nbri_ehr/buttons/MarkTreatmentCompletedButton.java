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
package org.labkey.nbri_ehr.buttons;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.TableInfo;
import org.labkey.api.ehr.buttons.MarkCompletedButton;
import org.labkey.api.ehr.security.EHRCompletedUpdatePermission;
import org.labkey.api.module.Module;
import org.labkey.api.security.permissions.Permission;
import org.labkey.api.util.PageFlowUtil;

public class MarkTreatmentCompletedButton extends MarkCompletedButton
{
    public MarkTreatmentCompletedButton(Module owner, String schemaName, String queryName, String label)
    {
        this(owner, schemaName, queryName, label, EHRCompletedUpdatePermission.class, false);
    }

    public MarkTreatmentCompletedButton(Module owner, String schemaName, String queryName, String label, Class<? extends Permission> perm, boolean forceDateOnlyField)
    {
        super(owner, schemaName, queryName, label, perm, forceDateOnlyField);
    }

    @Override
    protected String getJsHandler(TableInfo ti)
    {
        // NOTE: we have a problem if this is called before all XML metadata is applied.  for example, a dataset could call the tablecustomizer from studyData.query.xml
        // but the dataset-specific query.xml file will apply a different format to the enddate column.  as a result, defer creating the JS handler as long as we can.
        ColumnInfo col = ti.getColumn("enddate");
        String xtype = "xdatetime";
        if (!_forceDateOnlyField && col != null && col.getFormat() != null && col.getFormat().contains("HH"))
            xtype = "xdatetime";

        String pkColName = null;
        if (ti.getPkColumnNames() != null && ti.getPkColumnNames().size() == 1)
        {
            pkColName = ti.getPkColumnNames().getFirst();
        }

        return "NBRI_EHR.window.MarkTreatmentCompletedWindow.buttonHandler(dataRegionName, " + PageFlowUtil.jsString(_schemaName) + ", " + PageFlowUtil.jsString(_queryName) + ", " + PageFlowUtil.jsString(xtype) + ", " + PageFlowUtil.jsString(pkColName) + ");";
    }
}

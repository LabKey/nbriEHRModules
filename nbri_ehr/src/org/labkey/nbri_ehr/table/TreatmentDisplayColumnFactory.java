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
package org.labkey.nbri_ehr.table;

import org.labkey.api.data.ColumnInfo;
import org.labkey.api.data.DataColumn;
import org.labkey.api.data.DisplayColumn;
import org.labkey.api.data.DisplayColumnFactory;
import org.labkey.api.data.RenderContext;
import org.labkey.api.ehr.security.EHRClinicalEntryPermission;
import org.labkey.api.query.FieldKey;
import org.labkey.api.util.DateUtil;
import org.labkey.api.util.LinkBuilder;
import org.labkey.api.view.ActionURL;
import org.labkey.api.writer.HtmlWriter;

import java.util.Date;
import java.util.Set;

/**
 * Display column factory for creating Record Treatment links. When includeScheduledDate is set, the row's date is
 * passed as the scheduledDate URL parameter, so it should only be set on tables whose date column is the scheduled
 * slot being recorded (e.g. treatmentSchedule), not the treatment order's start date.
 */
public class TreatmentDisplayColumnFactory implements DisplayColumnFactory
{
    private final boolean _includeScheduledDate;

    public TreatmentDisplayColumnFactory(boolean includeScheduledDate)
    {
        _includeScheduledDate = includeScheduledDate;
    }

    @Override
    public DisplayColumn createRenderer(final ColumnInfo colInfo)
    {
        return new DataColumn(colInfo){

            @Override
            public void renderGridCellContents(RenderContext ctx, HtmlWriter out)
            {
                String objectid = (String)getBoundColumn().getValue(ctx);
                Date date = (Date)ctx.get("date");
                String caseid = (String)ctx.get("caseid");
                String category = (String)ctx.get("category");
                ActionURL url = new ActionURL("ehr", "dataEntryForm", colInfo.getParentTable().getUserSchema().getContainer());
                if (!colInfo.getParentTable().getUserSchema().getContainer().hasPermission(colInfo.getParentTable().getUserSchema().getUser(), EHRClinicalEntryPermission.class))
                    return;

                if (category == null)
                    return;

                if (category.equals("Behavior"))
                {
                    if (caseid != null)
                    {
                        url.addParameter("formType", "Behavioral Rounds");
                        url.addParameter("caseid", caseid);
                    }
                    else
                    {
                        url.addParameter("formType", "Bulk Behavior Entry");
                    }
                }
                else
                {
                    if (caseid != null)
                    {
                        url.addParameter("formType", "Clinical Rounds");
                        url.addParameter("caseid", caseid);
                    }
                    else
                    {
                        url.addParameter("formType", "medicationTreatment");
                    }
                }

                url.addParameter("treatmentid", objectid);
                if (_includeScheduledDate && date != null)
                    url.addParameter("scheduledDate", DateUtil.formatIsoDateShortTime(date));

                String returnUrl = new ActionURL("ehr", "animalHistory", colInfo.getParentTable().getUserSchema().getContainer()) + "#inputType:none&showReport:0&activeReport:clinMedicationSchedule";
                url.addParameter("returnUrl", returnUrl);

                out.write(LinkBuilder.labkeyLink("Record Treatment", url).target("_blank"));
            }

            @Override
            public void addQueryFieldKeys(Set<FieldKey> keys)
            {
                super.addQueryFieldKeys(keys);
                keys.add(getBoundColumn().getFieldKey());
                keys.add(FieldKey.fromString("date"));
                keys.add(FieldKey.fromString("caseid"));
                keys.add(FieldKey.fromString("category"));
            }

            @Override
            public boolean isSortable()
            {
                return false;
            }

            @Override
            public boolean isFilterable()
            {
                return false;
            }

            @Override
            public boolean isEditable()
            {
                return false;
            }
        };
    }
}

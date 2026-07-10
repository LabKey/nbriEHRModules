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
package org.labkey.nbri_ehr.dataentry.form;

import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.forms.AdminLinksFormType;
import org.labkey.api.ehr.security.EHRProtocolEditPermission;
import org.labkey.api.module.Module;
import org.labkey.api.view.ActionURL;

import java.util.ArrayList;

public class NBRIProjectFormType extends AdminLinksFormType
{
    public NBRIProjectFormType(DataEntryFormContext ctx, Module owner)
    {
        super(ctx, owner, "Projects", "Projects", "Admin", new ArrayList<>());
    }

    @Override
    protected ActionURL dataEntryLink()
    {
        ActionURL url = new ActionURL("ldk", "updateQuery", getCtx().getContainer());
        url.addParameter("schemaName", "ehr");
        url.addParameter("query.queryName", "project");
        url.addParameter("showImport", "true");
        return url;
    }

    @Override
    public boolean isAvailable()
    {
        return (super.isAvailable() || getCtx().getContainer().hasPermission(getCtx().getUser(), EHRProtocolEditPermission.class));
    }
}

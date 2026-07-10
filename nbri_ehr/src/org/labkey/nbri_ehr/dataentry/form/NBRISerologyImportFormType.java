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
package org.labkey.nbri_ehr.dataentry.form;

import org.json.JSONObject;
import org.labkey.api.ehr.dataentry.AbstractDataEntryForm;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.module.Module;
import org.labkey.api.view.ActionURL;

import java.util.ArrayList;

public class NBRISerologyImportFormType extends AbstractDataEntryForm
{
    private final DataEntryFormContext _formContext;

    public NBRISerologyImportFormType(DataEntryFormContext ctx, Module owner)
    {
        super(ctx, owner, "SerologyImport", "Serology Import", "Lab Results", new ArrayList<>());
        _formContext = ctx;
    }

    @Override
    public JSONObject toJSON(boolean includeFormElements)
    {
        JSONObject json = super.toJSON(includeFormElements);

        ActionURL url = new ActionURL("study", "import", _formContext.getContainer());
        url.addParameter("datasetId", "1032");
        json.put("url", url);

        return json;
    }
}

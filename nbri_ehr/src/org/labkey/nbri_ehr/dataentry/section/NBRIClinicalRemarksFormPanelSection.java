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
package org.labkey.nbri_ehr.dataentry.section;

import org.json.JSONObject;
import org.labkey.api.data.TableInfo;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.ParentFormPanelSection;
import org.labkey.api.ehr.security.EHRVeterinarianPermission;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.permissions.AdminPermission;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.nbri_ehr.security.NBRIEHRVetTechPermission;

import java.util.List;

public class NBRIClinicalRemarksFormPanelSection extends ParentFormPanelSection
{
    private boolean isVetTech;
    private boolean isVet;
    private boolean isFolderAdmin;
    private boolean isBehavior;

    public NBRIClinicalRemarksFormPanelSection(String label)
    {
        super("study", "clinremarks", label);
        setSupportFormSort(false);
    }

    public NBRIClinicalRemarksFormPanelSection(boolean isChild, String parentQueryName, String label, DataEntryFormContext ctx, boolean isBehavior)
    {
        this(label);
        this.isVetTech = ctx.getContainer().hasPermission(ctx.getUser(), NBRIEHRVetTechPermission.class);
        this.isVet = ctx.getContainer().hasPermission(ctx.getUser(), EHRVeterinarianPermission.class);
        this.isFolderAdmin = ctx.getContainer().hasPermission(ctx.getUser(), AdminPermission.class);
        this.isBehavior = isBehavior;

        if (isChild)
        {
            addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/ParentChild.js"));
            addConfigSource("ParentChild");

            addClientDependency(ClientDependency.supplierFromPath("ehr/data/ChildClientStore.js"));
            setClientStoreClass("EHR.data.ChildClientStore");
            addExtraProperty("parentQueryName", parentQueryName);
        }

    }

    @Override
    public JSONObject toJSON(DataEntryFormContext ctx, boolean includeFormElements)
    {
        JSONObject json = super.toJSON(ctx, includeFormElements);
        json.put("collapsible", true);
        json.put("initCollapsed", false);
        json.put("dataDependentCollapseHeader", true);
        return json;
    }

    @Override
    protected List<FieldKey> getFieldKeys(TableInfo ti)
    {
        List<FieldKey> keys = super.getFieldKeys(ti);

        // only Vets and Folder Admins can enter S.O.A.P.
        if (isBehavior || (!isVet && (!isFolderAdmin || isVetTech)))
        {
            keys.remove(FieldKey.fromString("s"));
            keys.remove(FieldKey.fromString("o"));
            keys.remove(FieldKey.fromString("a"));
            keys.remove(FieldKey.fromString("p"));
        }

        return keys;
    }
}

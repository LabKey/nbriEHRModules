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

public class NBRICasesFormPanelSection extends ParentFormPanelSection
{
    private final boolean isVetTech;
    private final boolean isVet;
    private final boolean isFolderAdmin;
    private final boolean isBehavior;

    public NBRICasesFormPanelSection(String label, DataEntryFormContext ctx, boolean isBehavior)
    {
        super("study", "cases", label);
        this.isBehavior = isBehavior;
        this.isVetTech = ctx.getContainer().hasPermission(ctx.getUser(), NBRIEHRVetTechPermission.class);
        this.isVet = ctx.getContainer().hasPermission(ctx.getUser(), EHRVeterinarianPermission.class);
        this.isFolderAdmin = ctx.getContainer().hasPermission(ctx.getUser(), AdminPermission.class);
        setSupportFormSort(false);

        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/field/AnimalIdCases.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/field/EditCase.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/buttons/SelectCaseButton.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/panel/SelectCasePanel.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/window/SelectCaseWindow.js"));

        setClientStoreClass("NBRI_EHR.data.CaseClientStore");
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/data/CaseClientStore.js"));
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

        // only Vets and Folder Admins can see the enddate ('Close date') field to be able to close the case.
        // Does not apply to behavior cases.
        if (!this.isBehavior && !isVet && (!isFolderAdmin || isVetTech))
        {
            keys.remove(FieldKey.fromString("enddate"));
            keys.remove(FieldKey.fromString("closeRemark"));
        }

        keys.add(FieldKey.fromString("qcstate/label"));

        return keys;
    }
}

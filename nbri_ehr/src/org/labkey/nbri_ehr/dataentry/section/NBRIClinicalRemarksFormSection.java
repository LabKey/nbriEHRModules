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

import org.labkey.api.data.TableInfo;
import org.labkey.api.query.FieldKey;

import java.util.List;

public class NBRIClinicalRemarksFormSection extends BaseFormSection
{
    public static final String LABEL = "Clinical Remarks";
    private final boolean isVetTech;
    private final boolean isVet;
    private final boolean isFolderAdmin;

    public NBRIClinicalRemarksFormSection(boolean isVetTech, boolean isVet, boolean isFolderAdmin)
    {
        super("study", "clinremarks", LABEL, "ehr-gridpanel", true, true, true);
        this.isVetTech = isVetTech;
        this.isVet = isVet;
        this.isFolderAdmin = isFolderAdmin;
    }

    public NBRIClinicalRemarksFormSection(String label, boolean isVetTech, boolean isVet, boolean isFolderAdmin)
    {
        super("study", "clinremarks", label, "ehr-gridpanel", true, true, true);
        this.isVetTech = isVetTech;
        this.isVet = isVet;
        this.isFolderAdmin = isFolderAdmin;
    }

    @Override
    protected List<FieldKey> getFieldKeys(TableInfo ti)
    {
        List<FieldKey> keys = super.getFieldKeys(ti);

        // only Vets and Folder Admins can enter S.O.A.P.
        if (!isVet && (!isFolderAdmin || isVetTech))
        {
            keys.remove(FieldKey.fromString("s"));
            keys.remove(FieldKey.fromString("o"));
            keys.remove(FieldKey.fromString("a"));
            keys.remove(FieldKey.fromString("p"));
        }

        return keys;
    }
}

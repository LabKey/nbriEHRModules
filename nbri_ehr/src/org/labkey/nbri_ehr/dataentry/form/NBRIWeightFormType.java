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
package org.labkey.nbri_ehr.dataentry.form;

import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.module.Module;
import org.labkey.nbri_ehr.dataentry.section.NBRIAnimalDetailsFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRITaskFormSection;
import org.labkey.nbri_ehr.dataentry.section.NBRIWeightFormSection;

import java.util.List;

public class NBRIWeightFormType extends NBRIBaseTaskFormType
{
    public static final String NAME = "Weight";
    public static final String LABEL = "Weights";

    public NBRIWeightFormType(DataEntryFormContext ctx, Module owner)
    {
        super(ctx, owner, NAME, LABEL, "Clinical", List.of(
                new NBRITaskFormSection(),
                new NBRIAnimalDetailsFormSection(),
                new NBRIWeightFormSection(false, false)
        ));
    }
}

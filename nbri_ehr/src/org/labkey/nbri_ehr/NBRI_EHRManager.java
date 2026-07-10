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
package org.labkey.nbri_ehr;

import org.labkey.api.query.Queryable;

import java.util.List;

public class NBRI_EHRManager
{
    @Queryable
    public static final String DAILY_CLINICAL_OBS_TITLE = "Daily Clinical Observations";
    public static final List<String> DAILY_CLINICAL_OBS = List.of("Activity", "Appetite", "BCS", "Hydration", "Stool", "Verified Id?");
    public static final String SIB_OBS_TITLE = "SIB Observations";
    public static final List<String> SIB_OBS = List.of("Environmental Change", "Self Biting Observed", "Other Stereotopy", "New Injury Observed", "Special Enrichment", "Wound Status", "Wound Severity");

    private static final NBRIOrchardFileGenerator _orchardFileGenerator = new NBRIOrchardFileGenerator();
    public static NBRIOrchardFileGenerator getOrchardFileGenerator()
    {
        return _orchardFileGenerator;
    }
}

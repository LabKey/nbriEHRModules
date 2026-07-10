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
package org.labkey.nbri_ehr.security;

import org.labkey.api.ehr.security.AbstractEHRPermission;

public class NBRIEHRVetTechPermission extends AbstractEHRPermission
{
    public NBRIEHRVetTechPermission()
    {
        super("NBRIEHRVetTechPermission", "This is the base permission for vet techs. It grants the ability to submit and update records but not close, approve, or delete them.");
    }
}
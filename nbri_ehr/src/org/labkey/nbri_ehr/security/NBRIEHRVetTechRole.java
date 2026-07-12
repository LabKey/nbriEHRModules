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
package org.labkey.nbri_ehr.security;

import org.labkey.api.ehr.security.EHRDataEntryPermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;
import org.labkey.api.security.roles.AbstractRole;

public class NBRIEHRVetTechRole extends AbstractRole
{
    public NBRIEHRVetTechRole()
    {
        super("EHR Veterinarian Technician", "Users with this role are permitted to submit and update records but not close, approve, or delete them.",
                ReadPermission.class,
                InsertPermission.class,
                UpdatePermission.class,
                EHRDataEntryPermission.class,
                NBRIEHRVetTechPermission.class
        );
    }
}
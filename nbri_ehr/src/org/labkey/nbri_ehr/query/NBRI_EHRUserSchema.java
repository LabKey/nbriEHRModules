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
package org.labkey.nbri_ehr.query;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.Container;
import org.labkey.api.data.ContainerFilter;
import org.labkey.api.data.DbSchema;
import org.labkey.api.data.TableInfo;
import org.labkey.api.ehr.security.EHRProtocolEditPermission;
import org.labkey.api.ldk.table.CustomPermissionsTable;
import org.labkey.api.query.SimpleUserSchema;
import org.labkey.api.security.User;
import org.labkey.api.security.permissions.DeletePermission;
import org.labkey.api.security.permissions.InsertPermission;
import org.labkey.api.security.permissions.ReadPermission;
import org.labkey.api.security.permissions.UpdatePermission;

public class NBRI_EHRUserSchema extends SimpleUserSchema
{
    public static String SCHEMA_NAME = "nbri_ehr";

    public NBRI_EHRUserSchema(String name, @Nullable String description, User user, Container container, DbSchema dbschema)
    {
        super(name, description, user, container, dbschema);
    }

    @Override
    @Nullable
    protected TableInfo createWrappedTable(String name, @NotNull TableInfo schemaTable, ContainerFilter cf)
    {
        // Approving an amendment additionally requires NBRIProtocolAmendmentApprovePermission, but that is checked on
        // the status transition in ProtocolAmendment.js -- mapping it to UpdatePermission here would stop submitters
        // editing their own drafts.
        if ("protocolAmendment".equalsIgnoreCase(name))
        {
            CustomPermissionsTable<?> ti = new CustomPermissionsTable<>(this, schemaTable, cf).init();
            ti.addPermissionMapping(InsertPermission.class, EHRProtocolEditPermission.class);
            ti.addPermissionMapping(UpdatePermission.class, EHRProtocolEditPermission.class);
            ti.addPermissionMapping(DeletePermission.class, EHRProtocolEditPermission.class);
            return ti;
        }

        return super.createWrappedTable(name, schemaTable, cf);
    }

    @Override
    public boolean canReadSchema()
    {
        User user = getUser();
        if (user == null)
            return false;

        return getContainer().hasPermission(user, ReadPermission.class);
    }
}

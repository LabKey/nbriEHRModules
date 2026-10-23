/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.demographics;

import org.labkey.api.data.Container;
import org.labkey.api.module.Module;
import org.labkey.api.query.FieldKey;
import org.labkey.api.security.User;

import java.util.Set;

/**
 * Adds the group title, which NBRI displays, to the shared provider. Shares its name so it replaces that provider,
 * so it must stay limited to folders where this module is active.
 */
public class ActiveGroupsDemographicsProvider extends org.labkey.api.ehr.demographics.ActiveGroupsDemographicsProvider
{
    public ActiveGroupsDemographicsProvider(Module owner)
    {
        super(owner);
    }

    @Override
    protected Set<FieldKey> getFieldKeys()
    {
        Set<FieldKey> keys = super.getFieldKeys();
        keys.add(FieldKey.fromString("groupId/title"));
        return keys;
    }

    @Override
    public boolean isAvailable(Container c, User u)
    {
        return super.isAvailable(c, u) && c.getActiveModules().contains(_owner);
    }
}

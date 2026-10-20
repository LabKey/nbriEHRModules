/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.demographics;

import org.labkey.api.ehr.demographics.AbstractDemographicsProvider;
import org.labkey.api.module.Module;
import org.labkey.api.query.FieldKey;

import java.util.Collection;
import java.util.Set;

public class MostRecentBCSDemographicsProvider extends AbstractDemographicsProvider
{
    public MostRecentBCSDemographicsProvider(Module owner)
    {
        super(owner, "study", "demographicsMostRecentBCS");
        // The query applies the publicdata filter itself and exposes no QCState column
        _supportsQCState = false;
    }

    @Override
    public String getName()
    {
        return "Most Recent BCS";
    }

    @Override
    protected Collection<FieldKey> getFieldKeys()
    {
        return Set.of(FieldKey.fromString("Id"), FieldKey.fromString("mostRecentBCS"), FieldKey.fromString("mostRecentBCSDate"));
    }

    @Override
    public boolean requiresRecalc(String schema, String query)
    {
        return "study".equalsIgnoreCase(schema) && "clinical_observations".equalsIgnoreCase(query);
    }
}

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.section;

import org.json.JSONObject;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;

import java.util.List;

public class NBRITissueMeasurementsFormSection extends BaseFormSection
{
    private final boolean _initCollapsed;

    public NBRITissueMeasurementsFormSection(boolean initCollapsed)
    {
        super("study", "tissueMeasurements", "Tissue Measurements", "ehr-gridpanel", true, false, false);
        NecropsySectionUtils.configureAsChild(this);
        _initCollapsed = initCollapsed;
    }

    @Override
    public List<String> getTbarButtons()
    {
        // the animal comes from the necropsy request
        List<String> defaults = super.getTbarButtons();
        defaults.remove("NBRI_ADDANIMALS");
        return defaults;
    }

    @Override
    public JSONObject toJSON(DataEntryFormContext ctx, boolean includeFormElements)
    {
        JSONObject json = super.toJSON(ctx, includeFormElements);
        NecropsySectionUtils.putCollapseConfig(json, _initCollapsed);
        return json;
    }
}

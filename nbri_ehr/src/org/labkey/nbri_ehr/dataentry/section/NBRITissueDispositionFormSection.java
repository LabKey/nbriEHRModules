/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.section;

import org.json.JSONObject;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;

import java.util.List;

public class NBRITissueDispositionFormSection extends BaseFormSection
{
    private final boolean _initCollapsed;

    public NBRITissueDispositionFormSection(boolean initCollapsed)
    {
        super("study", "tissueDisposition", "Tissue Disposition", "ehr-gridpanel", true, false, true);
        NecropsySectionUtils.configureAsChild(this);
        _initCollapsed = initCollapsed;
    }

    @Override
    public List<String> getTbarButtons()
    {
        List<String> defaults = super.getTbarButtons();
        defaults.remove("NBRI_ADDANIMALS");
        defaults.remove("TEMPLATE");
        defaults.add("TEMPLATE_NO_ID_NO_DATE");
        return defaults;
    }

    @Override
    public List<String> getTbarMoreActionButtons()
    {
        List<String> defaultButtons = super.getTbarMoreActionButtons();
        defaultButtons.remove("COPY_IDS");
        return defaultButtons;
    }

    @Override
    public JSONObject toJSON(DataEntryFormContext ctx, boolean includeFormElements)
    {
        JSONObject json = super.toJSON(ctx, includeFormElements);
        NecropsySectionUtils.putCollapseConfig(json, _initCollapsed);
        return json;
    }
}

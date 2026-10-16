/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.section;

import org.json.JSONObject;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.SimpleFormPanelSection;

public class NBRINecropsyTaskFormSection extends SimpleFormPanelSection
{
    private final boolean _initCollapsed;

    public NBRINecropsyTaskFormSection(boolean initCollapsed)
    {
        super("nbri_ehr", "necropsyTask", "Request");
        setTemplateMode(TEMPLATE_MODE.NONE);
        setSupportFormSort(false);
        _initCollapsed = initCollapsed;
    }

    @Override
    public JSONObject toJSON(DataEntryFormContext ctx, boolean includeFormElements)
    {
        JSONObject json = super.toJSON(ctx, includeFormElements);
        NecropsySectionUtils.putCollapseConfig(json, _initCollapsed);
        return json;
    }
}

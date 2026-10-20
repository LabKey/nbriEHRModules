/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.section;

import org.json.JSONObject;
import org.labkey.api.data.TableInfo;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.SimpleFormPanelSection;
import org.labkey.api.query.FieldKey;

import java.util.List;

public class NBRINecropsyFinalFormSection extends SimpleFormPanelSection
{
    private final boolean _initCollapsed;

    public NBRINecropsyFinalFormSection(boolean initCollapsed)
    {
        super("study", "necropsyFinal", "Final Results");
        setTemplateMode(TEMPLATE_MODE.NONE);
        NecropsySectionUtils.configureAsChild(this);
        _initCollapsed = initCollapsed;
    }

    @Override
    protected List<FieldKey> getFieldKeys(TableInfo ti)
    {
        // a column added to an existing dataset lands last, so place the category explicitly
        FieldKey category = FieldKey.fromString("causeCategory");
        List<FieldKey> keys = super.getFieldKeys(ti);
        if (keys.remove(category))
            keys.addFirst(category);
        return keys;
    }

    @Override
    public JSONObject toJSON(DataEntryFormContext ctx, boolean includeFormElements)
    {
        JSONObject json = super.toJSON(ctx, includeFormElements);
        NecropsySectionUtils.putCollapseConfig(json, _initCollapsed);
        return json;
    }
}

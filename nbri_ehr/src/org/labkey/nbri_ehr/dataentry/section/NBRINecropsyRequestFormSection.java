/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.section;

import org.json.JSONObject;
import org.labkey.api.data.TableInfo;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.ParentFormPanelSection;
import org.labkey.api.query.FieldKey;

import java.util.List;

public class NBRINecropsyRequestFormSection extends ParentFormPanelSection
{
    private final boolean _initCollapsed;

    public NBRINecropsyRequestFormSection(boolean initCollapsed)
    {
        super("study", NecropsySectionUtils.PARENT_QUERY, "Necropsy Request");
        setSupportFormSort(false);
        _initCollapsed = initCollapsed;
    }

    @Override
    protected List<FieldKey> getFieldKeys(TableInfo ti)
    {
        List<FieldKey> keys = super.getFieldKeys(ti);
        keys.add(1, FieldKey.fromString("Id/Demographics/death"));
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

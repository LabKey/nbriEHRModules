/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.section;

import org.json.JSONObject;
import org.labkey.api.ehr.dataentry.AbstractFormSection;
import org.labkey.api.view.template.ClientDependency;

/**
 * Shared configuration for the sections of the necropsy forms, which all hang off the necropsy request.
 */
final class NecropsySectionUtils
{
    static final String PARENT_QUERY = "necropsyRequest";

    private NecropsySectionUtils()
    {
    }

    static void configureAsChild(AbstractFormSection section)
    {
        section.setSupportFormSort(false);
        section.addExtraProperty("parentQueryName", PARENT_QUERY);

        section.addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/ParentChild.js"));
        section.addConfigSource("ParentChild");

        section.addClientDependency(ClientDependency.supplierFromPath("ehr/data/ChildClientStore.js"));
        section.setClientStoreClass("EHR.data.ChildClientStore");
    }

    static void putCollapseConfig(JSONObject json, boolean initCollapsedWithData)
    {
        json.put("collapsible", true);
        json.put("initCollapsedWithData", initCollapsedWithData);
        json.put("dataDependentCollapseHeader", true);
    }
}

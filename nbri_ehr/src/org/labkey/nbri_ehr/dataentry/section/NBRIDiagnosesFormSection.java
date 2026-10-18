/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.section;

import org.json.JSONObject;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.view.template.ClientDependency;

import java.util.List;

/** The preliminary or final necropsy diagnoses grid, one row per lesion. */
public class NBRIDiagnosesFormSection extends BaseFormSection
{
    private final boolean _initCollapsed;

    private NBRIDiagnosesFormSection(String queryName, String label, String xtype, boolean initCollapsed)
    {
        super("study", queryName, label, xtype, true, false, false);
        NecropsySectionUtils.configureAsChild(this);
        _initCollapsed = initCollapsed;
    }

    public static NBRIDiagnosesFormSection forPreliminary(boolean initCollapsed)
    {
        return new NBRIDiagnosesFormSection("preliminaryDiagnoses", "Gross Diagnoses", "ehr-gridpanel", initCollapsed);
    }

    /** Starts from a copy of the preliminary diagnoses, so the form using it must also include {@link #forPreliminary}. */
    public static NBRIDiagnosesFormSection forFinal(boolean initCollapsed)
    {
        NBRIDiagnosesFormSection section = new NBRIDiagnosesFormSection("finalDiagnoses", "Final Diagnoses", "nbri_ehr-finaldiagnosesgridpanel", initCollapsed);
        section.addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/grid/FinalDiagnosesGridPanel.js"));
        return section;
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

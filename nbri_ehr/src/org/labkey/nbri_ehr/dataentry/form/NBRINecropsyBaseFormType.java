/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.form;

import org.json.JSONObject;
import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.FormSection;
import org.labkey.api.ehr.security.EHRPathologyEntryPermission;
import org.labkey.api.module.Module;
import org.labkey.api.view.template.ClientDependency;

import java.util.ArrayList;
import java.util.List;

/**
 * The three necropsy stages share one task: each stage's form type is recorded on the task when it is submitted, and
 * nbri_ehr.necropsyTask.status (shown on each record as necropsyStatus) tracks the workflow.
 */
public abstract class NBRINecropsyBaseFormType extends NBRIBaseTaskFormType
{
    public static final String REQUEST = "Necropsy Request";
    public static final String PRELIMINARY = "Necropsy";
    public static final String FINAL = "Necropsy Final";
    public static final List<String> STAGES = List.of(REQUEST, PRELIMINARY, FINAL);

    // queries owned by each stage, as schema.query in lower case for the client store collection
    protected static final List<String> REQUEST_QUERIES = List.of("nbri_ehr.necropsytask", "study.necropsyrequest");
    protected static final List<String> PRELIMINARY_QUERIES = List.of("study.necropsy", "study.procedurestaff", "study.grosspathology",
            "study.tissuemeasurements", "study.tissuedisposition", "study.preliminarydiagnoses");

    private final String _reviewButton;
    private final String _submitButton;
    private final boolean _requiresPathologyPermission;
    private final List<String> _earlierStageQueries;

    /** @param earlierStageQueries queries an earlier stage owns; this form saves their records only when edited */
    protected NBRINecropsyBaseFormType(DataEntryFormContext ctx, Module owner, String name, List<FormSection> sections,
                                       String reviewButton, String submitButton, boolean requiresPathologyPermission,
                                       List<String> earlierStageQueries)
    {
        super(ctx, owner, name, name, "Pathology", sections);
        _reviewButton = reviewButton;
        _submitButton = submitButton;
        _requiresPathologyPermission = requiresPathologyPermission;
        _earlierStageQueries = earlierStageQueries;

        setStoreCollectionClass("NBRI_EHR.data.NecropsyStoreCollection");
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/data/NecropsyStoreCollection.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/Necropsy.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/buttons/necropsyButtons.js"));
    }

    @Override
    public JSONObject toJSON(boolean includeFormElements)
    {
        JSONObject json = super.toJSON(includeFormElements);
        json.put("earlierStageQueries", _earlierStageQueries);
        return json;
    }

    @Override
    protected List<String> getButtonConfigs()
    {
        List<String> buttons = new ArrayList<>();
        buttons.add("NBRISAVEDRAFTBUTTON");
        buttons.add(_reviewButton);
        buttons.add(_submitButton);
        return buttons;
    }

    @Override
    protected List<String> getMoreActionButtonConfigs()
    {
        List<String> configs = super.getMoreActionButtonConfigs();
        configs.remove("SUBMITANDNEXT");
        configs.remove("FORCESUBMIT");
        configs.remove("DISCARD");
        return configs;
    }

    @Override
    public boolean isAvailable()
    {
        return super.isAvailable() && (!_requiresPathologyPermission ||
                getCtx().getContainer().hasPermission(getCtx().getUser(), EHRPathologyEntryPermission.class));
    }
}

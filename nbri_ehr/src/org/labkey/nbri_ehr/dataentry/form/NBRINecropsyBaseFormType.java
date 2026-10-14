/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
 */
package org.labkey.nbri_ehr.dataentry.form;

import org.labkey.api.ehr.dataentry.DataEntryFormContext;
import org.labkey.api.ehr.dataentry.FormSection;
import org.labkey.api.ehr.security.EHRPathologyEntryPermission;
import org.labkey.api.module.Module;
import org.labkey.api.view.template.ClientDependency;

import java.util.ArrayList;
import java.util.List;

/**
 * The three necropsy stages share one task: each stage's form type is recorded on the task when it is submitted, and
 * nbri_ehr.necropsyTask.status (copied to each record's necropsyStatus) tracks the workflow.
 */
public abstract class NBRINecropsyBaseFormType extends NBRIBaseTaskFormType
{
    public static final String REQUEST = "Necropsy Request";
    public static final String PRELIMINARY = "Necropsy";
    public static final String FINAL = "Necropsy Final";
    public static final List<String> STAGES = List.of(REQUEST, PRELIMINARY, FINAL);

    private final String _reviewButton;
    private final String _submitButton;
    private final boolean _requiresPathologyPermission;

    protected NBRINecropsyBaseFormType(DataEntryFormContext ctx, Module owner, String name, List<FormSection> sections,
                                       String reviewButton, String submitButton, boolean requiresPathologyPermission)
    {
        super(ctx, owner, name, name, "Pathology", sections);
        _reviewButton = reviewButton;
        _submitButton = submitButton;
        _requiresPathologyPermission = requiresPathologyPermission;

        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/model/sources/Necropsy.js"));
        addClientDependency(ClientDependency.supplierFromPath("nbri_ehr/buttons/necropsyButtons.js"));
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

/*
 * Copyright (c) 2026 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.labkey.nbri_ehr;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.UpgradeCode;
import org.labkey.api.ehr.EHRService;
import org.labkey.api.ehr.SharedEHRUpgradeCode;
import org.labkey.api.ehr.buttons.MarkCompletedButton;
import org.labkey.api.ehr.dataentry.DefaultDataEntryFormFactory;
import org.labkey.api.ehr.demographics.ParentsDemographicsProvider;
import org.labkey.api.ehr.demographics.SourceDemographicsProvider;
import org.labkey.api.ehr.history.DefaultAlopeciaDataSource;
import org.labkey.api.ehr.history.DefaultAnimalRecordFlagDataSource;
import org.labkey.api.ehr.history.DefaultClinicalRemarksDataSource;
import org.labkey.api.ehr.history.DefaultNotesDataSource;
import org.labkey.api.ehr.history.DefaultVitalsDataSource;
import org.labkey.api.ehr.security.EHRDataAdminPermission;
import org.labkey.api.ldk.ExtendedSimpleModule;
import org.labkey.api.ldk.buttons.ShowEditUIButton;
import org.labkey.api.ldk.notification.NotificationService;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleContext;
import org.labkey.api.query.DefaultSchema;
import org.labkey.api.query.DetailsURL;
import org.labkey.api.query.QuerySchema;
import org.labkey.api.resource.Resource;
import org.labkey.api.security.roles.RoleManager;
import org.labkey.api.util.NetworkDrive;
import org.labkey.api.view.WebPartFactory;
import org.labkey.api.view.template.ClientDependency;
import org.labkey.nbri_ehr.dataentry.form.*;
import org.labkey.nbri_ehr.demographics.ActiveAssignmentsDemographicsProvider;
import org.labkey.nbri_ehr.demographics.ActiveCasesDemographicsProvider;
import org.labkey.nbri_ehr.demographics.ActiveFlagsDemographicsProvider;
import org.labkey.nbri_ehr.demographics.ActiveTreatmentsDemographicsProvider;
import org.labkey.nbri_ehr.demographics.CagematesDemographicsProvider;
import org.labkey.nbri_ehr.demographics.HousingDemographicsProvider;
import org.labkey.nbri_ehr.demographics.NecropsyStatusDemographicsProvider;
import org.labkey.nbri_ehr.demographics.ProtocolAssignmentDemographicsProvider;
import org.labkey.nbri_ehr.history.*;
import org.labkey.nbri_ehr.notification.NBRIClinicalMoveNotification;
import org.labkey.nbri_ehr.notification.NBRIDeathNotification;
import org.labkey.nbri_ehr.notification.NBRIPregnancyOutcomeNotification;
import org.labkey.nbri_ehr.notification.NBRIProcedureOverdueNotification;
import org.labkey.nbri_ehr.query.NBRI_EHRUserSchema;
import org.labkey.nbri_ehr.security.NBRIEHRVetTechRole;
import org.labkey.nbri_ehr.table.NBRI_EHRCustomizer;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class NBRI_EHRModule extends ExtendedSimpleModule
{
    public static final String NAME = "NBRI_EHR";

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public @Nullable Double getSchemaVersion()
    {
        return 26.002;
    }

    @Override
    @NotNull
    protected Collection<WebPartFactory> createWebPartFactories()
    {
        return Collections.emptyList();
    }

    @Override
    protected void init()
    {
        addController(NBRI_EHRController.NAME, NBRI_EHRController.class);

        EHRService ehrService = EHRService.get();
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/nbriReports.js"), this);
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/window/NBRIRecentCasesWindow.js"), this);
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/window/NBRIRecentRemarksWindow.js"), this);
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("ehr/sharedReports.js"), this);
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/panel/SnapshotPanel.js"), this);
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/panel/NarrowSnapshotPanel.js"), this);
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/panel/BloodSummaryPanel.js"), this);
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/panel/AnimalDetailsPanel.js"), this);
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/window/NBRIClinicalHistoryWindow.js"), this);
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/window/NBRICaseHistoryWindow.js"), this);
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/panel/ClinicalHistoryPanel.js"), this);
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/panel/CaseHistoryPanel.js"), this);

    }

    @Override
    protected void doStartupAfterSpringConfig(ModuleContext moduleContext)
    {
        EHRService ehrService = EHRService.get();
        ehrService.registerModule(this);

        Resource r = getModuleResource("/scripts/nbri_triggers.js");
        assert r != null;
        EHRService.get().registerTriggerScript(this, r);

        EHRService.get().registerTableCustomizer(this, NBRI_EHRCustomizer.class);

        ehrService.addModulePreferringTaskFormEditUI(this);

        ehrService.registerDemographicsProvider(new ActiveFlagsDemographicsProvider(this));
        ehrService.registerDemographicsProvider(new ParentsDemographicsProvider(this));
        ehrService.registerDemographicsProvider(new ActiveAssignmentsDemographicsProvider(this));
        ehrService.registerDemographicsProvider(new ProtocolAssignmentDemographicsProvider(this));
        ehrService.registerDemographicsProvider(new HousingDemographicsProvider(this));
        ehrService.registerDemographicsProvider(new CagematesDemographicsProvider(this));
        ehrService.registerDemographicsProvider(new ActiveCasesDemographicsProvider(this));
        ehrService.registerDemographicsProvider(new ActiveTreatmentsDemographicsProvider(this));
        ehrService.registerDemographicsProvider(new SourceDemographicsProvider(this));
        ehrService.registerDemographicsProvider(new NecropsyStatusDemographicsProvider(this));

        EHRService.get().registerHistoryDataSource(new AnimalGroupsDataSource(this));
        EHRService.get().registerHistoryDataSource(new AnimalGroupsEndDataSource(this));
        EHRService.get().registerHistoryDataSource(new ArrivalDataSource(this));
        EHRService.get().registerHistoryDataSource(new BiopsyDataSource(this));
        EHRService.get().registerHistoryDataSource(new BirthDataSource(this));
        EHRService.get().registerHistoryDataSource(new BloodDrawDataSource(this));
        EHRService.get().registerHistoryDataSource(new BreederDataSource(this));
        EHRService.get().registerHistoryDataSource(new DeathDataSource(this));
        EHRService.get().registerHistoryDataSource(new DefaultAlopeciaDataSource(this));
        EHRService.get().registerHistoryDataSource(new DefaultAnimalRecordFlagDataSource(this));
        EHRService.get().registerHistoryDataSource(new DefaultClinicalRemarksDataSource(this));
        EHRService.get().registerHistoryDataSource(new DefaultNotesDataSource(this));
        EHRService.get().registerHistoryDataSource(new DefaultVitalsDataSource(this));
        EHRService.get().registerHistoryDataSource(new DepartureDataSource(this));
        EHRService.get().registerHistoryDataSource(new DrugAdminDataSource(this));
        EHRService.get().registerHistoryDataSource(new FlagsDataSource(this));
        EHRService.get().registerHistoryDataSource(new FosteringDataSource(this));
        EHRService.get().registerHistoryDataSource(new ExemptionsDataSource(this));
        EHRService.get().registerHistoryDataSource(new HistopathologyDataSource(this));
        EHRService.get().registerHistoryDataSource(new NBRICaseCloseDataSource(this));
        EHRService.get().registerHistoryDataSource(new NBRICaseOpenDataSource(this));
        EHRService.get().registerHistoryDataSource(new NBRIClinicalObservationsDataSource(this));
        EHRService.get().registerHistoryDataSource(new NBRIBehaviorObservationsDataSource(this));
        EHRService.get().registerHistoryDataSource(new NBRIClinicalRemarksDataSource(this));
        EHRService.get().registerHistoryDataSource(new NBRIEndTreatmentOrderDataSource(this));
        EHRService.get().registerHistoryDataSource(new NBRIHousingDataSource(this));
        EHRService.get().registerHistoryDataSource(new NBRIObservationOrdersDataSource(this));
        EHRService.get().registerHistoryDataSource(new NBRIVitalsDataSource(this));
        EHRService.get().registerHistoryDataSource(new PairingsDataSource(this));
        EHRService.get().registerHistoryDataSource(new PhysicalExamDataSource(this));
        EHRService.get().registerHistoryDataSource(new PregnancyDataSource(this));
        EHRService.get().registerHistoryDataSource(new ProceduresDataSource(this));
        EHRService.get().registerHistoryDataSource(new NBRIProcedureOrdersDataSource(this));
        EHRService.get().registerHistoryDataSource(new ProjectAssignmentDataSource(this));
        EHRService.get().registerHistoryDataSource(new ProtocolDataSource(this));
        EHRService.get().registerHistoryDataSource(new SerologyDataSource(this));

        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/nbri_ehr_api"), this);
        ehrService.registerClientDependency(ClientDependency.supplierFromPath("nbri_ehr/nbriOverrides.js"), this);
        ehrService.registerActionOverride("animalHistory", this, "views/animalHistory.html");
        ehrService.registerActionOverride("participantView", this, "views/participantView.html");
        ehrService.registerActionOverride("enterData", this, "views/enterData.html");

        ehrService.registerTriggerScriptOption("datasetsToCloseOnNewEntry", List.of("assignment", "protocolAssignment", "animal_group_members"));
        RoleManager.registerRole(new NBRIEHRVetTechRole());

        EHRService.get().registerMoreActionsButton(new ShowEditUIButton(this, "ehr", "observation_types", EHRDataAdminPermission.class), "ehr", "observation_types");

        EHRService.get().registerMoreActionsButton(new MarkCompletedButton(this, "study", "observation_order", "Set End Date"), "study", "observation_order");
        EHRService.get().registerMoreActionsButton(new MarkCompletedButton(this, "study", "flags", "Set End Date"), "study", "flags");

        registerDataEntry();
        NotificationService.get().registerNotification(new NBRIDeathNotification(this));
        NotificationService.get().registerNotification(new NBRIClinicalMoveNotification(this));
        NotificationService.get().registerNotification(new NBRIProcedureOverdueNotification(this));
        NotificationService.get().registerNotification(new NBRIPregnancyOutcomeNotification(this));

        EHRService.get().registerReportLink(EHRService.REPORT_LINK_TYPE.moreReports, "Printable Necropsy Report", this, DetailsURL.fromString("/nbri_ehr-necropsy.view"), "Pathology");
        EHRService.get().registerReportLink(EHRService.REPORT_LINK_TYPE.moreReports, "Acquisition Report", this, DetailsURL.fromString("/nbri_ehr-acquisitionReport.view"), "Population Overview");


        // Ensure N: is mounted if it's configured, as it's being mapped in via a symlink/shortcut, so we can't
        // recognize paths using it based solely on their drive letter and mount just-in-time
        if (NetworkDrive.getNetworkDrive("N:\\") != null)
        {
            NetworkDrive.exists(new File("N:\\"));
        }
    }

    private void registerDataEntry()
    {
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIAliasFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIAssignmentFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIGroupAssignmentFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIArrivalFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIBirthFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIBulkClinicalFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIDepartureFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIDeathNecropsyFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIDeathFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIHousingFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIMedicationTreatmentFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIPregnancyFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIWeightFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIFlagsFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIExemptionsFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRINotesFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRICasesFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIBehavioralCasesFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIClinicalObservationsFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIClinicalRoundsFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIAnimalTrainingFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIPairingsFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIBulkBehaviorFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIBehaviorRoundsFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIChemistryImportFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRISerologyImportFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIRearrivalFormType.class, this));
        EHRService.get().registerFormType(new DefaultDataEntryFormFactory(NBRIConceptionFormType.class, this));
    }

    @Override
    protected void registerSchemas()
    {
        DefaultSchema.registerProvider(NBRI_EHRSchema.NAME, new DefaultSchema.SchemaProvider(this)
        {
            @Override
            public @NotNull QuerySchema createSchema(DefaultSchema schema, Module module)
            {
                return new NBRI_EHRUserSchema(NBRI_EHRSchema.NAME, null, schema.getUser(), schema.getContainer(), NBRI_EHRSchema.getInstance().getSchema());
            }
        });
    }

    @Override
    public @NotNull Collection<String> getSchemaNames()
    {
        return Collections.singleton(NBRI_EHRSchema.NAME);
    }

    @Override
    public @NotNull UpgradeCode getUpgradeCode()
    {
        return SharedEHRUpgradeCode.getInstance(this);
    }
}
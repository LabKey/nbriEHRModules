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

package org.labkey.test.tests.nbri_ehr;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.api.reader.Readers;
import org.labkey.api.util.FileUtil;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.remoteapi.core.SaveModulePropertiesCommand;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.ImportDataCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.RowsResponse;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.EHR;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.pages.ehr.AnimalHistoryPage;
import org.labkey.test.pages.ehr.EHRAdminPage;
import org.labkey.test.pages.ehr.EHRLookupPage;
import org.labkey.test.pages.ehr.EnterDataPage;
import org.labkey.test.pages.ehr.NotificationAdminPage;
import org.labkey.test.pages.ehr.ParticipantViewPage;
import org.labkey.test.params.ModuleProperty;
import org.labkey.test.tests.ehr.AbstractGenericEHRTest;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.PostgresOnlyTest;
import org.labkey.test.util.ehr.EHRClientAPIHelper;
import org.labkey.test.util.ext4cmp.Ext4ComboRef;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.labkey.test.util.ext4cmp.Ext4GridRef;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.labkey.test.components.html.Input.Input;

@Category({EHR.class})
public class NBRI_EHRTest extends AbstractGenericEHRTest implements PostgresOnlyTest
{
    private static final String PROJECT_NAME = "NBRI";
    private static final String PROJECT_TYPE = "NBRI EHR";
    private static final String NBRI_BASIC_SUBMITTER = "ac_bs@nbritest.com";
    private static final String NBRI_BASIC_SUBMITTER_NAME = "ac bs";
    private static final String NBRI_BASIC_SUBMITTER_VET_TECH = "vet_tech_bs@nbritest.com";
    private static final String NBRI_FULL_SUBMITTER_VET_TECH = "vet_tech_fs@nbritest.com";
    private static final String NBRI_FULL_SUBMITTER_VET = "vet_fs@nbritest.com";
    private static final String NBRI_VET_NAME = "vet fs";
    // Assign the vet-tech role by its class-based name rather than its "EHR Veterinarian Technician" display
    // name: NIRC's role shares that display name, so a display-name lookup can resolve to the wrong module's
    // role, leaving the user without NBRIEHRVetTechPermission (hiding "Submit Necropsy for Review", etc.).
    private static final String NBRI_VET_TECH_ROLE = "NBRIEHRVetTechRole";

    private static final String deadAnimalId = "D5454";
    private static final String departedAnimalId = "H6767";
    private static final String aliveAnimalId = "A4545";
    // Dedicated animal for testScheduledObservationTaskGrouping; provisioned (alive, housed, assigned) in
    // createTestSubjects so the clinical case form raises no warnings that would keep the validation banner up.
    private static final String taskGroupAnimalId = "TESTGRP9090";

    private final String[] weightFields = {"Id", "date", "enddate", "project", "weight", FIELD_QCSTATELABEL, FIELD_OBJECTID, FIELD_LSID, "_recordid", "performedby"};
    private final Object[] weightData1 = {getExpectedAnimalIDCasing("TESTSUBJECT1"), EHRClientAPIHelper.DATE_SUBSTITUTION, null, null, "12", EHRQCState.IN_PROGRESS.label, null, null, "_recordID", 1004};

    DateTimeFormatter _dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @BeforeClass
    public static void setupProject() throws Exception
    {
        NBRI_EHRTest init = getCurrentTest();
        init.doSetup();
    }

    @Override
    public void importStudy()
    {
        File path = FileUtil.appendPath(TestFileUtils.getLabKeyRoot(), org.labkey.api.util.Path.parse(getModulePath() + "/resources/referenceStudy"));
        importFolderByPath(path, getContainerPath(), 1);
        path = TestFileUtils.getSampleData("nbri_ehr/study");
        importFolderByPath(path, getContainerPath(), 2);
    }

    @Override
    protected String getExpectedAnimalIDCasing(String id)
    {
        return id.toUpperCase();
    }

    @Override
    @LogMethod
    protected void populateProtocolRecords() throws Exception
    {
        final String emailDomain = "@ehrstudy.test";
        CreateUserResponse inves1 = _userHelper.createUser(INVES_ID + emailDomain, true);
        CreateUserResponse inves2 = _userHelper.createUser(DUMMY_INVES + emailDomain, true);
        goToEHRFolder();
        _permissionsHelper.addUserToProjGroup(inves1.getEmail(), getProjectName(), INVESTIGATOR.getGroup());
        _permissionsHelper.addUserToProjGroup(inves2.getEmail(), getProjectName(), INVESTIGATOR.getGroup());

        InsertRowsCommand insertCmd = new InsertRowsCommand("ehr", "protocol");

        Map<String, Object> rowMap = new HashMap<>();
        rowMap.put("protocol", PROTOCOL_ID);
        rowMap.put("InvestigatorId", inves1.getUserId());
        rowMap.put("title", PROTOCOL_ID);
        insertCmd.addRow(rowMap);
        rowMap = new HashMap<>();
        rowMap.put("protocol", DUMMY_PROTOCOL);
        rowMap.put("InvestigatorId", inves2.getUserId());
        rowMap.put("title", DUMMY_PROTOCOL);
        insertCmd.addRow(rowMap);

        insertCmd.execute(createDefaultConnection(), getContainerPath());
    }

    public void importFolderByPath(File path, String containerPath, int finishedJobsExpected)
    {
        setPipelineRoot(path.getPath(), false);

        beginAt(WebTestHelper.getBaseURL() + "/" + containerPath + "/pipeline-status-begin.view");
        clickButton("Process and Import Data", defaultWaitForPage);
        _fileBrowserHelper.expandFileBrowserRootNode();
        _fileBrowserHelper.checkFileBrowserFileCheckbox("folder.xml");
        _fileBrowserHelper.selectImportDataAction("Import Folder");

        Locator cb = Locator.checkboxByName("validateQueries");
        waitForElement(cb);
        uncheckCheckbox(cb);
        clickButton("Start Import");

        waitForPipelineJobsToComplete(finishedJobsExpected, "Folder import", false, MAX_WAIT_SECONDS * 2500);
    }

    @Override
    protected String getModuleDirectory()
    {
        return "nbriEHRModules/nbri_ehr";
    }

    @Override
    protected boolean doSetUserPasswords()
    {
        return true;
    }

    @Override
    public String getModulePath()
    {
        return "/server/modules/" + getModuleDirectory();
    }

    @Override
    protected File getStudyPolicyXML()
    {
        return TestFileUtils.getSampleData("nbriEHRStudyPolicy.xml");
    }

    @Override
    @LogMethod
    protected void populateInitialData() throws Exception
    {
        List<ModuleProperty> props = List.of(
                new ModuleProperty("EHR", "/" + getProjectName(), "EHRCustomModule", "NBRI_EHR")
        );
        SaveModulePropertiesCommand command = new SaveModulePropertiesCommand(props);
        command.execute(createDefaultConnection(), "/");

        beginAt(WebTestHelper.buildURL("ehr", getContainerPath(), "populateLookupData", Map.of("manifest", "lookupsManifestTest")));

        waitForElement(Locator.linkWithText("Populate Lookups"));
        click(Locator.linkWithText("Populate Lookups"));
        acceptAlert();

        waitFor(() -> Input(Locator.textarea("populateLookupResults"), getDriver()).waitFor().getValue().contains("Loading lookups is complete."),
                "Lookups didn't finish loading", 60000);

        waitForElement(Locator.linkWithText("Populate Reports"));
        click(Locator.linkWithText("Populate Reports"));
        acceptAlert();

        waitFor(() -> Input(Locator.textarea("populateLookupResults"), getDriver()).waitFor().getValue().contains("Loading reports is complete."),
                "Reports didn't finish loading", 60000);
        populateFormulary();
    }

    private void populateFormulary() throws IOException, CommandException
    {
        InsertRowsCommand insertRowsCommand = new InsertRowsCommand("ehr_lookups", "drug_defaults");
        insertRowsCommand.addRow(new HashMap<>()
        {
            {
                put("code", "NBRI-001");
            }
        });

        RowsResponse saveRowsResponse = insertRowsCommand.execute(getApiHelper().getConnection(), getContainerPath());
    }

    @Override
    protected void populateRoomRecords() throws Exception
    {
        InsertRowsCommand insertCmd = new InsertRowsCommand("ehr_lookups", "rooms");
        Map<String, Object> rowMap = new HashMap<>();
        rowMap.put("name", ROOM_ID);
        rowMap.put("floor", "floor1");
        rowMap.put("housingType", 1);
        rowMap.put("housingCondition", 1);
        insertCmd.addRow(rowMap);

        rowMap = new HashMap<>();
        rowMap.put("name", ROOM_ID2);
        rowMap.put("floor", "floor2");
        rowMap.put("housingType", 1);
        rowMap.put("housingCondition", 1);
        insertCmd.addRow(rowMap);

        insertCmd.execute(createDefaultConnection(), getContainerPath());
    }

    private void doSetup() throws Exception
    {
        initProject(PROJECT_TYPE);
        goToEHRFolder();
        createTestSubjects();
        addNBRIEhrLinks();
        addExtensibleCols();
        enableSiteNotification();
        populateLocations();
        addUsersAndPermissions();//create users and assign roles, created for Death/Necropsy form, but users can be repurposed for other forms.
        populateEHRTables();
    }

    @LogMethod
    private void populateEHRTables()
    {
        goToEHRFolder();
        File fileName = FileUtil.appendPath(TestFileUtils.getLabKeyRoot(), org.labkey.api.util.Path.parse(getModulePath() + "/resources/data/observation_types.tsv"));
        ImportDataCommand command = new ImportDataCommand("ehr", "observation_types");
        command.setFile(fileName);
        try
        {
            command.execute(getApiHelper().getConnection(), getContainerPath());
        }
        catch (IOException | CommandException e)
        {
            log("Error while inserting into observation_types " + e.getMessage());
        }

    }

    private void enableSiteNotification()
    {
        log("Enabling the notification at the site level");
        goToAdminConsole().clickNotificationServiceAdmin();
        _ext4Helper.selectComboBoxItem("Status of Notification Service:", "Enabled");
        clickButton("Save", 0);
        _helper.clickExt4WindowBtn("Success", "OK");
    }

    private void enableNotification(String notification)
    {
        goToEHRFolder();
        _containerHelper.enableModule("Dumbster");
        log("Setup the notification service for this container");
        EHRAdminPage.beginAt(this, getContainerPath());
        NotificationAdminPage notificationAdminPage = EHRAdminPage.clickNotificationService(this);
        notificationAdminPage.setNotificationUserAndReplyEmail(DATA_ADMIN_USER);
        notificationAdminPage.addManageUsers("org.labkey.nbri_ehr.notification.NBRIDeathNotification", "EHR Administrators");
        notificationAdminPage.enableRequestAdminAlerts(notification);
    }

    @LogMethod
    private void populateLocations() throws IOException, CommandException
    {
        goToEHRFolder();
        log("Inserting values in rooms");
        InsertRowsCommand roomCmd = new InsertRowsCommand("ehr_lookups", "rooms");
        roomCmd.addRow(Map.of("name", "R1", "floor", "F1"));
        roomCmd.addRow(Map.of("name", "R2", "floor", "F2"));
        roomCmd.addRow(Map.of("name", "R3", "floor", "F3"));
        roomCmd.execute(getApiHelper().getConnection(), getContainerPath());

        log("Inserting values in cage");
        InsertRowsCommand cageCmd = new InsertRowsCommand("ehr_lookups", "cage");
        cageCmd.addRow(Map.of("location", "L1", "cage", "C1", "room", "R1"));
        cageCmd.addRow(Map.of("location", "L2", "cage", "C2", "room", "R1"));
        cageCmd.addRow(Map.of("location", "L3", "cage", "C3", "room", "R2"));
        cageCmd.addRow(Map.of("location", "L4", "cage", "C4", "room", "R3"));
        cageCmd.execute(getApiHelper().getConnection(), getContainerPath());
    }

    private void addExtensibleCols()
    {
        log("Setup the EHR table definitions");
        EHRAdminPage.beginAt(this, getContainerPath());
        clickAndWait(Locator.linkWithText("EHR Extensible Columns"));

        log("Load EHR table definitions");
        click(Locator.linkWithText("Load EHR table definitions"));
        waitForElement(Locator.tagWithClass("span", "x4-window-header-text").withText("Success"));
        assertExt4MsgBox("EHR tables updated successfully.", "OK");

        log("Load EHR_Lookup table definitions");
        Locator.linkWithText("Load EHR_Lookup table definitions").findElement(getDriver()).click();
        waitForElement(Ext4Helper.Locators.window().withDescendant(Window.Locators.title().withText("Success")));
        assertExt4MsgBox("EHR_Lookups tables updated successfully.", "OK");
    }

    private void addNBRIEhrLinks()
    {
        navigateToFolder(getProjectName(), getFolderName());
        (new PortalHelper(this)).addWebPart("NBRI EHR Links");
    }

    @Override
    protected String getMale()
    {
        return "3";
    }

    @Override
    protected String getFemale()
    {
        return "2";
    }

    @Test
    public void testSetup()
    {

    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected String getAnimalHistoryPath()
    {
        return "/ehr/" + PROJECT_NAME + "/animalHistory.view?";
    }

    @Override
    protected List<String> skipLinksForValidation()
    {
        List<String> links = new ArrayList<>(super.skipLinksForValidation());
        links.add("ehr-colonyOverview.view"); // Not fully implemented
        return links;
    }

    @Override
    protected void createTestSubjects() throws Exception
    {
        String[] fields;
        Object[][] data;
        SimplePostCommand insertCommand;

        //insert into demographics
        log("Creating test subjects");
        fields = new String[]{"Id", "Species", "Birth", "Gender", "date", "calculated_status", "objectid", "performedby"};
        data = new Object[][]{
                {SUBJECTS[0], "Rhesus", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {SUBJECTS[1], "Cynomolgus", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {SUBJECTS[2], "Marmoset", (new Date()).toString(), getFemale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {SUBJECTS[3], "Cynomolgus", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {SUBJECTS[4], "Cynomolgus", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "demographics", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "demographics", new Filter("Id", StringUtils.join(SUBJECTS, ";"), Filter.Operator.IN));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        //for simplicity, also create the animals from MORE_ANIMAL_IDS right now
        data = new Object[][]{
                {MORE_ANIMAL_IDS[0], "Rhesus", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {MORE_ANIMAL_IDS[1], "Cynomolgus", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {MORE_ANIMAL_IDS[2], "Marmoset", (new Date()).toString(), getFemale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {MORE_ANIMAL_IDS[3], "Cynomolgus", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {MORE_ANIMAL_IDS[4], "Cynomolgus", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "demographics", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "demographics", new Filter("Id", StringUtils.join(MORE_ANIMAL_IDS, ";"), Filter.Operator.IN));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        //used as initial dates
        Date pastDate1 = TIME_FORMAT.parse("2012-01-03 09:30");
        Date pastDate2 = TIME_FORMAT.parse("2012-05-03 19:20");

        //set housing
        log("Creating initial housing records");
        fields = new String[]{"Id", "date", "enddate", "room", "cage", "performedby"};
        data = new Object[][]{
                {SUBJECTS[0], pastDate1, pastDate2, getRooms()[0], CAGES[0], 1004},
                {SUBJECTS[0], pastDate2, null, getRooms()[0], CAGES[0], 1004},
                {SUBJECTS[1], pastDate1, pastDate2, getRooms()[0], CAGES[0], 1004},
                {SUBJECTS[1], pastDate2, null, getRooms()[2], CAGES[2], 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "Housing", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "Housing", new Filter("Id", StringUtils.join(SUBJECTS, ";"), Filter.Operator.IN));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        //set a base weight
        log("Setting initial weights");
        fields = new String[]{"Id", "date", "weight", "QCStateLabel", "performedby"};
        data = new Object[][]{
                {SUBJECTS[0], pastDate2, 10.5, EHRQCState.COMPLETED.label, 1004},
                {SUBJECTS[0], new Date(), 12, EHRQCState.COMPLETED.label, 1004},
                {SUBJECTS[1], new Date(), 12, EHRQCState.COMPLETED.label, 1004},
                {SUBJECTS[2], new Date(), 12, EHRQCState.COMPLETED.label, 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "Weight", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "Weight", new Filter("Id", StringUtils.join(SUBJECTS, ";"), Filter.Operator.IN));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        //set assignment
        log("Setting initial assignments");
        fields = new String[]{"Id", "date", "enddate", "project", "performedby"};
        data = new Object[][]{
                {SUBJECTS[0], pastDate1, pastDate2, PROJECTS[0], 1004},
                {SUBJECTS[1], pastDate1, pastDate2, PROJECTS[0], 1004},
                {SUBJECTS[1], pastDate2, null, PROJECTS[2], 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "Assignment", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "Assignment", new Filter("Id", StringUtils.join(SUBJECTS, ";"), Filter.Operator.IN));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        // Fully provision the task-grouping test animal (alive demographics, current housing, active assignment) so
        // the clinical case form has no unknown-animal warnings to keep the validation banner from clearing.
        log("Creating task grouping test subject");
        fields = new String[]{"Id", "Species", "Birth", "Gender", "date", "calculated_status", "objectid", "performedby"};
        data = new Object[][]{
                {taskGroupAnimalId, "Rhesus", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "demographics", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "demographics", new Filter("Id", taskGroupAnimalId));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        fields = new String[]{"Id", "date", "enddate", "room", "cage", "performedby"};
        data = new Object[][]{
                {taskGroupAnimalId, pastDate1, null, getRooms()[0], CAGES[0], 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "Housing", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "Housing", new Filter("Id", taskGroupAnimalId));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        fields = new String[]{"Id", "date", "enddate", "project", "performedby"};
        data = new Object[][]{
                {taskGroupAnimalId, pastDate1, null, PROJECTS[0], 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "Assignment", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "Assignment", new Filter("Id", taskGroupAnimalId));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        primeCaches();
    }

    @Override
    protected String[] getWeightFields()
    {
        return weightFields;
    }

    @Override
    protected Object[] getWeightData1()
    {
        return weightData1;
    }

    @Test
    public void testWeightValidation()
    {
        //initialize weight of subject 3
        String[] fields;
        Object[][] data;
        SimplePostCommand insertCommand;
        fields = new String[]{"Id", "date", "weight", "QCStateLabel", "performedby"};
        data = new Object[][]{
                {SUBJECTS[3], new Date(), 12, EHRQCState.COMPLETED.label, 1004},
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "Weight", "lsid", fields, data);
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        //expect weight out of range
        data = new Object[][]{
                {SUBJECTS[3], new Date(), null, null, 120, EHRQCState.IN_PROGRESS.label, null, null, "recordID", 1004}
        };
        Map<String, List<String>> expected = new HashMap<>();
        expected.put("weight", Arrays.asList(
                "WARN: Weight above the allowable value of 20.0 kg for Cynomolgus",
                "INFO: Weight gain of >10%. Last weight 12 kg")
        );
        getApiHelper().testValidationMessage(DATA_ADMIN.getEmail(), "study", "weight", getWeightFields(), data, expected);

        //expect INFO for +10% diff
        data = new Object[][]{
                {SUBJECTS[3], new Date(), null, null, 20, EHRQCState.IN_PROGRESS.label, null, null, "recordID", 1004}
        };
        expected = new HashMap<>();
        expected.put("weight", Collections.singletonList("INFO: Weight gain of >10%. Last weight 12 kg"));
        getApiHelper().testValidationMessage(DATA_ADMIN.getEmail(), "study", "weight", getWeightFields(), data, expected);

        //expect INFO for -10% diff
        data = new Object[][]{
                {SUBJECTS[3], new Date(), null, null, 5, EHRQCState.IN_PROGRESS.label, null, null, "recordID", 1004}
        };
        expected = new HashMap<>();
        expected.put("weight", Collections.singletonList("INFO: Weight drop of >10%. Last weight 12 kg"));
        getApiHelper().testValidationMessage(DATA_ADMIN.getEmail(), "study", "weight", getWeightFields(), data, expected);

        //TODO: test error threshold
    }

    @Test
    public void testArrivalForm()
    {
        String arrivedAnimal = "30905";
        LocalDateTime now = LocalDateTime.now();

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Arrivals"));
        lockForm();

        Ext4GridRef arrivals = _helper.getExt4GridForFormSection("Arrivals");
        _helper.addRecordToGrid(arrivals);
        arrivals.setGridCellJS(1, "date", now.minusDays(1).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_STRING)));
        arrivals.setGridCell(1, "arrivalType", "Non-quarantine Arrival");
        arrivals.setGridCell(1, "acquisitionType", "Lab Transfer (Wild Born)");
        arrivals.setGridCell(1, "Id", arrivedAnimal);
        arrivals.setGridCell(1, "cage", "C1");
        arrivals.setGridCell(1, "project", "640991");
        arrivals.setGridCell(1, "arrivalProtocol", "dummyprotocol");
        arrivals.setGridCell(1, "Id/demographics/gender", "female");
        arrivals.setGridCell(1, "Id/demographics/geographic_origin", "BRAZIL");
        arrivals.setGridCell(1, "Id/demographics/species", "Macaca nemestrina PIG");
        arrivals.setGridCellJS(1, "Id/demographics/birth", now.minusDays(7).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_STRING)));
        arrivals.setGridCell(1, "sourceFacility", "BIOQUAL, Inc.");
        submitForm("Submit Final", "Finalize");

        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("study", "arrival");
        table.setFilter("Id", "Equals", arrivedAnimal);
        CustomizeView view = table.openCustomizeGrid();
        view.addColumn("cage");
        view.addColumn("project");
        view.addColumn("arrivalProtocol");
        view.applyCustomView();
        Assert.assertEquals("Invalid Arrival record", Arrays.asList(arrivedAnimal), table.getRowDataAsText(0, "Id"));
        Assert.assertEquals("Invalid Arrival record", Arrays.asList("C1"), table.getRowDataAsText(0, "cage"));
        Assert.assertEquals("Invalid Arrival record", Arrays.asList("640991"), table.getRowDataAsText(0, "project"));
        Assert.assertEquals("Invalid Arrival record", Arrays.asList("dummyprotocol"), table.getRowDataAsText(0, "arrivalProtocol"));

        verifyRowCreated("study", "birth", arrivedAnimal, 1);
        verifyRowCreated("study", "assignment", arrivedAnimal, 1);
        verifyRowCreated("study", "protocolAssignment", arrivedAnimal, 1);
        verifyRowCreated("study", "demographics", arrivedAnimal, 1);
        verifyRowCreated("study", "housing", arrivedAnimal, 1);
    }

    @Test
    public void testBirthForm() throws IOException, CommandException
    {
        String bornAnimal = "80801";
        String conceptId = "TESTCONCEPT1";
        LocalDateTime now = LocalDateTime.now();

        log("Creating conception record");
        InsertRowsCommand conception = new InsertRowsCommand("nbri_ehr", "Conception");
        conception.addRow(Map.of("ConceptId", conceptId, "ConceptDate", now.minusDays(160), "Dam", "TEST4551032"));
        conception.execute(getApiHelper().getConnection(), getContainerPath());

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Birth"));
        lockForm();

        Ext4GridRef births = _helper.getExt4GridForFormSection("Births");
        _helper.addRecordToGrid(births);
        births.setGridCellJS(1, "date", now.minusDays(1).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_STRING)));
        births.setGridCell(1, "Id", bornAnimal);
        births.setGridCell(1, "cage", "C3");
        births.setGridCell(1, "Id/demographics/species", "Cebus apella CAP");
        births.setGridCell(1, "Id/demographics/gender", "female");
        births.setGridCell(1, "project", "795644");
        births.setGridCell(1, "birthProtocol", "protocol101");
        births.setGridCell(1, "conceptId", conceptId);
        submitForm("Submit Final", "Finalize");

        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("study", "birth");
        table.setFilter("Id", "Equals", bornAnimal);
        Assert.assertEquals("Invalid Birth record", Arrays.asList(bornAnimal), table.getRowDataAsText(0, "Id"));
        Assert.assertEquals("Invalid Birth record", Arrays.asList("C3"), table.getRowDataAsText(0, "cage"));
        Assert.assertEquals("Invalid Birth record", Arrays.asList("795644"), table.getRowDataAsText(0, "project"));
        Assert.assertEquals("Invalid Birth record", Arrays.asList("protocol101"), table.getRowDataAsText(0, "birthProtocol"));
        Assert.assertEquals("Invalid Birth record", Arrays.asList(conceptId), table.getRowDataAsText(0, "conceptId"));

        verifyRowCreated("study", "assignment", bornAnimal, 1);
        verifyRowCreated("study", "protocolAssignment", bornAnimal, 1);
        verifyRowCreated("study", "housing", bornAnimal, 1);
        verifyRowCreated("study", "demographics", bornAnimal, 1);

        log("Verifying conception outcome in ConceptionsByDam");
        goToSchemaBrowser();
        DataRegionTable report = viewQueryData("nbri_ehr", "ConceptionsByDam");
        report.setFilter("ConceptId", "Equals", conceptId);
        Assert.assertEquals("Invalid ConceptionsByDam row", Arrays.asList("Live Birth"), report.getRowDataAsText(0, "conceptionOutcome"));
    }

    @Test
    public void testPregnancyForm() throws IOException, CommandException
    {
        String animalId = "TEST4551032";
        String conceptId = "TESTCONCEPT2";
        LocalDateTime now = LocalDateTime.now();

        log("Creating conception record");
        InsertRowsCommand conception = new InsertRowsCommand("nbri_ehr", "Conception");
        conception.addRow(Map.of("ConceptId", conceptId, "ConceptDate", now.minusDays(90), "Dam", animalId));
        conception.execute(getApiHelper().getConnection(), getContainerPath());

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Pregnancy Outcomes"));
        lockForm();

        Ext4GridRef outcomes = _helper.getExt4GridForFormSection("Pregnancy Outcomes");
        _helper.addRecordToGrid(outcomes);
        outcomes.setGridCellJS(1, "date", now.minusDays(1).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_STRING)));
        outcomes.setGridCell(1, "Id", animalId);
        outcomes.setGridCell(1, "result", "Stillborn");
        outcomes.setGridCell(1, "conceptId", conceptId);
        submitForm("Submit Final", "Finalize");

        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("study", "pregnancy");
        table.setFilter("Id", "Equals", animalId);
        Assert.assertEquals("Invalid Pregnancy Outcome record", Arrays.asList(animalId), table.getRowDataAsText(0, "Id"));
        Assert.assertEquals("Invalid Pregnancy Outcome record", Arrays.asList("Stillborn"), table.getRowDataAsText(0, "result"));
        Assert.assertEquals("Invalid Pregnancy Outcome record", Arrays.asList(conceptId), table.getRowDataAsText(0, "conceptId"));

        log("Verifying conception outcome in ConceptionsByDam");
        goToSchemaBrowser();
        DataRegionTable report = viewQueryData("nbri_ehr", "ConceptionsByDam");
        report.setFilter("ConceptId", "Equals", conceptId);
        Assert.assertEquals("Invalid ConceptionsByDam row", Arrays.asList(animalId), report.getRowDataAsText(0, "Id"));
        Assert.assertEquals("Invalid ConceptionsByDam row", Arrays.asList("Stillborn"), report.getRowDataAsText(0, "conceptionOutcome"));
    }

    @Test
    public void testConceptionForm()
    {
        String damId = "TEST4551032";
        String sireId = "44444";
        String conceptId = "TESTCONCEPT3";
        LocalDateTime now = LocalDateTime.now();

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Conception"));
        lockForm();

        Ext4GridRef conceptions = _helper.getExt4GridForFormSection("Conception");
        _helper.addRecordToGrid(conceptions);
        conceptions.setGridCell(1, "ConceptId", conceptId);
        conceptions.setGridCellJS(1, "ConceptDate", now.minusDays(30).format(_dateFormat));
        conceptions.setGridCellJS(1, "ConceptTermDate", now.plusDays(135).format(_dateFormat));
        conceptions.setGridCell(1, "Dam", damId);
        conceptions.setGridCell(1, "Sire", sireId);
        conceptions.setGridCell(1, "Remark", "Conception entry test");
        submitForm("Submit Final", "Finalize");

        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("nbri_ehr", "Conception");
        table.setFilter("ConceptId", "Equals", conceptId);
        Assert.assertEquals("Invalid Conception record", Arrays.asList(damId), table.getRowDataAsText(0, "Dam"));
        Assert.assertEquals("Invalid Conception record", Arrays.asList(sireId), table.getRowDataAsText(0, "Sire"));
        Assert.assertEquals("Invalid Conception record", Arrays.asList("Conception entry test"), table.getRowDataAsText(0, "Remark"));

        log("Verifying unmatched conception appears as Unknown in ConceptionsByDam");
        goToSchemaBrowser();
        DataRegionTable report = viewQueryData("nbri_ehr", "ConceptionsByDam");
        report.setFilter("ConceptId", "Equals", conceptId);
        Assert.assertEquals("Invalid ConceptionsByDam row", Arrays.asList(damId), report.getRowDataAsText(0, "Id"));
        Assert.assertEquals("Invalid ConceptionsByDam row", Arrays.asList("Unknown"), report.getRowDataAsText(0, "conceptionOutcome"));
    }

    @Test
    public void testClinicalObservation()
    {
        String animalId = "TEST4551032";

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Clinical Cases"));

        //Fill out Clinical Case section with Id, Date, Open Remark
        Ext4FieldRef problem = _helper.getExt4FieldForFormSection("Clinical Case", "Problem Area");
        problem.clickTrigger();
        problem.setValue("General abnormality");
        _helper.setDataEntryField("openRemark", "Clinical Case WorkFlow - Test");
        _helper.setDataEntryField("plan", "Case plan - Test");
        _helper.getExt4FieldForFormSection("Clinical Case", "Open Date").setValue(LocalDateTime.now().minusDays(1).format(_dateFormat));
        setFormElement(Locator.name("Id"), animalId);

        _helper.setDataEntryField("s", "Subjective for " + animalId);
        _helper.setDataEntryField("remark", "Remarks for " + animalId);

        Ext4GridRef observationOrders = _helper.getExt4GridForFormSection("Observation Orders");
        _helper.addRecordToGrid(observationOrders);
        observationOrders.setGridCell(1, "category", "Lameness");
        observationOrders.setGridCell(1, "frequency", "QID");
        submitForm("Submit Final", "Finalize");

        log("Verifying Active Clinical Observation Orders");
        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Active Clinical Observation Orders"));
        DataRegionTable table = new AnimalHistoryPage<>(getDriver()).getActiveReportDataRegion();
        table.setFilter("Id", "Equals", animalId);

        List<String> expected = Arrays.asList("Activity", "Appetite", "BCS", "Hydration", "Stool", "Verified Id?", "Lameness");
        List<String> actual = table.getColumnDataAsText("category");
        Collections.sort(expected);
        Collections.sort(actual);

        Assert.assertEquals("Incorrect active clinical observation orders", expected, actual);

        log("Verifying Today's Observation Schedule");
        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Today's Observation Schedule"));
        table = new AnimalHistoryPage<>(getDriver()).getActiveReportDataRegion();
        table.setFilter("Id", "Equals", animalId);
        Assert.assertEquals("Incorrect rows in Today's Observation Schedule", 4, table.getDataRowCount());
        Assert.assertEquals("Incorrect observation title", "Daily Clinical Observations; Lameness", table.getDataAsText(0, "observationList"));
        Assert.assertEquals("Status is not updated", "", table.getDataAsText(0, "observationStatus"));

        // Capture existing observation-form tasks so we can confirm that entering scheduled
        // observations groups them onto the form's task without leaving an empty task behind.
        Set<String> obsTasksBefore = getObservationFormTaskIds();
        table.link(0, "observationRecord").click();

        switchToWindow(1);
        waitForText(animalId);
        Ext4GridRef observation = _helper.getExt4GridForFormSection("Observations");
        observation.setGridCell(1, "observation", "Lame on left leg");
        observation.setGridCellJS(1, "remark", "remark for lameness");

        observation.setGridCell(2, "observation", "0-1 Extremely Lethargic");
        observation.setGridCellJS(2, "remark", "remark for activity");

        observation.setGridCell(3, "observation", "Normal to low");
        observation.setGridCellJS(3, "remark", "remark for Appetite");

        observation.setGridCell(4, "observation", "2.5");
        observation.setGridCellJS(4, "remark", "remark for BCS");

        observation.setGridCell(5, "observation", "10%");
        observation.setGridCellJS(5, "remark", "remark for Hydration");

        observation.setGridCell(6, "observation", "M/F");
        observation.setGridCellJS(6, "remark", "remark for Stool");

        observation.setGridCell(7, "observation", "No");
        observation.setGridCellJS(7, "remark", "remark for Verified Id?");
        submitForm("Submit Final", "Finalize");

        table = new AnimalHistoryPage<>(getDriver()).getActiveReportDataRegion();
        Assert.assertEquals("Status is not updated", "Completed", table.getDataAsText(0, "observationStatus"));

        // This animal has a single clinical case, so every scheduled observation belongs to that one
        // order group and stays on the form's task: one task group, and no empty task created.
        verifyScheduledObservationTaskGrouping(animalId, obsTasksBefore, 1);

        log("Closing the case");
        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Active Clinical Cases"));
        DataRegionTable activeClinicalCases = new AnimalHistoryPage<>(getDriver()).getActiveReportDataRegion();
        activeClinicalCases.link(0, "caseCheck").click();
        switchToWindow(2);

        waitForText(animalId);
        waitForTextToDisappear("Id is required");
        setCaseSubjective("Closing the case");
        waitAndClick(Ext4Helper.Locators.ext4Button("Edit"));
        _helper.getExt4FieldForFormSection("Clinical Case", "Close Date").setValue(LocalDateTime.now().format(_dateFormat));
        _helper.setDataEntryField("closeRemark", "Case closed.");

        submitForm("Submit Final", "Finalize");

        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Active Clinical Cases"));
        ParticipantViewPage<?> reportPage = new AnimalHistoryPage<>(getDriver()).clickCategoryTab("Clinical")
                .clickReportTab("All Clinical Cases");
        table = reportPage.getActiveReportDataRegion();
        Assert.assertEquals("Case not closed correctly ", LocalDateTime.now().format(_dateFormat) + " 00:00", table.getDataAsText(0, "enddate"));
    }

    @Test
    public void testBulkClinicalEntry()
    {
        String animalId = "44443";
        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Bulk Clinical Entry"));

        Ext4GridRef observationOrders = _helper.getExt4GridForFormSection("Observation Orders");
        _helper.addRecordToGrid(observationOrders);
        observationOrders.setGridCell(1, "Id", animalId);
        observationOrders.setGridCell(1, "category", "Dental/Oral Observations");
        observationOrders.setGridCell(1, "frequency", "TID");
        waitAndClick(_helper.getDataEntryButton("Submit for Review"));
        Window<?> submitForReview = new Window<>("Submit For Review", getDriver());
        _ext4Helper.selectComboBoxItem("Assign To:", Ext4Helper.TextMatchTechnique.CONTAINS, NBRI_VET_NAME);
        submitForReview.clickButton("Submit");

        goToEHRFolder();
        impersonate(NBRI_FULL_SUBMITTER_VET);
        waitAndClickAndWait(Locator.linkContainingText("My Review Tasks"));
        DataRegionTable taskTable = new DataRegionTable.DataRegionFinder(getDriver()).withName("query").waitFor();
        taskTable.link(0, "rowid").click();
        waitAndClickAndWait(Locator.linkWithText("Edit"));
        submitForm("Submit Final", "Finalize");
        stopImpersonating();

        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Today's Observation Schedule"));
        DataRegionTable table = new AnimalHistoryPage<>(getDriver()).getActiveReportDataRegion();
        table.setFilter("Id", "Equals", animalId);
        Assert.assertEquals("Incorrect number of rows(TID) for " + animalId, 3, table.getDataRowCount());

        table.link(0, "observationRecord").click();
        switchToWindow(1);
        waitForText(animalId);
        Ext4GridRef observation = _helper.getExt4GridForFormSection("Observations");
        observation.setGridCell(1, "observation", "Fractured Tooth");
        observation.setGridCellJS(1, "remark", "remark for " + animalId);
        submitForm("Submit Final", "Finalize");

        table = new AnimalHistoryPage<>(getDriver()).getActiveReportDataRegion();
        table.setFilter("Id", "Equals", animalId);
        Assert.assertEquals("Status is not updated ", "Completed", table.getDataAsText(0, "observationStatus"));
    }

    // The ehr.tasks formtype for the clinical observations data entry form (NBRIClinicalObservationsFormType.NAME).
    private static final String NBRI_OBSERVATIONS_FORM_TYPE = "Observations";

    // Valid Observation/Score values keyed by daily clinical observation category. The Observation/Score
    // field is category-dependent, so each value must be legal for its category.
    private static final Map<String, String> NBRI_DAILY_OBS_VALUES = Map.of(
            "Activity", "0-1 Extremely Lethargic",
            "Appetite", "Normal to low",
            "BCS", "2.5",
            "Hydration", "10%",
            "Stool", "M/F",
            "Verified Id?", "No");

    @Test
    public void testScheduledObservationTaskGrouping()
    {
        String animalId = taskGroupAnimalId;

        // Two concurrent clinical cases for the same animal each generate their own set of daily
        // observation orders at the same scheduled slot (today at 8:00 AM). A single schedule entry
        // therefore matches two orders per category, each carrying a distinct order taskid. The entered
        // observations must be grouped by that order taskid -- the first group reuses the form's own
        // task and the second gets a freshly created task -- so the entries end up under exactly two
        // tasks with no empty task left behind.
        // The first case finalizes through the normal "Finalize Form" confirmation. The second case is for
        // the same animal and problem area, so its submission instead raises the "Similar Case Exists"
        // confirmation -- acknowledge that one to finalize it.
        createClinicalCase(animalId, "Finalize");
        createClinicalCase(animalId, "Similar Case Exists");

        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Today's Observation Schedule"));
        DataRegionTable table = new AnimalHistoryPage<>(getDriver()).getActiveReportDataRegion();
        table.setFilter("Id", "Equals", animalId);
        Assert.assertEquals("Both cases' orders should collapse to a single schedule row for " + animalId, 1, table.getDataRowCount());

        Set<String> obsTasksBefore = getObservationFormTaskIds();
        table.link(0, "observationRecord").click();
        switchToWindow(1);
        waitForText(animalId);
        enterScheduledObservations();

        // Each of the six daily categories matched two orders, so two entries per category were created,
        // grouped into exactly two tasks (one per originating order taskid) with no empty task.
        verifyScheduledObservationTaskGrouping(animalId, obsTasksBefore, 2);

        Map<String, Integer> entriesPerCategory = new HashMap<>();
        for (Map<String, Object> row : getClinicalObservations(animalId))
            entriesPerCategory.merge(String.valueOf(row.get("category")), 1, Integer::sum);
        Assert.assertEquals("Expected the six daily observation categories", NBRI_DAILY_OBS_VALUES.size(), entriesPerCategory.size());
        entriesPerCategory.forEach((category, count) ->
                Assert.assertEquals("Expected two entries (one per matching order) for category " + category, Integer.valueOf(2), count));
    }

    // Creates and finalizes a minimal clinical case for the animal. The case's open date is set to
    // yesterday so the auto-generated daily observation orders land on today's observation schedule.
    // confirmWindowTitle is the finalize-confirmation dialog expected on submit: "Finalize" for a brand
    // new case, or "Similar Case Exists" when the animal already has an active case for the same problem.
    private void createClinicalCase(String animalId, String confirmWindowTitle)
    {
        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Clinical Cases"));
        Ext4FieldRef problem = _helper.getExt4FieldForFormSection("Clinical Case", "Problem Area");
        problem.clickTrigger();
        problem.setValue("General abnormality");
        _helper.setDataEntryField("openRemark", "Clinical Case for " + animalId);
        _helper.setDataEntryField("plan", "Case plan for " + animalId);
        _helper.getExt4FieldForFormSection("Clinical Case", "Open Date").setValue(LocalDateTime.now().minusDays(1).format(_dateFormat));
        setFormElement(Locator.name("Id"), animalId);
        _helper.setDataEntryField("s", "Subjective for " + animalId);
        _helper.setDataEntryField("remark", "Remarks for " + animalId);
        submitForm("Submit Final", confirmWindowTitle);
    }

    // Fills in the Observations grid opened from the schedule, setting a valid value and remark for each
    // category row regardless of the grid's row order, then submits.
    private void enterScheduledObservations()
    {
        Ext4GridRef observation = _helper.getExt4GridForFormSection("Observations");
        int rowCount = observation.getRowCount();
        for (int row = 1; row <= rowCount; row++)
        {
            String category = String.valueOf(observation.getFieldValue(row, "category"));
            String value = NBRI_DAILY_OBS_VALUES.get(category);
            if (value != null)
                observation.setGridCell(row, "observation", value);
            observation.setGridCellJS(row, "remark", "remark for " + category);
        }
        submitForm("Submit Final", "Finalize");
    }

    // Asserts that the animal's scheduled observations are grouped under the expected number of distinct
    // tasks and that no observation-form task created while entering them was left empty.
    private void verifyScheduledObservationTaskGrouping(String animalId, Set<String> obsTasksBefore, int expectedTaskGroups)
    {
        List<Map<String, Object>> obsRows = getClinicalObservations(animalId);
        Assert.assertFalse("Expected scheduled clinical observations for " + animalId, obsRows.isEmpty());

        Set<String> taskIds = new HashSet<>();
        for (Map<String, Object> row : obsRows)
        {
            Object taskId = row.get("taskid");
            Assert.assertNotNull("A scheduled observation is missing its taskid", taskId);
            taskIds.add(String.valueOf(taskId));
        }
        Assert.assertEquals("Scheduled observations should be grouped under " + expectedTaskGroups + " task(s)", expectedTaskGroups, taskIds.size());

        // No empty task: every observation-form task created while entering these observations must carry
        // at least one observation. The old behavior abandoned the form's task (leaving it empty) when its
        // entries were moved onto freshly created group tasks.
        Set<String> newObsTasks = new HashSet<>(getObservationFormTaskIds());
        newObsTasks.removeAll(obsTasksBefore);
        Assert.assertFalse("Entering scheduled observations should have created at least one observation task", newObsTasks.isEmpty());
        for (String taskId : newObsTasks)
            Assert.assertTrue("An empty observation task was created: " + taskId, countObservationsForTask(taskId) > 0);
    }

    private List<Map<String, Object>> getClinicalObservations(String animalId)
    {
        // study datasets and ehr.tasks are defined in the EHR study folder, not the project root, so query
        // that container explicitly rather than relying on the default project-scoped overload.
        return executeSelectRowCommand("study", "clinical_observations", ContainerFilter.Current, "/" + getContainerPath(), List.of(new Filter("Id", animalId))).getRows();
    }

    private Set<String> getObservationFormTaskIds()
    {
        Set<String> taskIds = new HashSet<>();
        for (Map<String, Object> row : executeSelectRowCommand("ehr", "tasks", ContainerFilter.Current, "/" + getContainerPath(), List.of(new Filter("formtype", NBRI_OBSERVATIONS_FORM_TYPE))).getRows())
        {
            if (row.get("taskid") != null)
                taskIds.add(String.valueOf(row.get("taskid")));
        }
        return taskIds;
    }

    private int countObservationsForTask(String taskId)
    {
        return executeSelectRowCommand("study", "clinical_observations", ContainerFilter.Current, "/" + getContainerPath(), List.of(new Filter("taskid", taskId))).getRowCount().intValue();
    }

    @Test
    public void testObservationBulkEdit()
    {
        log("Verifying the bulk edit Observation/Score editor follows the selected Category");
        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Bulk Clinical Entry"));

        Ext4GridRef observations = _helper.getExt4GridForFormSection("Observations");
        _helper.addRecordToGrid(observations);
        _helper.addRecordToGrid(observations);
        int rowCount = observations.getRowCount();

        observations.clickTbarButton("Select All");
        observations.waitForSelected(rowCount);

        Locator.XPathLocator bulkEditWindow = _helper.openBulkEdit(observations);

        log("Selecting a Category whose observation editor is a lookup combo");
        _helper.toggleBulkEditExactField("Category");
        Ext4ComboRef categoryField = _ext4Helper.queryOne("window field[name=category]", Ext4ComboRef.class);
        Assert.assertNotNull("Category field not found in Bulk Edit window", categoryField);
        categoryField.waitForStoreLoad();
        categoryField.setComboByDisplayValue("Appetite");
        waitForObservationBulkEditFieldXtype("ehr-simplecombo", "Appetite");

        log("Enabling the rebuilt Observation/Score field and selecting an Appetite-specific value");
        _helper.toggleBulkEditExactField("Observation/Score");
        Ext4ComboRef observationCombo = new Ext4ComboRef(getObservationBulkEditField(), this);
        observationCombo.waitForStoreLoad();
        observationCombo.setComboByDisplayValue("Normal to low");
        Assert.assertEquals("Observation/Score value was not set from the Appetite lookup", "Normal to low", observationCombo.getValue());

        log("Switching to a Category whose observation editor is free text");
        categoryField.setComboByDisplayValue("Mass");
        waitForObservationBulkEditFieldXtype("textfield", "Mass");

        Ext4FieldRef observationField = getObservationBulkEditField();
        Assert.assertFalse("Observation/Score field should remain enabled across Category changes", observationField.isDisabled());
        Object staleValue = observationField.getValue();
        Assert.assertTrue("Observation/Score value should be cleared when the Category changes, but was: " + staleValue,
                staleValue == null || "".equals(staleValue));

        String observationText = "3 cm mass on left arm";
        observationField.setValue(observationText);

        waitAndClick(bulkEditWindow.append(Ext4Helper.Locators.ext4Button("Submit")));
        Window<?> msgWindow = new Window.WindowFinder(getDriver()).withTitle("Set Values").waitFor();
        msgWindow.clickButton("Yes", 0);
        waitForElementToDisappear(bulkEditWindow);

        log("Verifying the bulk edit values were applied to every selected row");
        for (int row = 1; row <= rowCount; row++)
        {
            Assert.assertEquals("Category was not bulk-set on row " + row, "Mass", observations.getFieldValue(row, "category"));
            Assert.assertEquals("Observation was not bulk-set on row " + row, observationText, observations.getFieldValue(row, "observation"));
        }

        _helper.discardForm();
    }

    private Ext4FieldRef getObservationBulkEditField()
    {
        return _ext4Helper.queryOne("window field[name=observation]", Ext4FieldRef.class);
    }

    private void waitForObservationBulkEditFieldXtype(String xtype, String category)
    {
        waitFor(() -> {
            Ext4FieldRef field = getObservationBulkEditField();
            return field != null && xtype.equals(field.getEval("xtype"));
        }, "Observation/Score editor was not rebuilt as '" + xtype + "' for the '" + category + "' category", WAIT_FOR_JAVASCRIPT);
    }

    @Override
    @Test
    public void testQuickSearch()
    {
        //TODO: Implement this test once Quick Search is customized for NBRI
    }

    public void addUsersAndPermissions()
    {
        //create animal care basic submitter user (ex. this user can 'Submit Death' in Death/Necropsy)
        createUser(NBRI_BASIC_SUBMITTER, "EHR Basic Submitters", null);

        //create a vet tech user with 'EHR Basic Submitters' group (ex. this user can 'Submit Necropsy for Review' in Death/Necropsy)
        createUser(NBRI_BASIC_SUBMITTER_VET_TECH, "EHR Basic Submitters", NBRI_VET_TECH_ROLE);

        //create a vet tech user with 'EHR Full Updaters' group (ex. this user can 'Submit for Review' and 'Submit Final' in cases)
        createUser(NBRI_FULL_SUBMITTER_VET_TECH, "EHR Full Updaters", NBRI_VET_TECH_ROLE);

        //create a vet user with 'EHR Full Updaters' group (ex. this user can 'Submit Final' in Death/Necropsy)
        createUser(NBRI_FULL_SUBMITTER_VET, "EHR Full Updaters", "EHR Veterinarian");

        _permissionsHelper.setPermissions(FULL_UPDATER.getGroup(), "EHR Clinical Entry");

    }

    public void createSubjectsForDeathForm() throws IOException, CommandException
    {
        goToEHRFolder();
        goToSchemaBrowser();
        log("Creating animals");
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), getApiHelper().prepareInsertCommand("study", "birth", "lsid",
                new String[]{"Id", "Date", "gender", "QCStateLabel", "performedby"},
                new Object[][]{
                        {aliveAnimalId, LocalDateTime.now().minusDays(30), "f", "Completed", 1004},
                        {deadAnimalId, LocalDateTime.now().minusDays(30), "m", "Completed", 1004},
                        {departedAnimalId, LocalDateTime.now().minusDays(30), "m", "Completed", 1004},
                }
        ), getExtraContext());

        log("Inserting rows in assignments, protocolAssignment and housing");
        InsertRowsCommand protocol = new InsertRowsCommand("study", "protocolAssignment");
        protocol.addRow(Map.of("Id", aliveAnimalId, "date", LocalDateTime.now().minusDays(10), "protocol", "protocol101", "QCStateLabel", "Completed", "performedby", 1004));
        protocol.execute(getApiHelper().getConnection(), getContainerPath());

        InsertRowsCommand project = new InsertRowsCommand("study", "assignment");
        project.addRow(Map.of("Id", aliveAnimalId, "date", LocalDateTime.now().minusDays(10), "project", "640991", "QCStateLabel", "Completed", "performedby", 1004));
        project.execute(getApiHelper().getConnection(), getContainerPath());

        InsertRowsCommand housing = new InsertRowsCommand("study", "housing");
        housing.addRow(Map.of("Id", aliveAnimalId, "date", LocalDateTime.now().minusDays(10), "cage", "C4", "QCStateLabel", "Completed", "performedby", 1004));
        housing.execute(getApiHelper().getConnection(), getContainerPath());

        log("Marking an animal dead");
        InsertRowsCommand deaths = new InsertRowsCommand("study", "deaths");
        deaths.addRow(Map.of("Id", deadAnimalId, "date", LocalDateTime.now().minusDays(10), "reason", "4", "performedby", 1004));
        deaths.execute(getApiHelper().getConnection(), getContainerPath());

        log("Marking an animal departed");
        InsertRowsCommand departure = new InsertRowsCommand("study", "departure");
        departure.addRow(Map.of("Id", departedAnimalId, "date", LocalDateTime.now().minusDays(1), "destination", "Oregon NPRC", "performedby", 1004));
        departure.execute(getApiHelper().getConnection(), getContainerPath());
    }

    private void createUser(String userEmail, String groupName, @Nullable String roleClass)
    {
        _userHelper.createUser(userEmail, false);
        goToEHRFolder();
        if (roleClass != null)
            _permissionsHelper.setUserPermissions(userEmail, roleClass);
        _permissionsHelper.addUserToProjGroup(userEmail, getProjectName(), groupName);
    }

    @Test
    public void testDeathNecropsyForm() throws IOException, CommandException
    {
        enableNotification("status_org.labkey.nbri_ehr.notification.NBRIDeathNotification");
        createSubjectsForDeathForm();

        log("Go to EHR page > Enter Data > Death/Necropsy");
        impersonate(NBRI_BASIC_SUBMITTER);
        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Death/Necropsy"));

        waitForElement(Locator.name("Id"));
        setFormElement(Locator.name("Id"), departedAnimalId);
        waitForText("Id: ERROR: Animal is not at the center.");

        setFormElement(Locator.name("Id"), aliveAnimalId);
        _ext4Helper.selectComboBoxItem("Death Type:", "Spontaneous/Normal");
        _ext4Helper.selectComboBoxItem("Disposition:", "Euthaniasia (project)");
        waitForElement(Locator.name("deathWeight"));
        setFormElement(Locator.name("deathWeight"), "23");
        Assert.assertFalse(isElementPresent(Locator.linkWithText("Submit Necropsy for Review")));
        Assert.assertFalse(isElementPresent(Locator.linkWithText("Submit Final")));
        submitForm("Submit Death", "Confirm");
        stopImpersonating();

        log("Trigger notifications");
        goToEHRFolder();
        NotificationAdminPage adminPage = NotificationAdminPage.beginAt(this);
        adminPage.clickManuallyTriggerEmail("NBRI Death Notification");

        goToModule("Dumbster");
        EmailRecordTable notifications = new EmailRecordTable(this);
        waitForTextWithRefresh(WAIT_FOR_PAGE + WAIT_FOR_JAVASCRIPT, "Death Notification: " + aliveAnimalId); //wait for more than a min
        notifications.getMessage("Death Notification: " + aliveAnimalId).getBody().
                contains("Animal '" + aliveAnimalId + "' has been declared dead on '" + LocalDateTime.now().format(_dateFormat) + "'.");
        notifications.clickMessage(notifications.getMessageWithSubjectContaining("Death Notification: " + aliveAnimalId));
        String url = Locator.linkWithText("Click here to record Necropsy").findElement(notifications).getAttribute("href");

        log("Entering Necropsy");
        impersonate(NBRI_BASIC_SUBMITTER_VET_TECH);
        beginAt(url);
        _helper.getExt4GridForFormSection("Necropsy");
        waitForElement(Ext4Helper.Locators.ext4Button("Submit Necropsy for Review"), WAIT_FOR_PAGE);
        waitForElement(Ext4Helper.Locators.formItemWithLabel("Performed By:"), WAIT_FOR_PAGE);
        scrollIntoView(Locator.name("accessionNumber"));
        _ext4Helper.selectComboBoxItem("Physical Condition:", "Excellent");
        _ext4Helper.selectComboBoxItem("Condition of Specimen:", "Fresh");
        sleep(1000);
        Ext4FieldRef accessionNumber = _helper.getExt4FieldForFormSection("Necropsy", "Accession Number");
        accessionNumber.setValue("123");
        waitFor(() -> "123".equals(accessionNumber.getValue()), WAIT_FOR_JAVASCRIPT);
        scrollIntoView(Locator.name("diagnosis"));
        Ext4FieldRef identification = _helper.getExt4FieldForFormSection("Necropsy", "Name/State/License no. (quarantine only)");
        identification.setValue("Extra information");
        waitFor(() -> "Extra information".equals(identification.getValue()), WAIT_FOR_JAVASCRIPT);
        Ext4FieldRef grossAbnormalities = _helper.getExt4FieldForFormSection("Necropsy", "Gross Abnormalities");
        grossAbnormalities.setValue("Extra leg");
        waitFor(() -> "Extra leg".equals(grossAbnormalities.getValue()), WAIT_FOR_JAVASCRIPT);
        Ext4FieldRef diagnosis = _helper.getExt4FieldForFormSection("Necropsy", "Diagnosis");
        diagnosis.setValue("Dead");
        waitFor(() -> "Dead".equals(diagnosis.getValue()), WAIT_FOR_JAVASCRIPT);
        _ext4Helper.selectComboBoxItem("Performed By:", NBRI_BASIC_SUBMITTER_NAME);

        log("Entering Tissue Disposition");
        Ext4GridRef tissueDisposition = _helper.getExt4GridForFormSection("Tissue Disposition");
        _helper.addRecordToGrid(tissueDisposition);
        tissueDisposition.setGridCell(1, "necropsyDispositionCode", "Frozen");
        tissueDisposition.setGridCell(1, "necropsyTissue", "Pancreas");
        waitAndClick(_helper.getDataEntryButton("Submit Necropsy for Review"));

        log("Assigning the reviewer");
        Window<?> submitForReview = new Window<>("Submit For Review", getDriver());

        // Make sure to find the element in submitForReview window.
        waitForElement(Locator.tagWithNameContaining("input", "assignedTo"));
        click(Locator.tagWithNameContaining("input", "assignedTo"));
        WebElement assignedToElement = Locator.tagWithNameContaining("input", "assignedTo").findWhenNeeded(submitForReview);
        setFormElement(assignedToElement, _userHelper.getDisplayNameForEmail(NBRI_FULL_SUBMITTER_VET));
        click(Locator.tagWithNameContaining("input", "assignedTo"));

        // Entering the text leaves the selection list visible, send 'Enter' to remove it.
        assignedToElement.sendKeys(Keys.ENTER);

        // The 'button' is actually a link tag.
        WebElement submitButton = Locator.tagWithText("a", "Submit").findWhenNeeded(submitForReview);
        scrollIntoView(submitButton);
        doAndWaitForPageToLoad(submitButton::click);

        stopImpersonating();

        log("Verify rows were inserted in appropriate datasets");
        goToEHRFolder();
        verifyRowCreated("study", "necropsy", aliveAnimalId, 1);
        verifyRowCreated("study", "grossPathology", aliveAnimalId, 9);
        verifyRowCreated("study", "tissueDisposition", aliveAnimalId, 1);

        goToEHRFolder();
        impersonate(NBRI_FULL_SUBMITTER_VET);
        EnterDataPage enterDataPage = EnterDataPage.beginAt(this, getContainerPath());
        enterDataPage.clickAllTasksTab();
        waitAndClick(Locator.linkWithText("Death/Necropsy"));
        switchToWindow(1);
        submitForm("Submit Final", "Finalize");
        switchToMainWindow();
        stopImpersonating();

        log("Verify rows were inserted in appropriate datasets");
        goToEHRFolder();
        verifyRowCreated("study", "weight", aliveAnimalId, 1);

        log("Verify animal is marked as dead");
        AnimalHistoryPage<?> historyPage = AnimalHistoryPage.beginAt(this);
        historyPage.searchSingleAnimal(aliveAnimalId);
        waitForText(WAIT_FOR_PAGE, "Dead");
        waitForText("23 kg"); //checking latest weight is updated.

        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("study", "housing");
        table.setFilter("Id", "Equals", aliveAnimalId);
        Assert.assertTrue("End date is not updated for study.housing", table.getDataAsText(0, "endDate").contains(LocalDateTime.now().format(_dateFormat)));

        log("Verify end date in study.assignment");
        goToSchemaBrowser();
        table = viewQueryData("study", "assignment");
        table.setFilter("Id", "Equals", aliveAnimalId);
        Assert.assertTrue("End date is not updated for study.assignment", table.getDataAsText(0, "endDate").contains(LocalDateTime.now().format(_dateFormat)));

        log("Verify end date in study.protocolAssignment");
        goToSchemaBrowser();
        table = viewQueryData("study", "protocolAssignment");
        table.setFilter("Id", "Equals", aliveAnimalId);
        Assert.assertTrue("End date is not updated for study.protocolAssignment", table.getDataAsText(0, "endDate").contains(LocalDateTime.now().format(_dateFormat)));

    }

    @Test
    public void testClinicalCasesWorkflow()
    {
        String animalId = "8377984";

        //Go to NBRI/EHR main page
        goToEHRFolder();

        //Impersonate as NBRI_FULL_SUBMITTER_VET_TECH
        impersonate(NBRI_FULL_SUBMITTER_VET_TECH);

        //Navigate to Enter Data > Clinical Cases
        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Clinical Cases"));

        //Fill out Clinical Case section with Id, Date, Open Remark
        Ext4FieldRef problem = _helper.getExt4FieldForFormSection("Clinical Case", "Problem Area");
        problem.clickTrigger();
        problem.setValue("Circulatory abnormality");
        click(Locator.textarea("openRemark"));
        setFormElement(Locator.textarea("openRemark"), "Clinical Case WorkFlow - Test");
        click(Locator.textarea("plan"));
        setFormElement(Locator.textarea("plan"), "Case plan");
        waitForElement(Locator.name("Id"));
        click(Locator.name("Id"));
        setFormElement(Locator.name("Id"), animalId);
        _helper.getExt4FieldForFormSection("Clinical Case", "Open Date").setValue(LocalDateTime.now().minusDays(1).format(_dateFormat));
        Assert.assertEquals("Performed by is incorrect ", "vet tech fs", getFormElement(Locator.name("performedby")));

        //Fill out Clinical Remarks section with Date, Remark
        scrollIntoView(Locator.textarea("remark"));
        _helper.getExt4FieldForFormSection("Clinical Remarks", "Date").setValue(LocalDateTime.now().minusDays(2).format(_dateFormat));
        _helper.setDataEntryField("remark", "Clinical Remarks - Test");
        if (null == _helper.getExt4FieldForFormSection("Clinical Remarks", "Remark").getValue())
            _helper.setDataEntryField("remark", "Clinical Remarks - Test");
        waitForTextToDisappear("Remark: WARN: Must enter at least one comment");

        Ext4GridRef weight = _helper.getExt4GridForFormSection("Weights");
        _helper.addRecordToGrid(weight);
        weight.setGridCellJS(1, "date", LocalDateTime.now().minusDays(1).format(_dateFormat));
        weight.setGridCell(1, "weight", "6.000");

        log("Adding Medications/Treatments Orders");
        Ext4GridRef orderGrid = _helper.getExt4GridForFormSection("Medications/Treatments Orders");
        _helper.addRecordToGrid(orderGrid);
        orderGrid.setGridCell(1, "date", LocalDateTime.now().minusDays(2).format(_dateFormat));
        orderGrid.clickDownArrowOnGrid(1, "code");
        orderGrid.setGridCell(1, "code", "Diazepam");
        orderGrid.clickDownArrowOnGrid(1, "frequency");
        orderGrid.setGridCell(1, "frequency", "QID");
        orderGrid.clickDownArrowOnGrid(1, "route");
        orderGrid.setGridCell(1, "route", "IVAG");
        // Set Ordered By directly in the grid rather than through the bulk-edit combo. That combo's async
        // user-store typeahead trips an Ext4 "onTypeAhead ... findRecord of null" client error that can
        // intermittently leave Ordered By unset, which fails validation and blocks the form submit.
        orderGrid.setGridCell(1, "orderedby", NBRI_VET_NAME);
        orderGrid.completeEdit();

        // Entering the Weights/Medications sections above can rebuild the Clinical Case section and drop the
        // Problem Area combo value; re-assert it before submitting so a "Problem Area is required" error
        // doesn't block the submit.
        setFormSectionFieldUntilWarningClears("Clinical Case", "Problem Area", "Circulatory abnormality", "Problem Area is required");
        submitForm("Submit Final", "Finalize Form");

        log("Completing today's Medication Schedule");
        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Today's Medication/Treatment Schedule"));
        AnimalHistoryPage<?> animalHistoryPage = new AnimalHistoryPage<>(getDriver());
        DataRegionTable scheduleTable = animalHistoryPage.getActiveReportDataRegion();
        Assert.assertEquals("Incorrect number of rows", 4, scheduleTable.getDataRowCount());
        scheduleTable.link(0, "treatmentRecord").click();
        switchToWindow(1);

        waitForText("Diazepam");
        waitForText(animalId);
        waitForTextToDisappear("Id is required");
        _helper.getExt4GridForFormSection("Medications/Treatments Given");
        submitForm("Submit Final", "Finalize");
        stopImpersonating();

        //Go to NBRI/EHR main page
        goToEHRFolder();
        impersonate(NBRI_FULL_SUBMITTER_VET);

        //Go to 'Active Clinical Cases'
        clickAndWait(Locator.linkWithText("Active Clinical Cases"));

        //Click on 'Case Update' link. Filter to this animal first: other tests (e.g. task grouping) leave
        //their own cases active in this folder, so select by animal rather than assuming row 0 is our case.
        AnimalHistoryPage<?> historyPage = new AnimalHistoryPage<>(getDriver());
        DataRegionTable activeClinicalCases = historyPage.getActiveReportDataRegion();
        activeClinicalCases.setFilter("Id", "Equals", animalId);
        activeClinicalCases.link(0, "caseCheck").click();
        switchToWindow(2);

        //Fill out Close Date
        waitForText(animalId);
        waitForTextToDisappear("Id is required");
        setCaseSubjective("Closing the case");

        waitForElement(Ext4Helper.Locators.ext4Button("Edit"));
        Ext4Helper.Locators.ext4Button("Edit").findElement(getDriver()).click();
        Ext4FieldRef enddateField = _helper.getExt4FieldForFormSection("Clinical Case", "Close Date");
        if (!enddateField.isVisible())
            Ext4Helper.Locators.ext4Button("Edit").findElement(getDriver()).click(); //click again
        enddateField.setValue(LocalDateTime.now().minusDays(1).format(_dateFormat));

        // Verify close remark required
        waitForText("Close remark required when closing a case.");
        _helper.setDataEntryField("closeRemark", "Case closed.");

        //'Submit Final'
        submitForm("Submit Final", "Finalize Form");

        //Go to NBRI/EHR main page
        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Active Clinical Cases"));

        //Verify that this animal's case is no longer present/is closed (other tests may leave cases active)
        historyPage = new AnimalHistoryPage<>(getDriver());
        activeClinicalCases = historyPage.getActiveReportDataRegion();
        activeClinicalCases.setFilter("Id", "Equals", animalId);
        Assert.assertEquals("Clinical case for " + animalId + " should be closed", 0, activeClinicalCases.getDataRowCount());
        stopImpersonating();
    }

    @Override
    @Test
    public void testCalculatedAgeColumns()
    {
        String subjectId = "TEST6390238";

        beginAt(String.format("%s/query-executeQuery.view?schemaName=study&query.queryName=Weight&query.Id~contains=%s", getContainerPath(), subjectId));
        _customizeViewsHelper.openCustomizeViewPanel();
        _customizeViewsHelper.addColumn("ageAtTime/AgeAtTime");
        _customizeViewsHelper.addColumn("ageAtTime/AgeAtTimeYearsRounded");
        _customizeViewsHelper.addColumn("ageAtTime/AgeAtTimeMonths");
        _customizeViewsHelper.applyCustomView();

        DataRegionTable table = new DataRegionTable("query", this);
        int columnCount = table.getColumnCount();
        List<String> row = table.getRowDataAsText(0);
        assertEquals("Calculated ages are incorrect", Arrays.asList("4.8", "4.0", "58.0"), row.subList(columnCount - 3, columnCount));
    }

    @Test
    public void testLookupPage() throws Exception
    {
        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Manage Lookup Tables"));

        EHRLookupPage ehrLookupPage = new EHRLookupPage(this);
        QueryGrid grid = ehrLookupPage.getQueryGrid();
        checker().verifyEquals("Missing look up tables", countLines(FileUtil.appendPath(TestFileUtils.getLabKeyRoot(), org.labkey.api.util.Path.parse(getModulePath() + "/resources/data/editable_lookups.tsv"))) - 1, grid.getRecordCount());

        clickAndWait(Locator.linkWithText("Age Class"));
        checker().verifyEquals("Navigated to incorrect schema", "ehr_lookups", getUrlParam("schemaName"));
        checker().verifyEquals("Navigated to incorrect query", "ageclass", getUrlParam("query.queryName"));
    }

    @Test
    public void testBehavioralCases()
    {
        String animalId1 = "56789";
        String animalId2 = "44444";
        String drug1 = "Aluminum Hydroxide";

        log("Adding behavioral case for " + animalId1);
        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Behavioral Cases"));
        waitForText("The field: Id is required");
        Ext4FieldRef problem = _helper.getExt4FieldForFormSection("Behavior Case", "Problem Area");
        problem.clickTrigger();
        problem.setValue("Behavioral");
        _helper.setDataEntryField("problemCategory", "Behavioral");
        _helper.setDataEntryField("remark", "Behavioral case remarks");
        _helper.getExt4FieldForFormSection("Behavior Case", "Open Date").setValue(LocalDateTime.now().minusDays(1).format(_dateFormat));
        setFormElement(Locator.name("Id"), animalId1);

        log("Adding Observations for " + animalId1);
        Ext4GridRef observationOrder = _helper.getExt4GridForFormSection("Observation Orders");
        _helper.addRecordToGrid(observationOrder);
        observationOrder.setGridCellJS(1, "date", LocalDateTime.now().minusDays(1).format(_dateFormat));
        observationOrder.setGridCell(1, "category", "Abnormal Behaviors");
        observationOrder.setGridCell(1, "frequency", "Alternating Days");

        log("Adding Medications/Treatments Orders for " + animalId1);
        Ext4GridRef treatmentOrder = _helper.getExt4GridForFormSection("Medications/Treatments Orders");
        _helper.addRecordToGrid(treatmentOrder);
        treatmentOrder.setGridCellJS(1, "date", LocalDateTime.now().minusDays(1).format(_dateFormat));

        treatmentOrder.clickDownArrowOnGrid(1, "code");
        Locator drugLoc = Locator.tag("ul").append(Locator.tagContainingText("li", drug1));
        shortWait().until(ExpectedConditions.visibilityOfElementLocated(drugLoc));
        drugLoc.findElement(getDriver()).click();
        treatmentOrder.completeEdit();

        treatmentOrder.setGridCell(1, "frequency", "QID");
        treatmentOrder.setGridCell(1, "route", "IV");
        treatmentOrder.setGridCell(1, "orderedby", NBRI_VET_NAME);
        // Re-assert the Behavior Assessment remark right before submitting: entering the orders above can
        // rebuild the section and drop the value typed earlier, which would leave the validation banner up.
        setDataEntryFieldUntilWarningClears("remark", "Behavioral case remarks", "Remark: WARN: Must enter at least one comment");
        submitForm("Submit Final", "Finalize");

        log("Adding behavioral case 31 days old for " + animalId2);
        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Behavioral Cases"));
        waitForText("The field: Id is required");

        // Set case problem
        problem = _helper.getExt4FieldForFormSection("Behavior Case", "Problem Area");
        problem.clickTrigger();
        problem.setValue("Behavioral");

        _helper.setDataEntryField("remark", "Behavioral case remarks ");
        _helper.getExt4FieldForFormSection("Behavior Case", "Open Date").setValue(LocalDateTime.now().minusDays(31).format(_dateFormat));
        setFormElement(Locator.name("Id"), animalId2);

        log("Adding Observations for " + animalId2);
        observationOrder = _helper.getExt4GridForFormSection("Observation Orders");
        _helper.addRecordToGrid(observationOrder);
        observationOrder.setGridCellJS(1, "date", LocalDateTime.now().minusDays(31).format(_dateFormat));
        observationOrder.setGridCell(1, "category", "Abnormal Behaviors");
        observationOrder.setGridCell(1, "frequency", "SID");
        // Re-assert the Behavior Assessment remark right before submitting (see note on the first case above).
        setDataEntryFieldUntilWarningClears("remark", "Behavioral case remarks ", "Remark: WARN: Must enter at least one comment");
        submitForm("Submit Final", "Finalize");

        log("Verify reports and schedule");
        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Active Behavior Cases"));
        AnimalHistoryPage<?> animalHistoryPage = new AnimalHistoryPage<>(getDriver());
        DataRegionTable activeCase = animalHistoryPage.getActiveReportDataRegion();
        Assert.assertEquals("Behavioral case did not get created", 2, activeCase.getDataRowCount());

        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Active Behavior Medication Orders"));
        animalHistoryPage = new AnimalHistoryPage<>(getDriver());
        DataRegionTable medicationOrderTable = animalHistoryPage.getActiveReportDataRegion();
        Assert.assertEquals("Medication order was not created for the behavioral case", 1, medicationOrderTable.getDataRowCount());
        Assert.assertEquals("Incorrect medication order", Arrays.asList(animalId1, drug1, "QID", "IV", NBRI_VET_NAME),
                medicationOrderTable.getRowDataAsText(0, "Id", "code", "frequency", "route", "orderedby"));

        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Active Behavior Observation Orders"));
        new AnimalHistoryPage<>(getDriver());
        DataRegionTable observationOrderTable = animalHistoryPage.getActiveReportDataRegion();
        Assert.assertEquals("Observation order was not created for the behavioral case", 2, observationOrderTable.getDataRowCount());
        observationOrderTable.setFilter("Id", "Equals", animalId1);
        Assert.assertEquals("Incorrect observation order", Arrays.asList(animalId1, "Abnormal Behaviors", "Alternating Days"),
                observationOrderTable.getRowDataAsText(0, "Id", "category", "frequency"));

        log("Navigating to Today's Medication/Treatment Schedule");
        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Today's Medication/Treatment Schedule"));
        animalHistoryPage = new AnimalHistoryPage<>(getDriver());
        DataRegionTable medicationSchedule = animalHistoryPage.getActiveReportDataRegion();
        medicationSchedule.setFilter("code", "Equals", drug1);

        medicationSchedule.link(0, "treatmentRecord").click();
        switchToWindow(1);
        submitForm("Submit Final", "Finalize");

        log("Navigating to Incomplete Past Observations.");
        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Incomplete Past Observations"));
        animalHistoryPage = new AnimalHistoryPage<>(getDriver());
        Assert.assertEquals("Incorrect rows in Incomplete Past Observations.", 31, animalHistoryPage.getActiveReportDataRegion().getDataRowCount());

        log("Verifying Close case");
        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Active Behavior Cases"));
        animalHistoryPage = new AnimalHistoryPage<>(getDriver());
        activeCase = animalHistoryPage.getActiveReportDataRegion();
        activeCase.setFilter("Id", "Equals", animalId1);
        activeCase.link(0, "caseCheck").click();
        switchToWindow(2);

        waitForText(animalId1);
        waitForTextToDisappear("Id is required");
        _helper.setDataEntryField("remark", "Closing the case");
        waitForTextToDisappear("Subjective: WARN: Must enter at least one comment");
        waitAndClick(Ext4Helper.Locators.ext4Button("Edit"));

        // Verify close remark required
        _helper.getExt4FieldForFormSection("Behavior Case", "Close Date").setValue(LocalDateTime.now().format(_dateFormat));
        waitForText("Close remark required when closing a case.");
        _helper.setDataEntryField("closeRemark", "Case closed.");

        submitForm("Submit Final", "Finalize");

        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Active Behavior Cases"));
        animalHistoryPage = new AnimalHistoryPage<>(getDriver());
        activeCase = animalHistoryPage.getActiveReportDataRegion();
        Assert.assertEquals("Case was not closed", 1, activeCase.getDataRowCount());
    }

    private int countLines(File file) throws Exception
    {
        try (BufferedReader reader = Readers.getReader(file))
        {
            int count = 0;
            while (reader.readLine() != null)
            {
                count++;
            }
            return count;
        }
    }

    private void verifyRowCreated(String schema, String query, String animalId, int rowCount)
    {
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData(schema, query);
        table.setFilter("Id", "Equals", animalId);
        Assert.assertEquals("Record not created in " + schema + "." + query, rowCount, table.getDataRowCount());
    }

    // Sets a data-entry field and waits for the given form warning to clear, re-entering the value if it
    // didn't take. EHR form sections can still be (re)building shortly after a form/window opens, and a value
    // typed too early is silently discarded; re-entering until the warning disappears makes the entry robust.
    private void setDataEntryFieldUntilWarningClears(String fieldName, String value, String warning)
    {
        _helper.setDataEntryField(fieldName, value);
        waitFor(() -> {
            if (!isTextPresent(warning))
                return true;
            _helper.setDataEntryField(fieldName, value);
            return false;
        }, "\"" + warning + "\" did not clear after setting " + fieldName, WAIT_FOR_JAVASCRIPT);
    }

    // Enters the Subjective ("s") comment on a case-close form and waits for its "must enter at least one
    // comment" warning to clear (see setDataEntryFieldUntilWarningClears for why re-entry is needed).
    private void setCaseSubjective(String value)
    {
        setDataEntryFieldUntilWarningClears("s", value, "Subjective: WARN: Must enter at least one comment");
    }

    // Like setDataEntryFieldUntilWarningClears, but for an Ext4 form-section field (e.g. a combo) addressed by
    // its section title and field label. Combos are especially prone to losing their value when a section is
    // rebuilt, so re-select until the warning/error clears.
    private void setFormSectionFieldUntilWarningClears(String section, String label, String value, String warning)
    {
        waitFor(() -> {
            if (!isTextPresent(warning))
                return true;
            Ext4FieldRef field = _helper.getExt4FieldForFormSection(section, label);
            if (field != null)
                field.setValue(value);
            return false;
        }, "\"" + warning + "\" did not clear after re-setting " + label, WAIT_FOR_JAVASCRIPT);
    }

    private void submitForm(String buttonText, String windowTitle)
    {
        //Give time for errors to disappear after validation
        Locator.tagContainingText("div", "The form has the following errors and warnings:")
                .waitForElementToDisappear(longWait());
        Locator submitFinalBtn = Locator.linkWithText(buttonText);
        shortWait().until(ExpectedConditions.elementToBeClickable(submitFinalBtn));
        Window<?> msgWindow;
        try
        {
            submitFinalBtn.findElement(getDriver()).click();
            msgWindow = new Window.WindowFinder(this.getDriver()).withTitleContaining(windowTitle).waitFor();
        }
        catch (NoSuchElementException e)
        {
            //retry
            sleep(500);
            submitFinalBtn.findElement(getDriver()).click();
            msgWindow = new Window.WindowFinder(this.getDriver()).withTitleContaining(windowTitle).waitFor();
        }
        msgWindow.clickButton("Yes");
    }

    private void gotoEnterData()
    {
        beginAt(WebTestHelper.buildURL("ehr", getContainerPath(), "enterData.view"));
    }

    private void lockForm()
    {
        Locator.XPathLocator lockBtn = Ext4Helper.Locators.ext4Button("Lock Entry");
        Locator.XPathLocator unlockBtn = Ext4Helper.Locators.ext4Button("Unlock Entry");
        try
        {
            log("Locking the entry");
            waitForElement(lockBtn);
            lockBtn.findElement(getDriver()).click();
            waitForElementToDisappear(lockBtn);
            Assert.assertTrue("Entry did not lock", isElementPresent(unlockBtn));
        }
        catch (NoSuchElementException e)
        {
            log("Form is already unlocked");
        }
    }
}

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
import org.json.JSONObject;
import org.junit.experimental.categories.Category;
import org.labkey.api.reader.Readers;
import org.labkey.api.util.FileUtil;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.remoteapi.core.SaveModulePropertiesCommand;
import org.labkey.remoteapi.query.ContainerFilter;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.ImportDataCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.RowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.remoteapi.security.CreateUserResponse;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.EHR;
import org.labkey.test.components.CustomizeView;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.components.ext4.ComboBox;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.pages.ehr.AnimalHistoryPage;
import org.labkey.test.pages.ehr.EHRAdminPage;
import org.labkey.test.pages.ehr.EHRLookupPage;
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
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.labkey.test.util.ext4cmp.Ext4ComboRef;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.labkey.test.util.ext4cmp.Ext4GridRef;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import static org.junit.Assert.assertNotNull;
import static org.labkey.test.components.ext4.ComboBox.ComboBox;
import static org.labkey.test.components.ext4.RadioButton.RadioButton;
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
    private static final String NBRI_PATHOLOGIST = "pathologist@nbritest.com";
    private static final String DEATH_NOTIFICATION = "org.labkey.nbri_ehr.notification.NBRIDeathNotification";
    private static final String NECROPSY_REQUEST_NOTIFICATION = "org.labkey.nbri_ehr.notification.NBRINecropsyRequestNotification";
    private static final String ACCESSION_NUMBER = "N26-001";
    // Assign the vet-tech role by its class-based name rather than its "EHR Veterinarian Technician" display
    // name: NIRC's role shares that display name, so a display-name lookup can resolve to the wrong module's
    // role, leaving the user without NBRIEHRVetTechPermission.
    private static final String NBRI_VET_TECH_ROLE = "NBRIEHRVetTechRole";

    private static final String deadAnimalId = "D5454";
    private static final String departedAnimalId = "H6767";
    private static final String aliveAnimalId = "A4545";
    // never inserted into demographics; exercises the deaths trigger's unknown-Id rejection
    private static final String unknownAnimalId = "X9999";
    // Given completed housing and assignments followed by newer drafts, so the demographicsLast* queries must skip the drafts.
    private static final String FINAL_RECORDS_ANIMAL = "FIN0001";
    // Dedicated animal for testScheduledObservationTaskGrouping; provisioned (alive, housed, assigned) in
    // createTestSubjects so the clinical case form raises no warnings that would keep the validation banner up.
    private static final String taskGroupAnimalId = "TESTGRP9090";
    // Dedicated animal for testObservationTypeDerivedFromCategory; provisioned the same way so the Observations
    // form can be submitted final in one step.
    private static final String obsTypeAnimalId = "TESTOBSTYPE9191";

    // Rooms are keyed by building and name, so every room fixture needs a building to hang off of.
    // 'buildings' derives its key from the description, and 'SPF' is one of the areas seeded with the ehr_lookups schema.
    private static final String BUILDING_ID = "TestBuilding";
    private static final String BUILDING_AREA = "SPF";

    // Cage locations seeded by populateLocations, named for the room each one sits in. The cage trigger derives these
    // from the room and cage, and both housing records and the data entry cage pickers key off the location, so these
    // are what belongs in a 'cage' field and what the pickers list. datasetHousing.tsv spells the same values out,
    // since a TSV cannot call cageLocation.
    private static final String CAGE_IN_R1 = cageLocation("R1", "C1");
    private static final String CAGE_IN_R2 = cageLocation("R2", "C3");
    private static final String CAGE_IN_R3 = cageLocation("R3", "C4");

    // A group pen has no cage, so its location is the room key alone. Created by testGroupPenCagemates.
    private static final String PEN_ROOM_NAME = "PEN1";
    private static final String[] PEN_ANIMALS = {"PEN0001", "PEN0002"};

    // Housed against a cage-level location id, as opposed to a pen's room-level one. Every cage seeded by
    // populateLocations already has occupants from datasetHousing.tsv, so testCagematesWithoutRoom creates its own to
    // keep the expected cagemate count exact.
    private static final String ROOMLESS_CAGE_NAME = "C9";
    private static final String ROOMLESS_CAGE = cageLocation("R1", ROOMLESS_CAGE_NAME);
    private static final String[] ROOMLESS_ANIMALS = {"CAGE0001", "CAGE0002"};

    // Housed by testSnapshotShowsFullLocation against its own animal, so a sibling test relocating a shared one
    // cannot change what the snapshot reports.
    private static final String[] LOCATION_ANIMALS = {"LOC0001"};

    // Housed by testHousingTransferLocationColumns in different cages, so switching the row's Id must change the
    // current location it shows.
    private static final String[] TRANSFER_ANIMALS = {"HSG0001", "HSG0002"};

    // Rooms for testRoomDateDisabledCascade, dedicated to it so a sibling housing an animal or retiring a shared room
    // cannot change what it observes.
    private static final String RETIRED_ROOM_NAME = "DIS1";
    private static final String OCCUPIED_ROOM_NAME = "DIS2";
    private static final String DRAFT_ROOM_NAME = "DIS3";
    private static final String[] OCCUPIED_ROOM_ANIMALS = {"DIS0001"};
    private static final String[] DRAFT_ROOM_ANIMALS = {"DIS0002"};
    private static final String FOLLOWING_CAGE_NAME = "C1";
    private static final String INDEPENDENT_CAGE_NAME = "C2";
    // Predates every date this test retires a room on, so a cascade that wrongly overwrote it would be visible.
    private static final String INDEPENDENT_CAGE_DATE = "2020-01-15";

    // protocol.investigatorId looks up ehr.investigators rather than the user table, so a protocol shows an
    // investigator only when a row there carries its id.
    private static final String INVES_LAST_NAME = "Marsh";
    private static final String DUMMY_INVES_LAST_NAME = "Okafor";
    private static final String PROTOCOL_DESCRIPTION = "Chronic implant study";
    private static final String DUMMY_PROTOCOL_DESCRIPTION = "Placeholder protocol for the arrival and assignment fixtures";

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

        InsertRowsCommand invesCmd = new InsertRowsCommand("ehr", "investigators");
        invesCmd.addRow(Map.of("lastName", INVES_LAST_NAME, "firstName", "Robin", "userid", inves1.getUserId()));
        invesCmd.addRow(Map.of("lastName", DUMMY_INVES_LAST_NAME, "firstName", "Alex", "userid", inves2.getUserId()));
        RowsResponse invesResponse = invesCmd.execute(createDefaultConnection(), getContainerPath());

        Map<String, Object> investigatorIds = new HashMap<>();
        for (Map<String, Object> row : invesResponse.getRows())
        {
            Object rowId = row.get("rowid");
            // A null id here reaches the protocols below as a null investigator, which only surfaces much later
            // as a protocol dropdown missing the investigator half of its text.
            assertNotNull("Investigator insert did not return a row id for " + row.get("lastName"), rowId);
            investigatorIds.put((String) row.get("lastName"), rowId);
        }

        assertEquals("Investigator insert did not return a row per investigator", 2, investigatorIds.size());

        InsertRowsCommand insertCmd = new InsertRowsCommand("ehr", "protocol");

        Map<String, Object> rowMap = new HashMap<>();
        rowMap.put("protocol", PROTOCOL_ID);
        rowMap.put("InvestigatorId", investigatorIds.get(INVES_LAST_NAME));
        rowMap.put("title", PROTOCOL_ID);
        rowMap.put("description", PROTOCOL_DESCRIPTION);
        insertCmd.addRow(rowMap);
        rowMap = new HashMap<>();
        rowMap.put("protocol", DUMMY_PROTOCOL);
        rowMap.put("InvestigatorId", investigatorIds.get(DUMMY_INVES_LAST_NAME));
        rowMap.put("title", DUMMY_PROTOCOL);
        rowMap.put("description", DUMMY_PROTOCOL_DESCRIPTION);
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

        // The study import's trigger scripts query template columns such as protocol_counts.amendmentId
        addExtensibleCols();
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

    /**
     * The key a room trigger derives for the given room name. Mirrors ehr_lookups/rooms.js.
     */
    private static String roomKey(String roomName)
    {
        return BUILDING_ID + "-" + roomName;
    }

    /**
     * The location a cage trigger derives for the given room and cage. Mirrors ehr_lookups/cage.js.
     */
    private static String cageLocation(String roomName, String cageName)
    {
        return roomKey(roomName) + "-" + cageName;
    }

    /**
     * Housing records store the derived room key, so the fixture rooms have to be named by key for the room
     * lookups to resolve. The base implementation returns names that match no room in this study.
     */
    @Override
    protected String[] getRooms()
    {
        return new String[]{roomKey("R1"), roomKey("R2"), roomKey("R3")};
    }

    @LogMethod
    private void populateBuildingRecords() throws Exception
    {
        InsertRowsCommand insertCmd = new InsertRowsCommand("ehr_lookups", "buildings");
        Map<String, Object> rowMap = new HashMap<>();
        // Supply the derived key rather than relying on the trigger to fill it, matching how the base class seeds rooms.
        rowMap.put("name", BUILDING_ID);
        rowMap.put("description", BUILDING_ID);
        rowMap.put("area", BUILDING_AREA);
        insertCmd.addRow(rowMap);

        insertCmd.execute(createDefaultConnection(), getContainerPath());
    }

    @Override
    protected void populateRoomRecords() throws Exception
    {
        populateBuildingRecords();

        InsertRowsCommand insertCmd = new InsertRowsCommand("ehr_lookups", "rooms");
        Map<String, Object> rowMap = new HashMap<>();
        rowMap.put("name", ROOM_ID);
        rowMap.put("building", BUILDING_ID);
        rowMap.put("housingType", 1);
        rowMap.put("housingCondition", 1);
        insertCmd.addRow(rowMap);

        rowMap = new HashMap<>();
        rowMap.put("name", ROOM_ID2);
        rowMap.put("building", BUILDING_ID);
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
        enableSiteNotification();
        populateLocations();
        addUsersAndPermissions();//create users and assign roles, created for the death and necropsy forms, but users can be repurposed for other forms.
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

    private void enableNotification(String notificationClass)
    {
        goToEHRFolder();
        _containerHelper.enableModule("Dumbster");
        log("Setup the notification service for this container");
        EHRAdminPage.beginAt(this, getContainerPath());
        NotificationAdminPage notificationAdminPage = EHRAdminPage.clickNotificationService(this);
        notificationAdminPage.setNotificationUserAndReplyEmail(DATA_ADMIN_USER);
        notificationAdminPage.addManageUsers(notificationClass, "EHR Administrators");
        notificationAdminPage.enableRequestAdminAlerts("status_" + notificationClass);
    }

    @LogMethod
    private void populateLocations() throws IOException, CommandException
    {
        goToEHRFolder();
        // BUILDING_ID is created by populateRoomRecords, which runs earlier as part of initProject.
        log("Inserting values in rooms");
        InsertRowsCommand roomCmd = new InsertRowsCommand("ehr_lookups", "rooms");
        roomCmd.addRow(Map.of("name", "R1", "building", BUILDING_ID));
        roomCmd.addRow(Map.of("name", "R2", "building", BUILDING_ID));
        roomCmd.addRow(Map.of("name", "R3", "building", BUILDING_ID));
        roomCmd.execute(getApiHelper().getConnection(), getContainerPath());

        // 'location' is left out so the cage trigger derives it, exercising the same path production entry takes.
        log("Inserting values in cage");
        InsertRowsCommand cageCmd = new InsertRowsCommand("ehr_lookups", "cage");
        cageCmd.addRow(Map.of("cage", "C1", "room", roomKey("R1")));
        cageCmd.addRow(Map.of("cage", "C2", "room", roomKey("R1")));
        cageCmd.addRow(Map.of("cage", "C3", "room", roomKey("R2")));
        cageCmd.addRow(Map.of("cage", "C4", "room", roomKey("R3")));
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
        return "M";
    }

    @Override
    protected String getFemale()
    {
        return "F";
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
        // demographics.species holds an ehr_lookups.species_codes code, and ehr_lookups.weight_ranges is keyed
        // on that same code, so weight validation only fires for animals given a real code here.
        fields = new String[]{"Id", "Species", "Birth", "Gender", "date", "calculated_status", "objectid", "performedby"};
        data = new Object[][]{
                {SUBJECTS[0], "MMU", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {SUBJECTS[1], "MNE", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {SUBJECTS[2], "CAE", (new Date()).toString(), getFemale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {SUBJECTS[3], "MNE", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {SUBJECTS[4], "MNE", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "demographics", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "demographics", new Filter("Id", StringUtils.join(SUBJECTS, ";"), Filter.Operator.IN));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        //for simplicity, also create the animals from MORE_ANIMAL_IDS right now
        data = new Object[][]{
                {MORE_ANIMAL_IDS[0], "MMU", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {MORE_ANIMAL_IDS[1], "MNE", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {MORE_ANIMAL_IDS[2], "CAE", (new Date()).toString(), getFemale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {MORE_ANIMAL_IDS[3], "MNE", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004},
                {MORE_ANIMAL_IDS[4], "MNE", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004}
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
                {SUBJECTS[0], pastDate1, pastDate2, getRooms()[0], CAGE_IN_R1, 1004},
                {SUBJECTS[0], pastDate2, null, getRooms()[0], CAGE_IN_R1, 1004},
                {SUBJECTS[1], pastDate1, pastDate2, getRooms()[0], CAGE_IN_R1, 1004},
                {SUBJECTS[1], pastDate2, null, getRooms()[2], CAGE_IN_R3, 1004}
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
                {taskGroupAnimalId, "MMU", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "demographics", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "demographics", new Filter("Id", taskGroupAnimalId));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        fields = new String[]{"Id", "date", "enddate", "room", "cage", "performedby"};
        data = new Object[][]{
                {taskGroupAnimalId, pastDate1, null, getRooms()[0], CAGE_IN_R1, 1004}
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

        // Fully provision the observation-type test animal for the same reason.
        log("Creating observation type test subject");
        fields = new String[]{"Id", "Species", "Birth", "Gender", "date", "calculated_status", "objectid", "performedby"};
        data = new Object[][]{
                {obsTypeAnimalId, "MMU", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "demographics", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "demographics", new Filter("Id", obsTypeAnimalId));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        fields = new String[]{"Id", "date", "enddate", "room", "cage", "performedby"};
        data = new Object[][]{
                {obsTypeAnimalId, pastDate1, null, getRooms()[0], CAGE_IN_R1, 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "Housing", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "Housing", new Filter("Id", obsTypeAnimalId));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        fields = new String[]{"Id", "date", "enddate", "project", "performedby"};
        data = new Object[][]{
                {obsTypeAnimalId, pastDate1, null, PROJECTS[0], 1004}
        };
        insertCommand = getApiHelper().prepareInsertCommand("study", "Assignment", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "Assignment", new Filter("Id", obsTypeAnimalId));
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
                "WARN: Weight above the allowable value of 30.0 kg for MNE",
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
    public void testArrivalForm() throws IOException, CommandException
    {
        String arrivedAnimal = "30905";
        // demographics.socialCode holds an ehr_lookups.social_code code; the grids display its title
        String socialCode = "Acquired";
        // the group is an ehr_lookups.breeding_type code, carried to animal_group_members; the grids display its title
        String animalGroup = "Assigned Breeding Protocol";
        LocalDateTime now = LocalDateTime.now();

        // the protocol dropdown shows only protocols with an investigator, and the arriving animal is a Pig-Tailed Macaque
        String fullProtocol = "ARRFORM-FULL";
        createLimitedProtocol(fullProtocol, now.toLocalDate().minusYears(1), now.toLocalDate().plusYears(1), "MNE", 0);
        UpdateRowsCommand setInvestigator = new UpdateRowsCommand("ehr", "protocol");
        setInvestigator.addRow(Map.of("protocol", fullProtocol, "InvestigatorId", getInvestigatorRowId(DUMMY_INVES_LAST_NAME)));
        setInvestigator.execute(getApiHelper().getConnection(), getContainerPath());

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Arrivals"));
        lockForm();

        Ext4GridRef arrivals = _helper.getExt4GridForFormSection("Arrivals");
        _helper.addRecordToGrid(arrivals);
        arrivals.setGridCellJS(1, "date", now.minusDays(1).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_STRING)));
        arrivals.setGridCell(1, "arrivalType", "Non-quarantine Arrival");
        arrivals.setGridCell(1, "acquisitionType", "Lab Transfer (Wild Born)");
        arrivals.setGridCell(1, "Id", arrivedAnimal);
        arrivals.setGridCell(1, "cage", CAGE_IN_R1);
        arrivals.setGridCell(1, "Id/demographics/gender", "Female");
        arrivals.setGridCell(1, "Id/demographics/geographic_origin", "BRAZIL");
        arrivals.setGridCell(1, "Id/demographics/species", "Pig-Tailed Macaque");
        arrivals.setGridCellJS(1, "Id/demographics/birth", now.minusDays(7).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_STRING)));
        arrivals.setGridCell(1, "sourceFacility", "Bioqual, Incorporated");
        arrivals.setGridCell(1, "Id/demographics/socialCode", socialCode);

        log("Verifying Social Code is required");
        arrivals.setGridCellJS(1, "Id/demographics/socialCode", null);
        waitForFormError("The field: Social Code is required");
        arrivals.setGridCell(1, "Id/demographics/socialCode", socialCode);

        log("Verifying Generation is seeded to 0 and is required");
        assertEquals("An arriving animal should start at generation 0", "0", String.valueOf(arrivals.getFieldValue(1, "Id/demographics/generation")));
        arrivals.setGridCellJS(1, "Id/demographics/generation", null);
        waitForFormError("The field: Generation is required");
        arrivals.setGridCellJS(1, "Id/demographics/generation", 0);

        // the animal's opening project, protocol and group are entered on the arrival row itself; the trigger script
        // opens the matching assignment record for each one
        arrivals.setGridCell(1, "project", "640991");
        arrivals.setGridCell(1, "groupId", animalGroup);

        log("Verifying a protocol with no room left for the species is flagged on the form");
        arrivals.setGridCell(1, "arrivalProtocol", protocolChoice(fullProtocol, DUMMY_INVES_LAST_NAME));
        waitForFormError(overLimit(fullProtocol, "MNE", 1, 0, 0));
        arrivals.setGridCell(1, "arrivalProtocol", protocolChoice(DUMMY_PROTOCOL, DUMMY_INVES_LAST_NAME));

        log("Verifying the opening project, protocol and group are required");
        arrivals.setGridCellJS(1, "project", null);
        waitForFormError("The field: Project is required");
        arrivals.setGridCell(1, "project", "640991");

        arrivals.setGridCellJS(1, "arrivalProtocol", null);
        waitForFormError("The field: Protocol is required");
        arrivals.setGridCell(1, "arrivalProtocol", protocolChoice(DUMMY_PROTOCOL, DUMMY_INVES_LAST_NAME));

        arrivals.setGridCellJS(1, "groupId", null);
        waitForFormError("The field: Group is required");
        arrivals.setGridCell(1, "groupId", animalGroup);

        submitForm("Submit Final", "Finalize");

        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("study", "arrival");
        table.setFilter("Id", "Equals", arrivedAnimal);
        CustomizeView view = table.openCustomizeGrid();
        view.addColumn("cage");
        view.applyCustomView();
        Assert.assertEquals("Invalid Arrival record", Arrays.asList(arrivedAnimal), table.getRowDataAsText(0, "Id"));
        Assert.assertEquals("Invalid Arrival record", Arrays.asList(CAGE_IN_R1), table.getRowDataAsText(0, "cage"));

        goToSchemaBrowser();
        table = viewQueryData("study", "assignment");
        table.setFilter("Id", "Equals", arrivedAnimal);
        Assert.assertEquals("Invalid project assignment", Arrays.asList("640991"), table.getRowDataAsText(0, "project"));

        goToSchemaBrowser();
        table = viewQueryData("study", "protocolAssignment");
        table.setFilter("Id", "Equals", arrivedAnimal);
        Assert.assertEquals("Invalid protocol assignment", Arrays.asList("dummyprotocol"), table.getRowDataAsText(0, "protocol"));

        goToSchemaBrowser();
        table = viewQueryData("study", "animal_group_members");
        table.setFilter("Id", "Equals", arrivedAnimal);
        Assert.assertEquals("Invalid group assignment", Arrays.asList(animalGroup), table.getRowDataAsText(0, "groupId"));

        verifyRowCreated("study", "birth", arrivedAnimal, 1);
        verifyRowCreated("study", "assignment", arrivedAnimal, 1);
        verifyRowCreated("study", "protocolAssignment", arrivedAnimal, 1);
        verifyRowCreated("study", "animal_group_members", arrivedAnimal, 1);
        verifyRowCreated("study", "demographics", arrivedAnimal, 1);
        verifyRowCreated("study", "housing", arrivedAnimal, 1);

        log("Verifying the social code reached demographics");
        goToSchemaBrowser();
        table = viewQueryData("study", "demographics");
        table.setFilter("Id", "Equals", arrivedAnimal);
        Assert.assertEquals("Social code entered on the arrival form did not reach demographics",
                Arrays.asList(socialCode), table.getRowDataAsText(0, "socialCode"));
        Assert.assertEquals("Generation seeded by the arrival form did not reach demographics",
                Arrays.asList("0"), table.getRowDataAsText(0, "generation"));

        log("Verifying the birth date reached demographics and agrees with the birth record");
        String arrivalBirthDay = now.minusDays(7).format(_dateFormat);
        assertEquals("Birth record does not carry the birth date entered on the arrival form",
                arrivalBirthDay, getDatasetDay("birth", arrivedAnimal, "date"));
        assertEquals("Demographics birth date does not match the birth record",
                arrivalBirthDay, getDatasetDay("demographics", arrivedAnimal, "birth"));
    }

    @Test
    public void testAssignmentFormLookupColumns()
    {
        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Assignment"));
        lockForm();

        Ext4GridRef protocols = getExpandedFormSectionGrid("Protocol Assignment");
        _helper.addRecordToGrid(protocols);
        protocols.setGridCell(1, "Id", aliveAnimalId);

        log("Verifying the protocol dropdown lists each protocol with its investigator");
        // setGridCell clicks the list item whose text is exactly this, so selecting it is the check on the format
        protocols.setGridCell(1, "protocol", protocolChoice(DUMMY_PROTOCOL, DUMMY_INVES_LAST_NAME));
        Assert.assertEquals("Protocol dropdown stored something other than the protocol id",
                DUMMY_PROTOCOL, protocols.getFieldValue(1, "protocol"));

        log("Verifying the protocol description follows the selected protocol");
        Assert.assertEquals("Protocol description did not follow the selected protocol",
                DUMMY_PROTOCOL_DESCRIPTION, protocols.getFieldValue(1, "protocol/description"));
        protocols.setGridCell(1, "protocol", protocolChoice(PROTOCOL_ID, INVES_LAST_NAME));
        Assert.assertEquals("Protocol description did not follow a changed protocol",
                PROTOCOL_DESCRIPTION, protocols.getFieldValue(1, "protocol/description"));

        Assert.assertEquals("Protocol Description is not the last column",
                "protocol/description", getLastVisibleColumn(protocols));
        Assert.assertEquals("Wrong header over the protocol description",
                "Protocol Description", getColumnProperty(protocols, "protocol/description", "text"));
        Assert.assertEquals("Protocol Description should not be editable",
                false, getColumnProperty(protocols, "protocol/description", "editable"));

        Ext4GridRef projects = getExpandedFormSectionGrid("Project Assignment");
        _helper.addRecordToGrid(projects);
        projects.setGridCell(1, "Id", aliveAnimalId);
        projects.setGridCell(1, "project", PROJECT_ID);

        log("Verifying the project account follows the selected project");
        Assert.assertEquals("Project account did not follow the selected project",
                ACCOUNT_ID_2, projects.getFieldValue(1, "project/account"));
        Assert.assertEquals("Project Account is not the last column",
                "project/account", getLastVisibleColumn(projects));
        Assert.assertEquals("Wrong header over the project account",
                "Project Account", getColumnProperty(projects, "project/account", "text"));
        Assert.assertEquals("Project Account should not be editable",
                false, getColumnProperty(projects, "project/account", "editable"));

        // every check above reads the unsaved row, so discard rather than submit and leave the animal's own
        // assignments alone
        _helper.discardForm();
    }

    @Test
    public void testCurrentValueColumns() throws Exception
    {
        String animalId = "TESTCURVAL1";
        String currentGroup = "T";
        String currentGroupTitle = "Time-Mated";
        String newGroup = "P";
        String newGroupTitle = "Project Breeding";

        log("Creating an animal with a current group, protocol and project");
        Date pastDate = TIME_FORMAT.parse("2012-01-03 09:30");
        saveRows("demographics", new String[]{"Id", "Species", "Birth", "Gender", "date", "calculated_status", "objectid", "performedby"},
                new Object[][]{{animalId, "MMU", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004}});
        saveRows("Housing", new String[]{"Id", "date", "enddate", "room", "cage", "performedby"},
                new Object[][]{{animalId, pastDate, null, getRooms()[0], CAGE_IN_R1, 1004}});
        saveRows("animal_group_members", new String[]{"Id", "date", "enddate", "groupId", "performedby"},
                new Object[][]{{animalId, pastDate, null, currentGroup, 1004}});
        saveRows("protocolAssignment", new String[]{"Id", "date", "enddate", "protocol", "performedby"},
                new Object[][]{{animalId, pastDate, null, PROTOCOL_ID, 1004}});
        saveRows("Assignment", new String[]{"Id", "date", "enddate", "project", "performedby"},
                new Object[][]{{animalId, pastDate, null, PROTOCOL_PROJECT_ID, 1004}});

        Map<String, Object> current = getCurrentValues(animalId);
        assertEquals("Unexpected current group for " + animalId, List.of(currentGroupTitle), current.get("activeAnimalGroups/groups"));
        String currentProtocol = (String) current.get("activeProtocolAssignments/protocolDisplayName");
        Assert.assertTrue("No current protocol for " + animalId, StringUtils.isNotBlank(currentProtocol));
        String currentProject = String.valueOf(current.get("activeProjectAssignments/project"));

        log("Verifying the group assignment form shows the current group");
        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Animal Group Assignment"));
        Ext4GridRef groups = getExpandedFormSectionGrid("Group Assignments");
        _helper.addRecordToGrid(groups);
        groups.setGridCell(1, "Id", animalId);
        verifyCurrentValueColumn(groups, "Id/activeAnimalGroups/groups", "Current Group", currentGroupTitle, "groupId", "New Group");
        groups.setGridCell(1, "groupId", newGroupTitle);
        submitForm("Submit Final", "Finalize");
        verifyPreviousRecordClosed("animal_group_members", "groupId", animalId, currentGroup, newGroup);

        log("Verifying the assignment form shows the current protocol and project");
        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Assignment"));
        lockForm();
        Ext4GridRef protocols = getExpandedFormSectionGrid("Protocol Assignment");
        _helper.addRecordToGrid(protocols);
        protocols.setGridCell(1, "Id", animalId);
        verifyCurrentValueColumn(protocols, "Id/activeProtocolAssignments/protocolDisplayName", "Current Protocol", currentProtocol, "protocol", "New Protocol");
        protocols.setGridCell(1, "protocol", protocolChoice(DUMMY_PROTOCOL, DUMMY_INVES_LAST_NAME));

        Ext4GridRef projects = getExpandedFormSectionGrid("Project Assignment");
        _helper.addRecordToGrid(projects);
        projects.setGridCell(1, "Id", animalId);
        verifyCurrentValueColumn(projects, "Id/activeProjectAssignments/project", "Current Project", currentProject, "project", "New Project");
        projects.setGridCell(1, "project", PROJECT_ID);

        log("Verifying a saved draft does not count as the current protocol or project");
        waitForFormValidationToClear();
        clickAndWait(Locator.linkWithText("Save Draft"));
        Map<String, Object> afterDraft = getCurrentValues(animalId);
        assertEquals("A draft protocol assignment counted as current", currentProtocol, afterDraft.get("activeProtocolAssignments/protocolDisplayName"));
        assertEquals("A draft project assignment counted as current", currentProject, String.valueOf(afterDraft.get("activeProjectAssignments/project")));

        SelectRowsCommand draftSelect = new SelectRowsCommand("study", "assignment");
        draftSelect.setColumns(List.of("taskid"));
        draftSelect.addFilter(new Filter("Id", animalId));
        draftSelect.addFilter(new Filter("project", PROJECT_ID));
        String taskid = (String) draftSelect.execute(getApiHelper().getConnection(), getContainerPath()).getRows().getFirst().get("taskid");

        beginAt(WebTestHelper.buildURL("ehr", getContainerPath(), "dataEntryForm", Map.of("formType", "Assignment", "taskid", taskid)));
        waitForElement(Locator.linkWithText("Submit Final"), WAIT_FOR_PAGE);
        lockForm();
        protocols = getExpandedFormSectionGrid("Protocol Assignment");
        verifyCurrentValueColumn(protocols, "Id/activeProtocolAssignments/protocolDisplayName", "Current Protocol", currentProtocol, "protocol", "New Protocol");
        assertEquals("Reopened draft has the wrong number of protocol rows", 1, protocols.getRowCount());
        projects = getExpandedFormSectionGrid("Project Assignment");
        verifyCurrentValueColumn(projects, "Id/activeProjectAssignments/project", "Current Project", currentProject, "project", "New Project");
        assertEquals("Reopened draft has the wrong number of project rows", 1, projects.getRowCount());
        submitForm("Submit Final", "Finalize");

        verifyPreviousRecordClosed("protocolAssignment", "protocol", animalId, PROTOCOL_ID, DUMMY_PROTOCOL);
        verifyPreviousRecordClosed("assignment", "project", animalId, PROTOCOL_PROJECT_ID, PROJECT_ID);
    }

    /** The values the current-value columns read, which the client store fetches the same way. */
    private Map<String, Object> getCurrentValues(String animalId) throws Exception
    {
        SelectRowsCommand select = new SelectRowsCommand("study", "Animal");
        select.setColumns(List.of("activeAnimalGroups/groups", "activeProtocolAssignments/protocolDisplayName", "activeProjectAssignments/project"));
        select.addFilter(new Filter("Id", animalId));
        List<Map<String, Object>> rows = select.execute(getApiHelper().getConnection(), getContainerPath()).getRows();
        assertEquals("Expected one study.Animal row for " + animalId, 1, rows.size());
        return rows.getFirst();
    }

    private void saveRows(String queryName, String[] fields, Object[][] data) throws Exception
    {
        SimplePostCommand insertCommand = getApiHelper().prepareInsertCommand("study", queryName, "lsid", fields, data);
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());
    }

    private void verifyCurrentValueColumn(Ext4GridRef grid, String currentColumn, String currentHeader, String expectedCurrent,
                                          String newColumn, String newHeader)
    {
        waitFor(() -> expectedCurrent.equals(String.valueOf(grid.getFieldValue(1, currentColumn))),
                currentHeader + " was not filled with " + expectedCurrent + ", found " + grid.getFieldValue(1, currentColumn), WAIT_FOR_JAVASCRIPT);
        assertEquals("Wrong header over " + currentColumn, currentHeader, getColumnProperty(grid, currentColumn, "text"));
        assertEquals(currentHeader + " should not be editable", false, getColumnProperty(grid, currentColumn, "editable"));
        assertEquals("Wrong header over " + newColumn, newHeader, getColumnProperty(grid, newColumn, "text"));
    }

    /** Saving a new record should end the animal's open one, leaving only the new record open. */
    private void verifyPreviousRecordClosed(String queryName, String valueColumn, String animalId, String previousValue, String newValue) throws Exception
    {
        SelectRowsCommand select = new SelectRowsCommand("study", queryName);
        select.setColumns(List.of(valueColumn, "enddate"));
        select.addFilter(new Filter("Id", animalId));
        Map<String, Boolean> openByValue = new HashMap<>();
        for (Map<String, Object> row : select.execute(getApiHelper().getConnection(), getContainerPath()).getRows())
            openByValue.put(String.valueOf(row.get(valueColumn)), row.get("enddate") == null);

        assertEquals("Unexpected " + queryName + " records for " + animalId, Set.of(previousValue, newValue), openByValue.keySet());
        Assert.assertFalse("Previous " + queryName + " record was not closed", openByValue.get(previousValue));
        Assert.assertTrue("New " + queryName + " record should be open", openByValue.get(newValue));
    }

    /** The text every protocol dropdown displays for a protocol, built by ehr/activeProtocols.sql. */
    private static String protocolChoice(String protocol, String investigatorLastName)
    {
        return protocol + " - " + investigatorLastName;
    }

    /** A column config property, such as the rendered header or editability, which only the config carries. Null when
     * the grid has no such column, so an assertion against it fails rather than passing vacuously. */
    private Object getColumnProperty(Ext4GridRef grid, String dataIndex, String property)
    {
        return grid.getFnEval("for (var i=0;i<this.columns.length;i++){if (this.columns[i].dataIndex == '" + dataIndex + "') return this.columns[i]['" + property + "'];} return null;");
    }

    private String getLastVisibleColumn(Ext4GridRef grid)
    {
        return (String) grid.getFnEval("var last = null; for (var i=0;i<this.columns.length;i++){if (!this.columns[i].hidden) last = this.columns[i].dataIndex;} return last;");
    }

    @Test
    public void testNewProtocolForm() throws Exception
    {
        String protocol = "NEWPROT-01";
        LocalDate approve = LocalDate.now().minusDays(30);
        LocalDate expectedEnd = approve.plusYears(3);

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("New Protocol"));
        lockForm();

        log("Entering the protocol");
        _helper.getExt4FieldForFormSection("Protocol", "IACUC Protocol").setValue(protocol);
        _helper.getExt4FieldForFormSection("Protocol", "Title").setValue("New protocol form fixture");
        _helper.getExt4FieldForFormSection("Protocol", "PI").setValue(getInvestigatorRowId(INVES_LAST_NAME));
        getProtocolFormField("project_type").setValue("E");
        _helper.getExt4FieldForFormSection("Protocol", "Approval Date").setValue(approve.format(_dateFormat));

        log("Verifying the end date defaults to three years after approval");
        waitFor(() -> expectedEnd.format(_dateFormat).equals(toDay(_helper.getExt4FieldForFormSection("Protocol", "End Date").getDateValue())),
                "End date did not default from the approval date", WAIT_FOR_JAVASCRIPT);

        log("Verifying the IBC approval number appears only once IBC approval is required");
        Ext4FieldRef ibcNumber = getProtocolFormField("ibc_approval_num");
        Assert.assertFalse("IBC approval number should start hidden", ibcNumber.isVisible());
        getProtocolFormField("ibc_approval_required").setChecked(true);
        waitFor(ibcNumber::isVisible, "IBC approval number did not appear", WAIT_FOR_JAVASCRIPT);
        getProtocolFormField("ibc_approval_required").setChecked(false);
        waitForFormValidationToSettle();

        log("Entering the description and TB exemption");
        // a hidden field still accepts a value, so check visibility before setting it
        Ext4FieldRef description = getProtocolFormField("description");
        Assert.assertTrue("Description should be visible", description.isVisible());
        description.setValue("New protocol form description");
        Ext4FieldRef exemptTB = getProtocolFormField("exemptTB");
        Assert.assertTrue("Exempt TB should be visible", exemptTB.isVisible());
        exemptTB.setChecked(true);

        Ext4GridRef counts = _helper.getExt4GridForFormSection("Animals Allowed Per Species");
        _helper.addRecordToGrid(counts);
        counts.setGridCell(1, "species", "Rhesus Macaque");
        counts.setGridCell(1, "allowed", "12");

        submitForm("Submit Final", "Finalize");

        log("Verifying the protocol was saved with its approval defaults");
        SelectRowsCommand select = new SelectRowsCommand("ehr", "protocol");
        select.setColumns(List.of("protocol", "approve", "first_approval", "enddate", "description", "exemptTB"));
        select.addFilter(new Filter("protocol", protocol));
        List<Map<String, Object>> protocols = select.execute(getApiHelper().getConnection(), getContainerPath()).getRows();
        assertEquals("Expected one protocol row", 1, protocols.size());
        assertEquals("Wrong approval date", approve.format(_dateFormat), toDay(protocols.get(0).get("approve")));
        assertEquals("First approval should follow the approval date", approve.format(_dateFormat), toDay(protocols.get(0).get("first_approval")));
        assertEquals("Wrong end date", expectedEnd.format(_dateFormat), toDay(protocols.get(0).get("enddate")));
        assertEquals("Wrong description", "New protocol form description", protocols.get(0).get("description"));
        assertEquals("Exempt TB was not saved", true, protocols.get(0).get("exemptTB"));

        log("Verifying amendment 0 and its counts were saved with the protocol");
        Map<Integer, Map<String, Object>> amendments = getAmendments(protocol);
        assertEquals("Expected only amendment 0", Set.of(0), amendments.keySet());
        assertAmendmentWindow(amendments, 0, approve, expectedEnd);
        assertCounts(amendments.get(0), Map.of("MMU", 12));
    }

    @Test
    public void testProtocolAmendmentRules() throws Exception
    {
        String protocol = "AMENDRULES-01";
        LocalDate approve = LocalDate.now().minusYears(1);
        LocalDate end = approve.plusYears(3);
        LocalDate first = approve.plusMonths(6);
        insertProtocol(protocol, approve, end);

        log("Verifying the first amendment is numbered 0 and runs to the protocol's end date");
        insertAmendment(protocol, approve, null);
        assertAmendmentWindow(getAmendments(protocol), 0, approve, end);

        log("Verifying a new amendment takes the next number and closes the previous one the day before it takes effect");
        insertAmendment(protocol, first, null);
        Map<Integer, Map<String, Object>> amendments = getAmendments(protocol);
        assertAmendmentWindow(amendments, 0, approve, first.minusDays(1));
        assertAmendmentWindow(amendments, 1, first, end);

        log("Verifying the amendment date rules");
        assertRejected("Effective date must be after amendment 1", () -> insertAmendment(protocol, first, null));
        assertRejected("Effective date is after the protocol's end date", () -> insertAmendment(protocol, end.plusDays(1), end.plusDays(30)));
        assertRejected("End date cannot be before the effective date", () -> updateAmendmentEnddate(amendments.get(1), first.minusDays(1)));
        assertRejected("Overlaps amendment 1", () -> updateAmendmentEnddate(amendments.get(0), first.plusDays(10)));

        log("Verifying the latest amendment follows the protocol's end date");
        LocalDate extended = end.plusYears(1);
        updateProtocolEnddate(protocol, extended);
        assertAmendmentWindow(getAmendments(protocol), 1, first, extended);
        assertRejected("End date is before amendment 1", () -> updateProtocolEnddate(protocol, first.minusDays(1)));

        log("Verifying moving an amendment's effective date moves the end of the one before it");
        LocalDate later = first.plusDays(5);
        updateAmendmentDate(amendments.get(1), later);
        Map<Integer, Map<String, Object>> moved = getAmendments(protocol);
        assertAmendmentWindow(moved, 0, approve, later.minusDays(1));
        assertAmendmentWindow(moved, 1, later, extended);
        LocalDate earlier = first.minusDays(5);
        updateAmendmentDate(amendments.get(1), earlier);
        moved = getAmendments(protocol);
        assertAmendmentWindow(moved, 0, approve, earlier.minusDays(1));
        assertAmendmentWindow(moved, 1, earlier, extended);
        assertRejected("Effective date must be after amendment 0", () -> updateAmendmentDate(amendments.get(1), approve));
        updateAmendmentDate(amendments.get(1), first);
        assertAmendmentWindow(getAmendments(protocol), 0, approve, first.minusDays(1));

        log("Verifying a protocol with amendments cannot be deleted or duplicated");
        assertRejected("has amendments and cannot be deleted", () -> {
            DeleteRowsCommand delete = new DeleteRowsCommand("ehr", "protocol");
            delete.addRow(Map.of("protocol", protocol));
            delete.execute(getApiHelper().getConnection(), getContainerPath());
        });
        assertRejected("There is already a record where protocol equals", () -> insertProtocol(protocol, approve, end));

        log("Verifying a species can be entered only once per amendment");
        String amendmentId = (String) amendments.get(1).get("objectid");
        assertRejected("Species MMU is entered more than once", () -> insertCounts(protocol, amendmentId, "MMU", "MMU"));
        insertCounts(protocol, amendmentId, "MMU");
        assertRejected("Species MMU is entered more than once", () -> insertCounts(protocol, amendmentId, "MMU"));
        insertCounts(protocol, (String) amendments.get(0).get("objectid"), "MMU");

        log("Verifying only the latest amendment can be deleted, and deleting it reopens the one before");
        assertRejected("Only the latest amendment, 1, can be deleted", () -> deleteAmendment(amendments.get(0)));
        deleteAmendment(amendments.get(1));
        Map<Integer, Map<String, Object>> remaining = getAmendments(protocol);
        assertEquals("Expected only amendment 0 after the delete", Set.of(0), remaining.keySet());
        assertAmendmentWindow(remaining, 0, approve, extended);
        assertCounts(amendments.get(1), Map.of());
    }

    private void updateAmendmentDate(Map<String, Object> amendment, LocalDate date) throws IOException, CommandException
    {
        UpdateRowsCommand update = new UpdateRowsCommand("nbri_ehr", "ProtocolAmendment");
        update.addRow(Map.of("rowid", amendment.get("rowid"), "date", date.format(_dateFormat)));
        update.execute(getApiHelper().getConnection(), getContainerPath());
    }

    private void deleteAmendment(Map<String, Object> amendment) throws IOException, CommandException
    {
        DeleteRowsCommand delete = new DeleteRowsCommand("nbri_ehr", "ProtocolAmendment");
        delete.addRow(Map.of("rowid", amendment.get("rowid")));
        delete.execute(getApiHelper().getConnection(), getContainerPath());
    }

    private void insertCounts(String protocol, String amendmentId, String... species) throws IOException, CommandException
    {
        InsertRowsCommand insert = new InsertRowsCommand("ehr", "protocol_counts");
        for (String s : species)
            insert.addRow(Map.of("protocol", protocol, "amendmentId", amendmentId, "species", s, "allowed", 5));
        insert.execute(getApiHelper().getConnection(), getContainerPath());
    }

    @Test
    public void testProtocolAmendmentFormSeedsCounts() throws Exception
    {
        String protocol = "AMENDSEED-01";
        LocalDate approve = LocalDate.now().minusYears(1);
        LocalDate end = approve.plusYears(3);
        LocalDate effective = LocalDate.now().minusDays(1);
        insertProtocol(protocol, approve, end);
        insertAmendment(protocol, approve, null);

        InsertRowsCommand insertCounts = new InsertRowsCommand("ehr", "protocol_counts");
        String originalId = (String) getAmendments(protocol).get(0).get("objectid");
        insertCounts.addRow(Map.of("protocol", protocol, "amendmentId", originalId, "species", "CAP", "allowed", 3));
        insertCounts.addRow(Map.of("protocol", protocol, "amendmentId", originalId, "species", "MMU", "allowed", 8));
        insertCounts.execute(getApiHelper().getConnection(), getContainerPath());

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Protocol Amendment"));
        lockForm();

        log("Verifying choosing a protocol seeds the grid with the counts in effect");
        _helper.getExt4FieldForFormSection("Protocol Amendment", "IACUC Protocol").setValue(protocol);
        Ext4GridRef counts = _helper.getExt4GridForFormSection("Animals Allowed Per Species");
        counts.waitForRowCount(2);
        assertEquals("Wrong species seeded first", "CAP", counts.getFieldValue(1, "species"));
        assertEquals("Wrong count seeded for CAP", 3, ((Number) counts.getFieldValue(1, "allowed")).intValue());
        assertEquals("Wrong species seeded second", "MMU", counts.getFieldValue(2, "species"));
        assertEquals("Wrong count seeded for MMU", 8, ((Number) counts.getFieldValue(2, "allowed")).intValue());

        counts.setGridCell(2, "allowed", "10");
        _helper.getExt4FieldForFormSection("Protocol Amendment", "Effective Date").setValue(effective.format(_dateFormat));
        submitForm("Submit Final", "Finalize");

        log("Verifying the amendment was saved with its counts and superseded amendment 0");
        Map<Integer, Map<String, Object>> amendments = getAmendments(protocol);
        assertAmendmentWindow(amendments, 0, approve, effective.minusDays(1));
        assertAmendmentWindow(amendments, 1, effective, end);
        assertCounts(amendments.get(1), Map.of("CAP", 3, "MMU", 10));
    }

    private interface ApiCall
    {
        void run() throws IOException, CommandException;
    }

    private void assertRejected(String expectedMessage, ApiCall call)
    {
        try
        {
            call.run();
            Assert.fail("Save should have been rejected with: " + expectedMessage);
        }
        catch (IOException | CommandException e)
        {
            Assert.assertTrue("Unexpected failure, expected \"" + expectedMessage + "\": " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains(expectedMessage));
        }
    }

    private Ext4FieldRef getProtocolFormField(String name)
    {
        return _ext4Helper.queryOne("panel[title='Protocol'] [name='" + name + "']", Ext4FieldRef.class);
    }

    private Object getInvestigatorRowId(String lastName) throws IOException, CommandException
    {
        SelectRowsCommand select = new SelectRowsCommand("ehr", "investigators");
        select.setColumns(List.of("rowid"));
        select.addFilter(new Filter("lastName", lastName));
        return select.execute(getApiHelper().getConnection(), getContainerPath()).getRows().get(0).get("rowid");
    }

    private void insertProtocol(String protocol, LocalDate approve, LocalDate enddate) throws IOException, CommandException
    {
        InsertRowsCommand insert = new InsertRowsCommand("ehr", "protocol");
        insert.addRow(Map.of("protocol", protocol, "title", protocol, "approve", approve.format(_dateFormat), "enddate", enddate.format(_dateFormat)));
        insert.execute(getApiHelper().getConnection(), getContainerPath());
    }

    private void updateProtocolEnddate(String protocol, LocalDate enddate) throws IOException, CommandException
    {
        UpdateRowsCommand update = new UpdateRowsCommand("ehr", "protocol");
        update.addRow(Map.of("protocol", protocol, "enddate", enddate.format(_dateFormat)));
        update.execute(getApiHelper().getConnection(), getContainerPath());
    }

    /** A null end date leaves it to the server. */
    private void insertAmendment(String protocol, LocalDate date, @Nullable LocalDate enddate) throws IOException, CommandException
    {
        Map<String, Object> row = new HashMap<>(Map.of("protocol", protocol, "date", date.format(_dateFormat)));
        if (enddate != null)
            row.put("enddate", enddate.format(_dateFormat));

        InsertRowsCommand insert = new InsertRowsCommand("nbri_ehr", "ProtocolAmendment");
        insert.addRow(row);
        insert.execute(getApiHelper().getConnection(), getContainerPath());
    }

    private void updateAmendmentEnddate(Map<String, Object> amendment, LocalDate enddate) throws IOException, CommandException
    {
        UpdateRowsCommand update = new UpdateRowsCommand("nbri_ehr", "ProtocolAmendment");
        update.addRow(Map.of("rowid", amendment.get("rowid"), "enddate", enddate.format(_dateFormat)));
        update.execute(getApiHelper().getConnection(), getContainerPath());
    }

    /** A protocol's amendments, keyed by amendment number. */
    private Map<Integer, Map<String, Object>> getAmendments(String protocol) throws IOException, CommandException
    {
        SelectRowsCommand select = new SelectRowsCommand("nbri_ehr", "ProtocolAmendment");
        select.setColumns(List.of("rowid", "objectid", "amendmentNumber", "date", "enddate"));
        select.addFilter(new Filter("protocol", protocol));

        Map<Integer, Map<String, Object>> amendments = new HashMap<>();
        for (Map<String, Object> row : select.execute(getApiHelper().getConnection(), getContainerPath()).getRows())
            amendments.put(((Number) row.get("amendmentNumber")).intValue(), row);

        return amendments;
    }

    private void assertAmendmentWindow(Map<Integer, Map<String, Object>> amendments, int number, LocalDate date, LocalDate enddate)
    {
        Map<String, Object> amendment = amendments.get(number);
        assertNotNull("No amendment " + number, amendment);
        assertEquals("Wrong effective date on amendment " + number, date.format(_dateFormat), toDay(amendment.get("date")));
        assertEquals("Wrong end date on amendment " + number, enddate.format(_dateFormat), toDay(amendment.get("enddate")));
    }

    private void assertCounts(Map<String, Object> amendment, Map<String, Integer> expected) throws IOException, CommandException
    {
        SelectRowsCommand select = new SelectRowsCommand("ehr", "protocol_counts");
        select.setColumns(List.of("species", "allowed"));
        select.addFilter(new Filter("amendmentId", amendment.get("objectid")));

        Map<String, Integer> counts = new HashMap<>();
        for (Map<String, Object> row : select.execute(getApiHelper().getConnection(), getContainerPath()).getRows())
            counts.put((String) row.get("species"), ((Number) row.get("allowed")).intValue());

        assertEquals("Wrong counts on amendment " + amendment.get("amendmentNumber"), expected, counts);
    }

    @Test
    public void testProtocolAssignmentValidation() throws Exception
    {
        String limited = "PALIMIT-01";
        String batch = "PABATCH-01";
        String amended = "PAAMEND-01";
        String expired = "PAEXPIRED-01";
        String expiring = "PAEXPIRING-01";
        String open = "PAOPEN-01";
        String[] animals = {"PAV0001", "PAV0002", "PAV0003", "PAV0004", "PAV0005", "PAV0006", "PAV0007", "PAV0008", "PAV0009", "PAV0010", "PAV0011"};
        LocalDate today = LocalDate.now();
        createAnimals("MMU", animals);

        createLimitedProtocol(limited, today.minusYears(1), today.plusYears(1), "MMU", 2);
        createLimitedProtocol(batch, today.minusYears(1), today.plusYears(1), "MMU", 1);
        insertProtocol(expired, today.minusYears(2), today.minusDays(30));
        insertProtocol(expiring, today.minusYears(2), today.plusYears(1));
        insertProtocol(open, today.minusYears(1), today.plusYears(1));

        log("Verifying an expired protocol is refused, draft or final");
        assertFieldError(validateOnly(protocolAssignmentInsert(expired, today, EHRQCState.IN_PROGRESS, animals[0])), "protocol", "ERROR", "Protocol " + expired + " expired on ");
        assertFieldError(saveExpectingError(protocolAssignmentInsert(expired, today, EHRQCState.COMPLETED, animals[0])), "protocol", "ERROR", "Protocol " + expired + " expired on ");

        log("Verifying saved drafts do not count against the limit");
        assignToProtocol(limited, today, EHRQCState.IN_PROGRESS, animals[0], animals[1], animals[2]);
        assignToProtocol(limited, today, EHRQCState.COMPLETED, animals[3], animals[4]);

        log("Verifying a full protocol warns on another draft and refuses another final assignment");
        String full = overLimit(limited, "MMU", 3, 2, 0);
        assertFieldError(validateOnly(protocolAssignmentInsert(limited, today, EHRQCState.IN_PROGRESS, animals[5])), "protocol", "WARN", full);
        assertFieldError(saveExpectingError(protocolAssignmentInsert(limited, today, EHRQCState.COMPLETED, animals[5])), "protocol", "ERROR", full);

        log("Verifying finalizing a draft past the limit is refused");
        Map<String, Object> finalized = new HashMap<>(protocolAssignmentRow(animals[0], limited, today));
        finalized.put("QCStateLabel", EHRQCState.COMPLETED.label);
        assertFieldError(saveExpectingError(prepareUpdate("protocolAssignment", finalized)), "protocol", "ERROR", full);

        log("Verifying a completed assignment cannot be moved onto a full protocol");
        assignToProtocol(open, today, EHRQCState.COMPLETED, animals[6]);
        Map<String, Object> moved = new HashMap<>(protocolAssignmentRow(animals[6], open, today));
        moved.put("protocol", limited);
        assertFieldError(saveExpectingError(prepareUpdate("protocolAssignment", moved)), "protocol", "ERROR", full);
        assertEquals("A refused move should leave the assignment where it was", 1, countRows("protocolAssignment", new Filter("Id", animals[6]), new Filter("protocol", open)));

        log("Verifying rows saved together count against each other, and refusing one refuses them all");
        assertFieldError(saveExpectingError(protocolAssignmentInsert(batch, today, EHRQCState.COMPLETED, animals[7], animals[8])), "protocol", "ERROR", overLimit(batch, "MMU", 2, 1, 0));
        assertEquals("No assignment in a refused save should have been kept", 0, countRows("protocolAssignment", new Filter("protocol", batch)));

        log("Verifying a backdated assignment counts against the amendment in effect on its date");
        insertProtocol(amended, today.minusYears(1), today.plusYears(1));
        insertAmendment(amended, today.minusYears(1), null);
        insertAmendment(amended, today.minusMonths(3), null);
        Map<Integer, Map<String, Object>> amendments = getAmendments(amended);
        insertCount(amended, (String) amendments.get(0).get("objectid"), "MMU", 1);
        insertCount(amended, (String) amendments.get(1).get("objectid"), "MMU", 5);
        assignToProtocol(amended, today.minusMonths(6), EHRQCState.COMPLETED, animals[9]);
        assignToProtocol(amended, today, EHRQCState.COMPLETED, animals[10]);
        assertFieldError(saveExpectingError(protocolAssignmentInsert(amended, today.minusMonths(5), EHRQCState.COMPLETED, animals[7])), "protocol", "ERROR", overLimit(amended, "MMU", 2, 1, 0));

        log("Verifying an assignment on a protocol that has since expired can still be edited, but not redated past the expiry");
        LocalDate assigned = today.minusYears(1);
        assignToProtocol(expiring, assigned, EHRQCState.COMPLETED, animals[8]);
        updateProtocolEnddate(expiring, today.minusDays(30));
        Map<String, Object> edited = new HashMap<>(protocolAssignmentRow(animals[8], expiring, assigned));
        edited.put("remark", "Edited after the protocol expired");
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), prepareUpdate("protocolAssignment", edited), getExtraContext());
        edited.put("date", today.format(_dateFormat));
        assertFieldError(validateOnly(prepareUpdate("protocolAssignment", edited)), "protocol", "ERROR", "Protocol " + expiring + " expired on ");
    }

    @Test
    public void testArrivalProtocolValidation() throws Exception
    {
        String limited = "ARRLIMIT-01";
        String batch = "ARRBATCH-01";
        String expired = "ARREXPIRED-01";
        String resident = "ARRRES0001";
        String returning = "ARRRET0001";
        String[] arriving = {"ARRV0001", "ARRV0002"};
        LocalDate today = LocalDate.now();
        getApiHelper().deleteAllRecords("study", "demographics", new Filter("Id", StringUtils.join(arriving, ";"), Filter.Operator.IN));
        createAnimals("MNE", resident, returning);

        createLimitedProtocol(limited, today.minusYears(1), today.plusYears(1), "MNE", 1);
        createLimitedProtocol(batch, today.minusYears(1), today.plusYears(1), "MNE", 1);
        insertProtocol(expired, today.minusYears(2), today.minusDays(30));
        assignToProtocol(limited, today, EHRQCState.COMPLETED, resident);

        log("Verifying an expired arrival protocol is refused");
        assertFieldError(validateOnly(arrivalInsert(expired, EHRQCState.IN_PROGRESS, false, "MNE", arriving[0])), "arrivalProtocol", "ERROR", "Protocol " + expired + " expired on ");

        log("Verifying a new animal is counted under the species entered on the arrival");
        String full = overLimit(limited, "MNE", 2, 1, 0);
        assertFieldError(validateOnly(arrivalInsert(limited, EHRQCState.IN_PROGRESS, false, "MNE", arriving[0])), "arrivalProtocol", "WARN", full);
        assertFieldError(saveExpectingError(arrivalInsert(limited, EHRQCState.COMPLETED, false, "MNE", arriving[0])), "arrivalProtocol", "ERROR", full);
        for (String query : List.of("arrival", "demographics", "protocolAssignment"))
            assertEquals("A refused arrival should leave no " + query + " row", 0, countRows(query, new Filter("Id", arriving[0])));

        log("Verifying animals arriving together count against each other");
        assertFieldError(validateOnly(arrivalInsert(batch, EHRQCState.IN_PROGRESS, false, "MNE", arriving)), "arrivalProtocol", "WARN", overLimit(batch, "MNE", 2, 1, 0));

        log("Verifying a rearrival with no species entered is counted under the species on record");
        assertFieldError(validateOnly(arrivalInsert(limited, EHRQCState.IN_PROGRESS, true, null, returning)), "arrivalProtocol", "WARN", full);
    }

    @Test
    public void testBirthProtocolValidation() throws Exception
    {
        String limited = "BIRTHLIMIT-01";
        String expired = "BIRTHEXPIRED-01";
        String damId = "BPDAM0001";
        String sireId = "BPSIRE0001";
        String newborn = "BPV0001";
        LocalDate today = LocalDate.now();
        getApiHelper().deleteAllRecords("study", "demographics", new Filter("Id", newborn));
        createBreedingPair(damId, sireId, "CAP");

        // only the dam's species has a count row, so the newborn is checked against it only if it inherits that species
        createLimitedProtocol(limited, today.minusYears(1), today.plusYears(1), "CAP", 1);
        insertProtocol(expired, today.minusYears(2), today.minusDays(30));
        assignToProtocol(limited, today, EHRQCState.COMPLETED, damId);

        log("Verifying an expired birth protocol is refused");
        assertFieldError(validateOnly(birthInsert(expired, EHRQCState.IN_PROGRESS, newborn, damId)), "birthProtocol", "ERROR", "Protocol " + expired + " expired on ");

        log("Verifying a newborn with no species entered is counted under its dam's species");
        String full = overLimit(limited, "CAP", 2, 1, 0);
        assertFieldError(validateOnly(birthInsert(limited, EHRQCState.IN_PROGRESS, newborn, damId)), "birthProtocol", "WARN", full);
        assertFieldError(saveExpectingError(birthInsert(limited, EHRQCState.COMPLETED, newborn, damId)), "birthProtocol", "ERROR", full);
        assertEquals("A refused birth should leave no birth row", 0, countRows("birth", new Filter("Id", newborn)));
    }

    /** A protocol with a single amendment from its approval date, approved for the given number of one species. */
    private void createLimitedProtocol(String protocol, LocalDate approve, LocalDate enddate, String species, int allowed) throws IOException, CommandException
    {
        insertProtocol(protocol, approve, enddate);
        insertAmendment(protocol, approve, null);
        insertCount(protocol, (String) getAmendments(protocol).get(0).get("objectid"), species, allowed);
    }

    private void insertCount(String protocol, String amendmentId, String species, int allowed) throws IOException, CommandException
    {
        InsertRowsCommand insert = new InsertRowsCommand("ehr", "protocol_counts");
        insert.addRow(Map.of("protocol", protocol, "amendmentId", amendmentId, "species", species, "allowed", allowed));
        insert.execute(getApiHelper().getConnection(), getContainerPath());
    }

    private static String overLimit(String protocol, String species, int used, int allowed, int amendment)
    {
        return "Protocol " + protocol + ", " + species + ": " + used + " of " + allowed + " allowed under amendment " + amendment + ".";
    }

    /** Creates living animals of one species, replacing any left behind by an earlier run. */
    private void createAnimals(String species, String... animalIds) throws Exception
    {
        String[] fields = new String[]{"Id", "Species", "Birth", "Gender", "date", "calculated_status", "objectid", "performedby"};
        Object[][] data = Arrays.stream(animalIds)
                .map(id -> new Object[]{id, species, (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004})
                .toArray(Object[][]::new);
        SimplePostCommand insertCommand = getApiHelper().prepareInsertCommand("study", "demographics", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "demographics", new Filter("Id", StringUtils.join(animalIds, ";"), Filter.Operator.IN));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());
    }

    private void assignToProtocol(String protocol, LocalDate date, EHRQCState qcState, String... animalIds)
    {
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), protocolAssignmentInsert(protocol, date, qcState, animalIds), getExtraContext());
    }

    private SimplePostCommand protocolAssignmentInsert(String protocol, LocalDate date, EHRQCState qcState, String... animalIds)
    {
        Object[][] data = Arrays.stream(animalIds)
                .map(id -> new Object[]{id, date.format(_dateFormat), protocol, qcState.label, 1004})
                .toArray(Object[][]::new);
        return getApiHelper().prepareInsertCommand("study", "protocolAssignment", "lsid", new String[]{"Id", "date", "protocol", "QCStateLabel", "performedby"}, data);
    }

    // A committing arrival or birth opens its assignments, which need a task id; species is left out when null
    private SimplePostCommand arrivalInsert(String protocol, EHRQCState qcState, boolean rearrival, @Nullable String species, String... animalIds)
    {
        Object[][] data = Arrays.stream(animalIds)
                .map(id -> new Object[]{id, LocalDate.now().format(_dateFormat), species, getFemale(), protocol, rearrival, qcState.label, 1004, UUID.randomUUID().toString()})
                .toArray(Object[][]::new);
        return getApiHelper().prepareInsertCommand("study", "arrival", "lsid", new String[]{"Id", "date", "Id/demographics/species", "Id/demographics/gender", "arrivalProtocol", "rearrival", "QCStateLabel", "performedby", "taskid"}, data);
    }

    private SimplePostCommand birthInsert(String protocol, EHRQCState qcState, String animalId, String damId)
    {
        Object[][] data = {{animalId, LocalDate.now().format(_dateFormat), damId, getFemale(), protocol, qcState.label, 1004, UUID.randomUUID().toString()}};
        return getApiHelper().prepareInsertCommand("study", "birth", "lsid", new String[]{"Id", "date", "Id/demographics/dam", "Id/demographics/gender", "birthProtocol", "QCStateLabel", "performedby", "taskid"}, data);
    }

    /** The values an update of the animal's assignment to this protocol starts from, keyed by lsid. */
    private Map<String, Object> protocolAssignmentRow(String animalId, String protocol, LocalDate date) throws IOException, CommandException
    {
        SelectRowsCommand select = new SelectRowsCommand("study", "protocolAssignment");
        select.setColumns(List.of("lsid"));
        select.addFilter(new Filter("Id", animalId));
        select.addFilter(new Filter("protocol", protocol));
        List<Map<String, Object>> rows = select.execute(getApiHelper().getConnection(), getContainerPath()).getRows();
        assertEquals("Expected one assignment of " + animalId + " to " + protocol, 1, rows.size());

        return Map.of("lsid", rows.get(0).get("lsid"), "Id", animalId, "date", date.format(_dateFormat), "protocol", protocol, "performedby", 1004);
    }

    private SimplePostCommand prepareUpdate(String queryName, Map<String, Object> values)
    {
        String[] fields = values.keySet().toArray(new String[0]);
        Object[] row = Arrays.stream(fields).map(values::get).toArray();
        return getApiHelper().prepareUpdateCommand("study", queryName, "lsid", fields, new Object[][]{row}, null);
    }

    private int countRows(String queryName, Filter... filters) throws IOException, CommandException
    {
        SelectRowsCommand select = new SelectRowsCommand("study", queryName);
        select.setColumns(List.of("lsid"));
        for (Filter filter : filters)
            select.addFilter(filter);
        return select.execute(getApiHelper().getConnection(), getContainerPath()).getRowCount().intValue();
    }

    /** Errors from a validate-only save, which reports every severity and commits nothing, as the form's own checks do. */
    private Map<String, List<String>> validateOnly(SimplePostCommand command)
    {
        JSONObject extraContext = getExtraContext();
        extraContext.put("validateOnly", true);
        return getApiHelper().extractErrors(getApiHelper().doSaveRowsExpectingError(DATA_ADMIN.getEmail(), command, extraContext).getProperties());
    }

    /** Errors from a committing save, where only an ERROR survives to refuse it. */
    private Map<String, List<String>> saveExpectingError(SimplePostCommand command)
    {
        return getApiHelper().extractErrors(getApiHelper().doSaveRowsExpectingError(DATA_ADMIN.getEmail(), command, getExtraContext()).getProperties());
    }

    /** Asserts the field carries exactly one error, of this severity, starting with this message. */
    private void assertFieldError(Map<String, List<String>> errors, String field, String severity, String messageStart)
    {
        List<String> fieldErrors = errors.getOrDefault(field, List.of());
        Assert.assertTrue("Expected one " + severity + " on " + field + " starting \"" + messageStart + "\", got: " + errors,
                fieldErrors.size() == 1 && fieldErrors.get(0).startsWith(severity + ": " + messageStart));
    }

    @Test
    public void testBirthForm() throws Exception
    {
        String bornAnimal = "80801";
        String damId = "TESTDAM01";
        String sireId = "TESTSIRE01";
        // demographics.species holds an ehr_lookups.species_codes code; the grids display its common name
        String damSpeciesCode = "CAP";
        String damSpecies = "Brown-Tufted Capuchin";
        String conceptId = "TESTCONCEPT1";
        String pregnancyType = "Research Breeding";
        int damGeneration = 2;
        // demographics.socialCode holds an ehr_lookups.social_code code; the grids display its title
        String socialCode = "Mother-rearing (for indoors)";
        // the group is an ehr_lookups.breeding_type code, carried to animal_group_members; the grids display its title
        String animalGroup = "Project Breeding";
        LocalDateTime now = LocalDateTime.now();

        log("Creating the dam and sire of the conception");
        createBreedingPair(damId, sireId, damSpeciesCode, damGeneration);

        log("Creating conception record");
        InsertRowsCommand conception = new InsertRowsCommand("study", "conception");
        int conceptionDays = 160;
        conception.addRow(Map.of("conceptId", conceptId, "date", now.minusDays(conceptionDays), "Id", damId, "sire", sireId, "QCStateLabel", "Completed", "performedby", 1004));
        conception.execute(getApiHelper().getConnection(), getContainerPath());

        log("Verifying the dam's Animal Details reports the open conception and its day count before the birth");
        assertEquals("Animal Details did not report the open conception", conceptId + " (" + conceptionDays + " days)",
                getSnapshotFieldValue(damId, "Pregnant"));

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Birth"));
        lockForm();

        Ext4GridRef births = _helper.getExt4GridForFormSection("Births");
        verifyBirthColumnOrder(births);

        log("Starting a birth record from the conception");
        startWithConception(births, conceptId, 1);

        log("Verifying the conception populated the new birth record");
        assertEquals("Conception Id was not copied from the conception", conceptId, births.getFieldValue(1, "conceptId"));
        assertEquals("Dam was not copied from the conception", damId, births.getFieldValue(1, "Id/demographics/dam"));
        assertEquals("Sire was not copied from the conception", sireId, births.getFieldValue(1, "Id/demographics/sire"));
        assertEquals("Species was not copied from the dam of the conception", damSpeciesCode, births.getFieldValue(1, "Id/demographics/species"));
        assertEquals("Generation was not derived from the dam of the conception", String.valueOf(damGeneration + 1),
                String.valueOf(births.getFieldValue(1, "Id/demographics/generation")));

        log("Verifying Conception Id is required");
        births.setGridCellJS(1, "conceptId", null);
        waitForFormError("The field: Conception Id is required");
        births.setGridCellJS(1, "conceptId", conceptId);

        births.setGridCellJS(1, "date", now.minusDays(1).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_STRING)));
        births.setGridCell(1, "Id", bornAnimal);
        births.setGridCell(1, "cage", CAGE_IN_R2);
        births.setGridCell(1, "Id/demographics/gender", "Female");
        births.setGridCell(1, "pregnancyType", pregnancyType);
        births.setGridCell(1, "Id/demographics/socialCode", socialCode);

        log("Verifying Social Code is required");
        births.setGridCellJS(1, "Id/demographics/socialCode", null);
        waitForFormError("The field: Social Code is required");
        births.setGridCell(1, "Id/demographics/socialCode", socialCode);

        log("Verifying Generation is required");
        births.setGridCellJS(1, "Id/demographics/generation", null);
        waitForFormError("The field: Generation is required");
        births.setGridCellJS(1, "Id/demographics/generation", damGeneration + 1);

        // the animal's opening project, protocol and group are entered on the birth row itself; the trigger script
        // opens the matching assignment record for each one
        births.setGridCell(1, "project", "795644");
        births.setGridCell(1, "birthProtocol", protocolChoice(PROTOCOL_ID, INVES_LAST_NAME));
        births.setGridCell(1, "groupId", animalGroup);

        log("Verifying the opening project, protocol and group are required");
        births.setGridCellJS(1, "project", null);
        waitForFormError("The field: Project is required");
        births.setGridCell(1, "project", "795644");

        births.setGridCellJS(1, "birthProtocol", null);
        waitForFormError("The field: Protocol is required");
        births.setGridCell(1, "birthProtocol", protocolChoice(PROTOCOL_ID, INVES_LAST_NAME));

        births.setGridCellJS(1, "groupId", null);
        waitForFormError("The field: Group is required");
        births.setGridCell(1, "groupId", animalGroup);

        submitForm("Submit Final", "Finalize");

        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("study", "birth");
        table.setFilter("Id", "Equals", bornAnimal);
        Assert.assertEquals("Invalid Birth record", Arrays.asList(bornAnimal), table.getRowDataAsText(0, "Id"));
        Assert.assertEquals("Invalid Birth record", Arrays.asList(CAGE_IN_R2), table.getRowDataAsText(0, "cage"));
        Assert.assertEquals("Invalid Birth record", Arrays.asList(conceptId), table.getRowDataAsText(0, "conceptId"));
        Assert.assertEquals("Invalid Birth record", Arrays.asList(pregnancyType), table.getRowDataAsText(0, "pregnancyType"));

        log("Verifying the dam and sire of the conception reached demographics");
        goToSchemaBrowser();
        table = viewQueryData("study", "demographics");
        table.setFilter("Id", "Equals", bornAnimal);
        Assert.assertEquals("Invalid demographics record", Arrays.asList(damId), table.getRowDataAsText(0, "dam"));
        Assert.assertEquals("Invalid demographics record", Arrays.asList(sireId), table.getRowDataAsText(0, "sire"));
        Assert.assertEquals("Invalid demographics record", Arrays.asList(damSpecies), table.getRowDataAsText(0, "species"));
        Assert.assertEquals("Social code entered on the birth form did not reach demographics",
                Arrays.asList(socialCode), table.getRowDataAsText(0, "socialCode"));
        Assert.assertEquals("Generation derived from the dam did not reach demographics",
                Arrays.asList(String.valueOf(damGeneration + 1)), table.getRowDataAsText(0, "generation"));

        goToSchemaBrowser();
        table = viewQueryData("study", "assignment");
        table.setFilter("Id", "Equals", bornAnimal);
        Assert.assertEquals("Invalid project assignment", Arrays.asList("795644"), table.getRowDataAsText(0, "project"));

        goToSchemaBrowser();
        table = viewQueryData("study", "protocolAssignment");
        table.setFilter("Id", "Equals", bornAnimal);
        Assert.assertEquals("Invalid protocol assignment", Arrays.asList("protocol101"), table.getRowDataAsText(0, "protocol"));

        goToSchemaBrowser();
        table = viewQueryData("study", "animal_group_members");
        table.setFilter("Id", "Equals", bornAnimal);
        Assert.assertEquals("Invalid group assignment", Arrays.asList(animalGroup), table.getRowDataAsText(0, "groupId"));

        verifyRowCreated("study", "assignment", bornAnimal, 1);
        verifyRowCreated("study", "protocolAssignment", bornAnimal, 1);
        verifyRowCreated("study", "animal_group_members", bornAnimal, 1);
        verifyRowCreated("study", "housing", bornAnimal, 1);
        verifyRowCreated("study", "demographics", bornAnimal, 1);

        log("Verifying the birth date reached demographics and agrees with the birth record");
        String bornBirthDay = now.minusDays(1).format(_dateFormat);
        assertEquals("Birth record does not carry the date entered on the birth form",
                bornBirthDay, getDatasetDay("birth", bornAnimal, "date"));
        assertEquals("Demographics birth date does not match the birth record",
                bornBirthDay, getDatasetDay("demographics", bornAnimal, "birth"));

        log("Verifying conception outcome and offspring in ConceptionsByDam");
        goToSchemaBrowser();
        DataRegionTable report = viewQueryData("study", "ConceptionsByDam");
        report.setFilter("conceptId", "Equals", conceptId);
        Assert.assertEquals("Invalid ConceptionsByDam row", Arrays.asList(damId), report.getRowDataAsText(0, "Id"));
        Assert.assertEquals("Invalid ConceptionsByDam row", Arrays.asList("Live Birth"), report.getRowDataAsText(0, "conceptionOutcome"));
        Assert.assertEquals("Invalid ConceptionsByDam row", Arrays.asList(bornAnimal), report.getRowDataAsText(0, "offspring"));
        Assert.assertEquals("A conception claimed by a birth should not be active",
                Arrays.asList("false"), report.getRowDataAsText(0, "isActive"));

        log("Verifying the birth cleared the dam's pregnancy");
        assertEquals("Animal Details still reports a conception a birth has closed", "No", getSnapshotFieldValue(damId, "Pregnant"));
    }

    @Test
    public void testDuplicateConceptionRejected() throws Exception
    {
        String firstAnimal = "80811";
        String secondAnimal = "80812";
        String damId = "TESTDAM03";
        String sireId = "TESTSIRE03";
        String damSpeciesCode = "CAP";
        String firstConcept = "TESTCONCEPT4";
        String secondConcept = "TESTCONCEPT5";
        String socialCode = "Mother-rearing (for indoors)";
        String animalGroup = "Project Breeding";
        // the severity prefix is part of the message the server sends, so asserting on it also pins the rule at
        // ERROR, which is what refuses the save
        String duplicateError = "ERROR: This conception Id is already used by another birth record";
        LocalDateTime now = LocalDateTime.now();

        log("Creating the dam and sire of the conceptions");
        createBreedingPair(damId, sireId, damSpeciesCode);

        log("Creating two conception records for that pair");
        InsertRowsCommand conceptions = new InsertRowsCommand("study", "conception");
        conceptions.addRow(Map.of("conceptId", firstConcept, "date", now.minusDays(200), "Id", damId, "sire", sireId, "QCStateLabel", "Completed", "performedby", 1004));
        conceptions.addRow(Map.of("conceptId", secondConcept, "date", now.minusDays(160), "Id", damId, "sire", sireId, "QCStateLabel", "Completed", "performedby", 1004));
        conceptions.execute(getApiHelper().getConnection(), getContainerPath());

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Birth"));
        lockForm();

        Ext4GridRef births = _helper.getExt4GridForFormSection("Births");

        log("Entering two births that both claim the first conception");
        startWithConception(births, firstConcept, 1);
        assertEquals("A dam with no generation of her own should leave the birth at generation 1", "1",
                String.valueOf(births.getFieldValue(1, "Id/demographics/generation")));
        fillBirthRow(births, 1, firstAnimal, now.minusDays(1), socialCode, animalGroup, CAGE_IN_R2);
        startWithConception(births, firstConcept, 2);
        fillBirthRow(births, 2, secondAnimal, now.minusDays(1), socialCode, animalGroup, CAGE_IN_R3);

        // Live validation only sends the row that just changed, so the rows of one form entry first reach the
        // server together on submit. A rule that compares them therefore reports by refusing the save rather than
        // while the form is being filled.
        log("Verifying the save is refused while both births claim one conception");
        submitFormExpectingError(duplicateError);

        log("Verifying the save is accepted once the second birth points at its own conception");
        births.setGridCellJS(2, "conceptId", secondConcept);

        // This rule only ever fires at submit, so waitForValidationToClear has no form error to wait out. Waiting on
        // the message itself is no better: dismissing the error dialog leaves its text in the DOM, so a wait for it
        // to disappear never passes. The submit succeeding is the only signal that the fix took.
        submitForm("Submit Final", "Finalize");

        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("study", "birth");
        table.setFilter("conceptId", "Equals", firstConcept);
        Assert.assertEquals("The first conception should have exactly one birth", 1, table.getDataRowCount());
        Assert.assertEquals("Wrong birth recorded against the first conception",
                Arrays.asList(firstAnimal), table.getRowDataAsText(0, "Id"));

        goToSchemaBrowser();
        table = viewQueryData("study", "birth");
        table.setFilter("conceptId", "Equals", secondConcept);
        Assert.assertEquals("The second conception should have exactly one birth", 1, table.getDataRowCount());
        Assert.assertEquals("Wrong birth recorded against the second conception",
                Arrays.asList(secondAnimal), table.getRowDataAsText(0, "Id"));

        // the refused save is logged server side, so account for it or the run's server error check fails the test
        checkExpectedErrors(1);
    }

    @Test
    public void testConceptionPickedFromGridCell() throws Exception
    {
        String bornAnimal = "80813";
        String firstDam = "TESTDAM04";
        String firstSire = "TESTSIRE04";
        String secondDam = "TESTDAM05";
        String secondSire = "TESTSIRE05";
        // different species on each pair, so the copy from the newly picked dam is visible
        String firstSpeciesCode = "CAP";
        String secondSpeciesCode = "MMU";
        // different generations on each dam, so the re-derivation from the newly picked dam is visible
        int firstDamGeneration = 2;
        int secondDamGeneration = 5;
        String firstConcept = "TESTCONCEPT6";
        String secondConcept = "TESTCONCEPT7";
        String socialCode = "Mother-rearing (for indoors)";
        String animalGroup = "Project Breeding";
        LocalDateTime now = LocalDateTime.now();

        log("Creating a breeding pair and a conception for each");
        createBreedingPair(firstDam, firstSire, firstSpeciesCode, firstDamGeneration);
        createBreedingPair(secondDam, secondSire, secondSpeciesCode, secondDamGeneration);

        InsertRowsCommand conceptions = new InsertRowsCommand("study", "conception");
        conceptions.addRow(Map.of("conceptId", firstConcept, "date", now.minusDays(200), "Id", firstDam, "sire", firstSire, "QCStateLabel", "Completed", "performedby", 1004));
        conceptions.addRow(Map.of("conceptId", secondConcept, "date", now.minusDays(190), "Id", secondDam, "sire", secondSire, "QCStateLabel", "Completed", "performedby", 1004));
        conceptions.execute(getApiHelper().getConnection(), getContainerPath());

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Birth"));
        lockForm();

        Ext4GridRef births = _helper.getExt4GridForFormSection("Births");
        startWithConception(births, firstConcept, 1);
        fillBirthRow(births, 1, bornAnimal, now.minusDays(1), socialCode, animalGroup, CAGE_IN_R1);
        births.setGridCell(1, "pregnancyType", "Research Breeding");

        // the codes behind these lookups are not spelled out in the test, so remember what the row carries and
        // compare afterwards rather than asserting against a literal
        Object dateBefore = births.getFieldValue(1, "date");
        Object genderBefore = births.getFieldValue(1, "Id/demographics/gender");
        Object socialCodeBefore = births.getFieldValue(1, "Id/demographics/socialCode");
        Object pregnancyTypeBefore = births.getFieldValue(1, "pregnancyType");
        Object groupBefore = births.getFieldValue(1, "groupId");

        log("Opening the picker from the Conception Id cell of the existing row");
        // a single click starts the grid's editor, which focuses the field and opens the window; startEditing() is
        // avoided here because it retries the click when it cannot find a focused editor behind the modal window
        births.getCell(1, "conceptId").findElement(getDriver()).click();
        Window<?> changeWindow = new Window.WindowFinder(getDriver()).withTitle("Change Conception").waitFor();
        Ext4ComboRef conceptionCombo = _ext4Helper.queryOne("window #conceptionField", Ext4ComboRef.class);
        Assert.assertNotNull("Conception Id field not found in the Change Conception window", conceptionCombo);
        conceptionCombo.waitForStoreLoad();
        Assert.assertEquals("The window should open on the conception the row already carries",
                firstConcept, conceptionCombo.getValue());

        conceptionCombo.setComboByDisplayValue(secondConcept);
        changeWindow.clickButton("Submit", 0);

        log("Verifying the picked conception replaced the conception fields on that row");
        waitFor(() -> secondConcept.equals(births.getFieldValue(1, "conceptId")),
                "Conception Id was not replaced by the picked conception", WAIT_FOR_JAVASCRIPT);
        assertEquals("Dam was not replaced from the picked conception", secondDam, births.getFieldValue(1, "Id/demographics/dam"));
        assertEquals("Sire was not replaced from the picked conception", secondSire, births.getFieldValue(1, "Id/demographics/sire"));
        assertEquals("Species was not replaced from the dam of the picked conception", secondSpeciesCode, births.getFieldValue(1, "Id/demographics/species"));
        assertEquals("Generation was not re-derived from the dam of the picked conception", String.valueOf(secondDamGeneration + 1),
                String.valueOf(births.getFieldValue(1, "Id/demographics/generation")));

        log("Verifying nothing else on the row was touched");
        assertEquals("Animal Id should have been left alone", bornAnimal, births.getFieldValue(1, "Id"));
        assertEquals("Birth date should have been left alone", dateBefore, births.getFieldValue(1, "date"));
        assertEquals("Gender should have been left alone", genderBefore, births.getFieldValue(1, "Id/demographics/gender"));
        assertEquals("Social Code should have been left alone", socialCodeBefore, births.getFieldValue(1, "Id/demographics/socialCode"));
        assertEquals("Pregnancy Type should have been left alone", pregnancyTypeBefore, births.getFieldValue(1, "pregnancyType"));
        assertEquals("Group should have been left alone", groupBefore, births.getFieldValue(1, "groupId"));
        assertEquals("Project should have been left alone", "795644", String.valueOf(births.getFieldValue(1, "project")));
        assertEquals("Protocol should have been left alone", "protocol101", births.getFieldValue(1, "birthProtocol"));

        submitForm("Submit Final", "Finalize");

        log("Verifying the saved birth carries the picked conception");
        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("study", "birth");
        table.setFilter("Id", "Equals", bornAnimal);
        Assert.assertEquals("Saved birth does not carry the picked conception",
                Arrays.asList(secondConcept), table.getRowDataAsText(0, "conceptId"));

        goToSchemaBrowser();
        table = viewQueryData("study", "demographics");
        table.setFilter("Id", "Equals", bornAnimal);
        Assert.assertEquals("Demographics does not carry the dam of the picked conception",
                Arrays.asList(secondDam), table.getRowDataAsText(0, "dam"));
        Assert.assertEquals("Demographics does not carry the sire of the picked conception",
                Arrays.asList(secondSire), table.getRowDataAsText(0, "sire"));
    }

    @Test
    public void testPregnancyForm() throws IOException, CommandException
    {
        String animalId = "TEST4551032";
        String conceptId = "TESTCONCEPT2";
        // a non-live outcome, so ConceptionsByDam reports it rather than falling through to 'Live Birth'
        String result = "Fetal Death";
        String deliveryMode = "Vaginal";
        String pregnancyType = "Research Intervention";
        LocalDateTime now = LocalDateTime.now();

        log("Creating conception record");
        InsertRowsCommand conception = new InsertRowsCommand("study", "conception");
        conception.addRow(Map.of("conceptId", conceptId, "date", now.minusDays(90), "Id", animalId, "QCStateLabel", "Completed", "performedby", 1004));
        conception.execute(getApiHelper().getConnection(), getContainerPath());

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Pregnancy Outcomes"));
        lockForm();

        Ext4GridRef outcomes = _helper.getExt4GridForFormSection("Pregnancy Outcomes");
        _helper.addRecordToGrid(outcomes);
        outcomes.setGridCellJS(1, "date", now.minusDays(1).format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_STRING)));
        outcomes.setGridCell(1, "Id", animalId);
        outcomes.setGridCell(1, "result", result);
        outcomes.setGridCell(1, "conceptId", conceptId);
        outcomes.setGridCell(1, "type", deliveryMode);
        outcomes.setGridCell(1, "pregnancyType", pregnancyType);
        submitForm("Submit Final", "Finalize");

        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("study", "pregnancy");
        table.setFilter("Id", "Equals", animalId);
        Assert.assertEquals("Invalid Pregnancy Outcome record", Arrays.asList(animalId), table.getRowDataAsText(0, "Id"));
        Assert.assertEquals("Invalid Pregnancy Outcome record", Arrays.asList(result), table.getRowDataAsText(0, "result"));
        Assert.assertEquals("Invalid Pregnancy Outcome record", Arrays.asList(conceptId), table.getRowDataAsText(0, "conceptId"));
        Assert.assertEquals("Invalid Pregnancy Outcome record", Arrays.asList(deliveryMode), table.getRowDataAsText(0, "type"));
        Assert.assertEquals("Invalid Pregnancy Outcome record", Arrays.asList(pregnancyType), table.getRowDataAsText(0, "pregnancyType"));

        log("Verifying conception outcome in ConceptionsByDam");
        goToSchemaBrowser();
        DataRegionTable report = viewQueryData("study", "ConceptionsByDam");
        report.setFilter("conceptId", "Equals", conceptId);
        Assert.assertEquals("Invalid ConceptionsByDam row", Arrays.asList(animalId), report.getRowDataAsText(0, "Id"));
        Assert.assertEquals("Invalid ConceptionsByDam row", Arrays.asList(result), report.getRowDataAsText(0, "conceptionOutcome"));
        Assert.assertEquals("A conception claimed by a pregnancy outcome should not be active",
                Arrays.asList("false"), report.getRowDataAsText(0, "isActive"));

        log("Verifying the pregnancy outcome cleared the dam's pregnancy");
        // this dam carries other conceptions from sibling tests, so assert only that this one is gone
        Assert.assertFalse("Animal Details still reports a conception a pregnancy outcome has closed",
                getSnapshotFieldValue(animalId, "Pregnant").contains(conceptId));
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
        Assert.assertFalse("Breeding Type describes the birth and should no longer appear on the Conception form",
                conceptions.isColumnPresent("breedingType", false));

        _helper.addRecordToGrid(conceptions);
        conceptions.setGridCell(1, "conceptId", conceptId);
        conceptions.setGridCellJS(1, "date", now.minusDays(30).format(_dateFormat));
        conceptions.setGridCellJS(1, "estimated", true);
        conceptions.setGridCell(1, "Id", damId);
        conceptions.setGridCell(1, "sire", sireId);
        // Remark renders as a textarea, which Ext4GridRef's cell editor helpers cannot drive: they only recognize
        // an <input> as the active editor, so the click that opens the textarea is followed by a retry click that
        // the open textarea intercepts. Set it through the store instead.
        conceptions.setGridCellJS(1, "remark", "Conception entry test");
        submitForm("Submit Final", "Finalize");

        goToSchemaBrowser();
        DataRegionTable table = viewQueryData("study", "conception");
        table.setFilter("conceptId", "Equals", conceptId);
        Assert.assertEquals("Invalid Conception record", Arrays.asList(damId), table.getRowDataAsText(0, "Id"));
        Assert.assertEquals("Invalid Conception record", Arrays.asList(sireId), table.getRowDataAsText(0, "sire"));
        Assert.assertEquals("Invalid Conception record", Arrays.asList("true"), table.getRowDataAsText(0, "estimated"));
        Assert.assertEquals("Invalid Conception record", Arrays.asList("Conception entry test"), table.getRowDataAsText(0, "remark"));

        log("Verifying unmatched conception appears as Unknown in ConceptionsByDam");
        goToSchemaBrowser();
        DataRegionTable report = viewQueryData("study", "ConceptionsByDam");
        report.setFilter("conceptId", "Equals", conceptId);
        Assert.assertEquals("Invalid ConceptionsByDam row", Arrays.asList(damId), report.getRowDataAsText(0, "Id"));
        Assert.assertEquals("Invalid ConceptionsByDam row", Arrays.asList("Unknown"), report.getRowDataAsText(0, "conceptionOutcome"));
        Assert.assertEquals("A conception with no birth or pregnancy outcome should be active",
                Arrays.asList("true"), report.getRowDataAsText(0, "isActive"));

        log("Verifying the dam's Animal Details links to the open conception");
        // this dam carries other conceptions from sibling tests, so assert only that this one is listed
        Assert.assertTrue("Animal Details did not report the open conception",
                getSnapshotFieldValue(damId, "Pregnant").contains(conceptId));
    }

    /**
     * Reads one field from the Animal Details snapshot panel, which renders Ext4 displayfields rather than a grid, so
     * there is no page object to read through. The value arrives from the demographics cache after the page settles,
     * so an empty field means not-yet-loaded rather than no value.
     */
    private String getSnapshotFieldValue(String animalId, String fieldLabel)
    {
        ParticipantViewPage.beginAt(this, animalId);
        Locator field = Locator.xpath("//*[contains(@class,'x4-form-item')][.//label[starts-with(normalize-space(.),'"
                + fieldLabel + "')]]//div[contains(@class,'x4-form-display-field')]");
        waitForElement(field);
        waitFor(() -> !field.findElement(getDriver()).getText().trim().isEmpty(),
                "Animal Details did not populate the " + fieldLabel + " field", WAIT_FOR_JAVASCRIPT);

        return field.findElement(getDriver()).getText().trim();
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
        Map<String, String> obsValues = new HashMap<>(NBRI_DAILY_OBS_VALUES);
        obsValues.put("Lameness", "Lame on left leg");
        enterScheduledObservations(obsValues);

        table = new AnimalHistoryPage<>(getDriver()).getActiveReportDataRegion();
        Assert.assertEquals("Status is not updated", "Completed", table.getDataAsText(0, "observationStatus"));

        // This animal has a single clinical case, so every scheduled observation belongs to that one
        // order group and stays on the form's task: one task group, and no empty task created.
        verifyScheduledObservationTaskGrouping(animalId, obsTasksBefore, 1);

        log("Verifying Animal Details shows the most recent BCS");
        // the BCS combo forces selection, so an unmatched value saves as null rather than failing the form
        List<String> savedBcs = getClinicalObservations(animalId).stream()
                .filter(row -> "BCS".equals(row.get("category")))
                .map(row -> String.valueOf(row.get("observation")))
                .toList();
        Assert.assertEquals("BCS observation was not saved", List.of("Lean (2.5)"), savedBcs);
        String bcs = getSnapshotFieldValue(animalId, "BCS");
        Assert.assertTrue("Animal Details shows the wrong BCS: " + bcs, bcs.startsWith("Lean (2.5) ("));

        log("Closing the case");
        goToEHRFolder();
        waitAndClickAndWait(Locator.linkWithText("Active Clinical Cases"));
        DataRegionTable activeClinicalCases = new AnimalHistoryPage<>(getDriver()).getActiveReportDataRegion();
        activeClinicalCases.link(0, "caseCheck").click();
        switchToWindow(2);

        waitForText(animalId);
        waitForValidationToClear("Id is required");
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
    public void testAnimalHistoryLocationSearch() throws IOException, CommandException
    {
        List<String> locations = List.of(CAGE_IN_R1, CAGE_IN_R2);

        // Sibling tests add cages and house animals, so read the expected values rather than hard-coding them.
        SelectRowsCommand cageSelect = new SelectRowsCommand("ehr_lookups", "cage");
        cageSelect.setColumns(List.of("location"));
        List<String> expectedLocations = cageSelect.execute(getApiHelper().getConnection(), getContainerPath()).getRows().stream()
                .map(row -> (String) row.get("location"))
                .sorted()
                .toList();

        SelectRowsCommand housingSelect = new SelectRowsCommand("study", "housing");
        housingSelect.setColumns(List.of("Id"));
        housingSelect.addFilter(new Filter("cage", String.join(";", locations), Filter.Operator.IN));
        housingSelect.addFilter(new Filter("isActive", true));
        List<String> expectedIds = housingSelect.execute(getApiHelper().getConnection(), getContainerPath()).getRows().stream()
                .map(row -> (String) row.get("Id"))
                .distinct()
                .sorted()
                .toList();
        Assert.assertFalse("No animals currently housed in " + locations, expectedIds.isEmpty());

        AnimalHistoryPage<?> animalHistory = AnimalHistoryPage.beginAt(this, getContainerPath());
        animalHistory.selectCurrectLocation();
        ComboBox cagePicker = ComboBox(getDriver()).withIdPrefix("nbri_ehr-locationField").find(getDriver())
                .setMatcher(Ext4Helper.TextMatchTechnique.LEADING_NBSP);

        log("Verifying the cage dropdown lists room-qualified locations");
        // Check-combo items are prefixed with a non-breaking space.
        List<String> options = cagePicker.getComboBoxOptions().stream()
                .map(option -> option.replace(Locator.NBSP, " ").strip())
                .filter(option -> !"All".equals(option))
                .sorted()
                .toList();
        assertEquals("Cage dropdown should list each cage's location", expectedLocations, options);

        cagePicker.selectComboBoxItem(locations.toArray(new String[0]));
        animalHistory.refreshReport();

        log("Verifying the report is limited to animals housed in the chosen locations");
        animalHistory.clickCategoryTab("Colony Management").clickReportTab("Housing - Active");
        List<String> reportIds = animalHistory.getActiveReportDataRegion().getColumnDataAsText("Id").stream()
                .distinct()
                .sorted()
                .toList();
        assertEquals("Location search returned the wrong animals", expectedIds, reportIds);
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

    @Test
    public void testAddBatchByLocation() throws IOException, CommandException
    {
        // Sibling tests add cages and house animals, so read the expected values rather than hard-coding them.
        SelectRowsCommand cageSelect = new SelectRowsCommand("ehr_lookups", "cage");
        cageSelect.setColumns(List.of("location"));
        List<String> expectedLocations = cageSelect.execute(getApiHelper().getConnection(), getContainerPath()).getRows().stream()
                .map(row -> (String) row.get("location"))
                .sorted()
                .toList();

        SelectRowsCommand housingSelect = new SelectRowsCommand("study", "housing");
        housingSelect.setColumns(List.of("Id"));
        housingSelect.addFilter(new Filter("cage", CAGE_IN_R2));
        housingSelect.addFilter(new Filter("isActive", true));
        List<String> expectedIds = housingSelect.execute(getApiHelper().getConnection(), getContainerPath()).getRows().stream()
                .map(row -> (String) row.get("Id"))
                .sorted()
                .toList();
        Assert.assertFalse("No animals currently housed in " + CAGE_IN_R2, expectedIds.isEmpty());

        Ext4GridRef weights = openBulkClinicalWeights();
        Window<?> chooseAnimals = openAddBatch(weights, "Location");

        log("Verifying the cage picker lists room-qualified locations");
        Locator.XPathLocator cagePicker = Ext4Helper.Locators.formItemWithLabelContaining("Cage(s)");
        // Check-combo items are prefixed with a non-breaking space.
        List<String> options = _ext4Helper.getComboBoxOptions(cagePicker).stream()
                .map(option -> option.replace(Locator.NBSP, " ").strip())
                .filter(option -> !"All".equals(option))
                .sorted()
                .toList();
        assertEquals("Cage picker should list each cage's location", expectedLocations, options);

        _ext4Helper.selectItemFromOpenComboList(CAGE_IN_R2, Ext4Helper.TextMatchTechnique.CONTAINS);
        _ext4Helper.closeComboList(cagePicker);
        chooseAnimals.clickButton("Submit", true);

        log("Verifying the animals housed in the chosen location were added");
        assertEquals("Add Batch by location added the wrong animals", expectedIds, getGridIds(weights, expectedIds.size()));
    }

    @Test
    public void testAddBatchByAnimalGroup() throws Exception
    {
        String group = "T";
        String groupTitle = "Time-Mated";
        String currentMember1 = SUBJECTS[0];
        String currentMember2 = SUBJECTS[1];
        String otherGroupMember = SUBJECTS[2];
        String formerMember = SUBJECTS[3];

        Date pastDate = TIME_FORMAT.parse("2012-01-03 09:30");
        Date endDate = TIME_FORMAT.parse("2012-05-03 19:20");
        String[] fields = {"Id", "date", "enddate", "groupId", "performedby"};
        Object[][] data = {
                {currentMember1, pastDate, null, group, 1004},
                {currentMember2, pastDate, null, group, 1004},
                {otherGroupMember, pastDate, null, "C", 1004},
                {formerMember, pastDate, endDate, group, 1004}
        };
        SimplePostCommand insertCommand = getApiHelper().prepareInsertCommand("study", "animal_group_members", "lsid", fields, data);
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());

        SelectRowsCommand groupTypeSelect = new SelectRowsCommand("ehr_lookups", "breeding_type");
        groupTypeSelect.setColumns(List.of("title"));
        List<String> expectedTitles = groupTypeSelect.execute(getApiHelper().getConnection(), getContainerPath()).getRows().stream()
                .map(row -> (String) row.get("title"))
                .sorted()
                .toList();

        // Sibling tests also assign groups, so read the current members rather than hard-coding them.
        SelectRowsCommand memberSelect = new SelectRowsCommand("study", "animal_group_members");
        memberSelect.setColumns(List.of("Id"));
        memberSelect.addFilter(new Filter("groupId", group));
        memberSelect.addFilter(new Filter("isActive", true));
        List<String> expectedIds = memberSelect.execute(getApiHelper().getConnection(), getContainerPath()).getRows().stream()
                .map(row -> (String) row.get("Id"))
                .distinct()
                .sorted()
                .toList();
        Assert.assertTrue("Current members missing from " + group + ": " + expectedIds, expectedIds.containsAll(List.of(currentMember1, currentMember2)));
        Assert.assertFalse("Member of another group returned for " + group, expectedIds.contains(otherGroupMember));
        Assert.assertFalse("Former member returned for " + group, expectedIds.contains(formerMember));

        Ext4GridRef weights = openBulkClinicalWeights();
        Window<?> chooseAnimals = openAddBatch(weights, "Animal Group");

        log("Verifying the group picker lists breeding types");
        // Its label matches the radio's, so locate the picker by component id.
        Ext4CmpRef groupField = _ext4Helper.queryOne("window[title='Choose Animals'] #groupField", Ext4CmpRef.class);
        Locator.XPathLocator groupPicker = Locator.tagWithId("table", groupField.getId());
        List<String> options = _ext4Helper.getComboBoxOptions(groupPicker).stream()
                .sorted()
                .toList();
        assertEquals("Group picker should list each breeding type", expectedTitles, options);

        _ext4Helper.selectItemFromOpenComboList(groupTitle, Ext4Helper.TextMatchTechnique.EXACT);
        _ext4Helper.closeComboList(groupPicker);
        chooseAnimals.clickButton("Submit", true);

        log("Verifying the current members of the chosen group were added");
        assertEquals("Add Batch by animal group added the wrong animals", expectedIds, getGridIds(weights, expectedIds.size()));
    }

    private Ext4GridRef openBulkClinicalWeights()
    {
        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Bulk Clinical Entry"));
        return getExpandedFormSectionGrid("Weights");
    }

    private Ext4GridRef getExpandedFormSectionGrid(String sectionTitle)
    {
        // The form is wider than the window, so the collapsed section's expand tool is off-screen and can't be clicked.
        String query = "panel[title='" + sectionTitle + "']";
        Ext4CmpRef.waitForComponent(this, query);
        _ext4Helper.queryOne(query, Ext4CmpRef.class).eval("expand(false)");
        return _helper.getExt4GridForFormSection(sectionTitle);
    }

    private Window<?> openAddBatch(Ext4GridRef grid, String type)
    {
        grid.clickTbarButton("Add Batch");
        Window<?> chooseAnimals = new Window<>("Choose Animals", getDriver());
        RadioButton().withLabel(type).find(chooseAnimals).check();
        return chooseAnimals;
    }

    private List<String> getGridIds(Ext4GridRef grid, int expectedCount)
    {
        grid.waitForRowCount(expectedCount);
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= grid.getRowCount(); i++)
            ids.add((String) grid.getFieldValue(i, "Id"));
        Collections.sort(ids);
        return ids;
    }

    // The ehr.tasks formtype for the clinical observations data entry form (NBRIClinicalObservationsFormType.NAME).
    private static final String NBRI_OBSERVATIONS_FORM_TYPE = "Observations";

    // Valid Observation/Score values keyed by daily clinical observation category. The Observation/Score
    // field is category-dependent, so each value must be legal for its category.
    private static final Map<String, String> NBRI_DAILY_OBS_VALUES = Map.of(
            "Activity", "0-1 Extremely Lethargic",
            "Appetite", "Normal to low",
            "BCS", "Lean (2.5)",
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
        enterScheduledObservations(NBRI_DAILY_OBS_VALUES);

        // Each of the six daily categories matched two orders, so two entries per category were created,
        // grouped into exactly two tasks (one per originating order taskid) with no empty task.
        verifyScheduledObservationTaskGrouping(animalId, obsTasksBefore, 2);

        Map<String, Integer> entriesPerCategory = new HashMap<>();
        for (Map<String, Object> row : getClinicalObservations(animalId))
        {
            entriesPerCategory.merge(String.valueOf(row.get("category")), 1, Integer::sum);
            // A scheduled observation takes its type from the originating order, which the daily clinical
            // observation orders create as Clinical.
            Assert.assertEquals("Scheduled observation for category " + row.get("category") + " should be Clinical",
                    "Clinical", String.valueOf(row.get("type")));
        }
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
    private void enterScheduledObservations(Map<String, String> valuesByCategory)
    {
        Ext4GridRef observation = _helper.getExt4GridForFormSection("Observations");
        int rowCount = observation.getRowCount();
        for (int row = 1; row <= rowCount; row++)
        {
            String category = String.valueOf(observation.getFieldValue(row, "category"));
            String value = valuesByCategory.get(category);
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

    // Two ehr.observation_types values on either side of the derivation: the first has no category, the second
    // is categorized as Behavior. Both use a free-text Observation/Score editor, so neither depends on an
    // ehr_lookups value list being populated.
    private static final String UNCATEGORIZED_OBS_TYPE = "Mass";
    private static final String BEHAVIOR_OBS_TYPE = "General Behavior Observation";

    @Test
    public void testObservationTypeDerivedFromCategory()
    {
        String animalId = obsTypeAnimalId;

        // The Observations form offers every observation type, so it cannot set the observation's type up
        // front; the trigger script derives it from the selected type's category. A type categorized as
        // Behavior must be stored as a Behavior observation and everything else as Clinical, otherwise the
        // entry drops out of the behavior views (study.behaviorObservations filters on type = 'Behavior').
        log("Entering an uncategorized and a Behavior-categorized observation type on the Observations form");
        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Observations"));

        Ext4GridRef observations = _helper.getExt4GridForFormSection("Observations");
        addObservationRow(observations, animalId, UNCATEGORIZED_OBS_TYPE, "3 cm mass on left arm");
        addObservationRow(observations, animalId, BEHAVIOR_OBS_TYPE, "Pacing observed");
        submitForm("Submit Final", "Finalize");

        Map<String, String> typeByCategory = new HashMap<>();
        for (Map<String, Object> row : getClinicalObservations(animalId))
            typeByCategory.put(String.valueOf(row.get("category")), String.valueOf(row.get("type")));

        Assert.assertEquals("Expected exactly the two entered observations for " + animalId,
                Set.of(UNCATEGORIZED_OBS_TYPE, BEHAVIOR_OBS_TYPE), typeByCategory.keySet());
        Assert.assertEquals("An uncategorized observation type should be stored as a Clinical observation",
                "Clinical", typeByCategory.get(UNCATEGORIZED_OBS_TYPE));
        Assert.assertEquals("A Behavior-categorized observation type should be stored as a Behavior observation",
                "Behavior", typeByCategory.get(BEHAVIOR_OBS_TYPE));
    }

    // Appends a row to an Observations grid and fills in the fields the trigger script needs to accept it: an
    // animal, an observation type (the grid's "category"), and an Observation/Score plus remark, since an entry
    // with neither raises a WARN that would disable Submit Final. The row index is read back from the grid
    // rather than assumed, so this works whether or not the form starts with rows of its own.
    private void addObservationRow(Ext4GridRef observations, String animalId, String category, String observation)
    {
        _helper.addRecordToGrid(observations);
        int row = observations.getRowCount();
        observations.setGridCell(row, "Id", animalId);
        observations.setGridCell(row, "category", category);
        observations.setGridCell(row, "observation", observation);
        observations.setGridCellJS(row, "remark", "remark for " + category);
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
        //create animal care basic submitter user
        createUser(NBRI_BASIC_SUBMITTER, "EHR Basic Submitters", null);

        //create a vet tech user with 'EHR Basic Submitters' group
        createUser(NBRI_BASIC_SUBMITTER_VET_TECH, "EHR Basic Submitters", NBRI_VET_TECH_ROLE);

        //create a vet tech user with 'EHR Full Updaters' group (ex. this user can 'Submit for Review' and 'Submit Final' in cases)
        createUser(NBRI_FULL_SUBMITTER_VET_TECH, "EHR Full Updaters", NBRI_VET_TECH_ROLE);

        //create a vet user with 'EHR Full Updaters' group (ex. this user can 'Submit Final' and request a necropsy)
        createUser(NBRI_FULL_SUBMITTER_VET, "EHR Full Updaters", "EHR Veterinarian");

        //create a pathologist, who enters the preliminary and final necropsy
        createUser(NBRI_PATHOLOGIST, "EHR Full Updaters", "EHRPathologyEntryRole");

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
                        {aliveAnimalId, LocalDateTime.now().minusDays(30), getFemale(), "Completed", 1004},
                        {deadAnimalId, LocalDateTime.now().minusDays(30), getMale(), "Completed", 1004},
                        {departedAnimalId, LocalDateTime.now().minusDays(30), getMale(), "Completed", 1004},
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
        housing.addRow(Map.of("Id", aliveAnimalId, "date", LocalDateTime.now().minusDays(10), "room", getRooms()[2], "cage", CAGE_IN_R3, "QCStateLabel", "Completed", "performedby", 1004));
        housing.execute(getApiHelper().getConnection(), getContainerPath());

        log("Marking an animal dead");
        InsertRowsCommand deaths = new InsertRowsCommand("study", "deaths");
        deaths.addRow(Map.of("Id", deadAnimalId, "date", LocalDateTime.now().minusDays(10), "performedby", 1004));
        deaths.execute(getApiHelper().getConnection(), getContainerPath());

        log("Marking an animal departed");
        InsertRowsCommand departure = new InsertRowsCommand("study", "departure");
        // destination stores an ehr_lookups.source code; the facility name is only the display value
        departure.addRow(Map.of("Id", departedAnimalId, "date", LocalDateTime.now().minusDays(1), "destination", "ORPRC", "performedby", 1004));
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
    public void testDeathAndNecropsyWorkflow() throws IOException, CommandException
    {
        enableNotification(DEATH_NOTIFICATION);
        enableNotification(NECROPSY_REQUEST_NOTIFICATION);
        createSubjectsForDeathForm();

        log("Verify the deaths trigger rejects animals that cannot die");
        assertDeathRejected(departedAnimalId, "ERROR: Animal is not at the center.");
        assertDeathRejected(unknownAnimalId, "ERROR: Id not found in the demographics table.");
        assertDeathRejected(deadAnimalId, "ERROR: Death record already exists for this animal.");

        log("Recording the death");
        InsertRowsCommand death = new InsertRowsCommand("study", "deaths");
        death.addRow(Map.of("Id", aliveAnimalId, "date", LocalDateTime.now(), "type", "D", "deathWeight", 23,
                "QCStateLabel", "Completed", "performedby", 1004));
        death.execute(getApiHelper().getConnection(), getContainerPath());

        log("Verify a second death insert is rejected with a validation error, not a unique constraint violation");
        assertDeathRejected(aliveAnimalId, "ERROR: Death record already exists for this animal.");

        log("Verify the death notification shows the records the death closed and offers a necropsy request");
        goToModule("Dumbster");
        EmailRecordTable notifications = new EmailRecordTable(this);
        waitForTextWithRefresh(WAIT_FOR_PAGE + WAIT_FOR_JAVASCRIPT, "Death Notification: " + aliveAnimalId);
        notifications.clickMessage(notifications.getMessageWithSubjectContaining("Death Notification: " + aliveAnimalId));
        String deathBody = notifications.getMessageWithSubjectContaining("Death Notification: " + aliveAnimalId).getBody();
        Assert.assertFalse("Death notification did not find the final housing: " + deathBody, deathBody.contains("Not Found"));
        Assert.assertTrue("Death notification is missing the final location: " + deathBody, deathBody.contains(CAGE_IN_R3));
        Assert.assertTrue("Death notification is missing the final project: " + deathBody, deathBody.contains(PROJECT_ID));
        Assert.assertTrue("Death notification is missing the final protocol: " + deathBody, deathBody.contains(PROTOCOL_ID + " - " + INVES_LAST_NAME));
        String requestUrl = Locator.linkWithText("Click here to request a Necropsy").findElement(notifications).getAttribute("href");

        log("Requesting the necropsy");
        impersonate(NBRI_FULL_SUBMITTER_VET);
        beginAt(requestUrl);
        waitForElement(Ext4Helper.Locators.ext4Button("Submit Request"), WAIT_FOR_PAGE);
        enterNecropsyRequest(aliveAnimalId);
        submitNecropsyStage("Submit Request");
        stopImpersonating();

        String taskid = getNecropsyTaskValue(aliveAnimalId, "taskid");
        assertEquals("Wrong necropsy status after the request", "Request Pending", getNecropsyTaskValue(aliveAnimalId, "status"));
        assertEquals("A requested necropsy should be pending", "Necropsy Pending", getNecropsyStatus(aliveAnimalId));

        log("Verify the request notification links to the preliminary stage");
        goToModule("Dumbster");
        waitForTextWithRefresh(WAIT_FOR_PAGE + WAIT_FOR_JAVASCRIPT, "Necropsy Request: " + aliveAnimalId);
        notifications = new EmailRecordTable(this);
        notifications.clickMessage(notifications.getMessageWithSubjectContaining("Necropsy Request: " + aliveAnimalId));
        String requestBody = notifications.getMessageWithSubjectContaining("Necropsy Request: " + aliveAnimalId).getBody();
        Assert.assertTrue("Necropsy notification is missing the final location: " + requestBody, requestBody.contains(CAGE_IN_R3));
        Assert.assertTrue("Necropsy notification is missing the final protocol: " + requestBody, requestBody.contains(PROTOCOL_ID + " - " + INVES_LAST_NAME));
        String preliminaryUrl = Locator.linkWithText("Click here to enter preliminary necropsy results").findElement(notifications).getAttribute("href");

        log("Entering the preliminary necropsy");
        impersonate(NBRI_PATHOLOGIST);
        beginAt(preliminaryUrl);
        waitForElement(Ext4Helper.Locators.ext4Button("Submit Preliminary Results"), WAIT_FOR_PAGE);
        _helper.getExt4FieldForFormSection("Necropsy", "Preliminary Cause of Death").setValue("Pneumonia");
        _helper.getExt4FieldForFormSection("Gross Description", "Gross Description").setValue("Consolidated right lung");

        Ext4GridRef tissueDisposition = _helper.getExt4GridForFormSection("Tissue Disposition");
        _helper.addRecordToGrid(tissueDisposition);
        tissueDisposition.setGridCell(1, "necropsyDispositionCode", "Frozen");
        tissueDisposition.setGridCell(1, "necropsyTissue", "Pancreas");

        Ext4GridRef preliminaryDiagnoses = _helper.getExt4GridForFormSection("Gross Diagnoses");
        _helper.addRecordToGrid(preliminaryDiagnoses);
        preliminaryDiagnoses.setGridCell(1, "tissueSample", "LUNG, NOS");
        preliminaryDiagnoses.setGridCell(1, "diagnosis", "Inflammation");
        preliminaryDiagnoses.setGridCell(1, "severity", "Marked");
        preliminaryDiagnoses.setGridCell(1, "distribution", "Diffuse");
        _helper.getExt4FieldForFormSection("Necropsy", "Accession Number").setValue(ACCESSION_NUMBER);
        setFormSectionFieldUntilWarningClears("Necropsy", "Accession Number", ACCESSION_NUMBER, "Accession number is required");
        submitNecropsyStage("Submit Preliminary Results");
        stopImpersonating();
        goToEHRFolder();

        assertEquals("Wrong necropsy status after the preliminary results", "Preliminary Results Submitted", getNecropsyTaskValue(aliveAnimalId, "status"));
        assertEquals("The necropsy record did not carry the status", "Preliminary Results Submitted",
                getSingleRowForAnimal("necropsy", aliveAnimalId, List.of("Id", "necropsyStatus")).get("necropsyStatus"));
        verifyRowCreated("study", "necropsy", aliveAnimalId, 1);
        verifyRowCreated("study", "grossPathology", aliveAnimalId, 1);
        verifyRowCreated("study", "tissueDisposition", aliveAnimalId, 1);
        verifyRowCreated("study", "preliminaryDiagnoses", aliveAnimalId, 1);
        assertEquals("The accession number did not become the animal's necropsy alias", ACCESSION_NUMBER, getNecropsyAlias(aliveAnimalId));

        log("Entering the final necropsy");
        impersonate(NBRI_PATHOLOGIST);
        beginAt(WebTestHelper.buildURL("ehr", getContainerPath(), "dataEntryForm", Map.of("formType", "Necropsy Final", "taskid", taskid)));
        waitForElement(Ext4Helper.Locators.ext4Button("Submit Final Results"), WAIT_FOR_PAGE);
        _helper.getExt4FieldForFormSection("Final Results", "Cause of Death Category").setValue("Respiratory");
        _helper.getExt4FieldForFormSection("Final Results", "Cause of Death").setValue("Bacterial pneumonia");

        Ext4GridRef finalDiagnoses = _helper.getExt4GridForFormSection("Final Diagnoses");
        assertEquals("The final diagnoses did not start from the preliminary diagnoses", 1, finalDiagnoses.getRowCount());
        assertEquals("Wrong copied diagnosis", "Inflammation", finalDiagnoses.getFieldValue(1, "diagnosis"));
        finalDiagnoses.setGridCell(1, "severity", "Severe");

        Ext4GridRef histology = _helper.getExt4GridForFormSection("Histology");
        _helper.addRecordToGrid(histology);
        histology.setGridCell(1, "slideNo", "S1");
        histology.setGridCell(1, "stain", "Gram Stain");
        setFormSectionFieldUntilWarningClears("Final Results", "Cause of Death Category", "Respiratory", "Cause of death category is required");
        submitNecropsyStage("Save Draft");
        stopImpersonating();

        log("Verify a final draft leaves the earlier stages submitted");
        assertEquals("A final draft changed the necropsy task's QC state", "Completed", getNecropsyTaskValue(aliveAnimalId, "QCState/Label"));
        for (String earlierStage : List.of("necropsyRequest", "necropsy", "preliminaryDiagnoses"))
        {
            assertEquals("A final draft changed the QC state of study." + earlierStage, "Completed",
                    getSingleRowForAnimal(earlierStage, aliveAnimalId, List.of("Id", "QCState/Label")).get("QCState/Label"));
        }

        impersonate(NBRI_PATHOLOGIST);
        beginAt(WebTestHelper.buildURL("ehr", getContainerPath(), "dataEntryForm", Map.of("formType", "Necropsy Final", "taskid", taskid)));
        waitForElement(Ext4Helper.Locators.ext4Button("Submit Final Results"), WAIT_FOR_PAGE);
        waitForFormValidationToClear();
        click(Ext4Helper.Locators.ext4Button("Submit Final Results"));
        new Window.WindowFinder(getDriver()).withTitle("Submit Final Necropsy").waitFor().clickButton("Submit Final");
        stopImpersonating();
        goToEHRFolder();

        assertEquals("Wrong necropsy status after the final results", "Final Results Submitted", getNecropsyTaskValue(aliveAnimalId, "status"));
        assertEquals("The request record does not show the task's status", "Final Results Submitted",
                getSingleRowForAnimal("necropsyRequest", aliveAnimalId, List.of("Id", "necropsyStatus")).get("necropsyStatus"));
        assertEquals("Final submission changed the request's QC state", "Completed",
                getSingleRowForAnimal("necropsyRequest", aliveAnimalId, List.of("Id", "QCState/Label")).get("QCState/Label"));
        verifyRowCreated("study", "necropsyFinal", aliveAnimalId, 1);
        verifyRowCreated("study", "histology", aliveAnimalId, 1);
        verifyRowCreated("study", "finalDiagnoses", aliveAnimalId, 1);
        assertEquals("Wrong cause of death category", "Respiratory",
                getSingleRowForAnimal("necropsyFinal", aliveAnimalId, List.of("Id", "causeCategory")).get("causeCategory"));
        assertEquals("The preliminary diagnosis changed when the final copy was edited", "Marked",
                getSingleRowForAnimal("preliminaryDiagnoses", aliveAnimalId, List.of("Id", "severity")).get("severity"));
        assertEquals("Wrong final severity", "Severe",
                getSingleRowForAnimal("finalDiagnoses", aliveAnimalId, List.of("Id", "severity")).get("severity"));
        assertEquals("A completed necropsy should leave the animal Dead", "Dead", getNecropsyStatus(aliveAnimalId));

        log("Verify a task cannot be moved back to an earlier necropsy stage");
        UpdateRowsCommand regress = new UpdateRowsCommand("ehr", "tasks");
        regress.addRow(Map.of("taskid", taskid, "formtype", "Necropsy Request"));
        regress.execute(getApiHelper().getConnection(), getContainerPath());
        assertEquals("The task moved back to an earlier stage", "Necropsy Final", getNecropsyTaskValue(aliveAnimalId, "taskid/formtype"));

        log("Verify the death closed out the animal's records");
        verifyRowCreated("study", "weight", aliveAnimalId, 1);
        String deathDay = getDatasetDay("deaths", aliveAnimalId, "date");
        Assert.assertNotNull("Death record has no date", deathDay);
        assertEquals("Demographics death date does not match the death record", deathDay, getDatasetDay("demographics", aliveAnimalId, "death"));
        assertEquals("Animal should be Dead once the death is recorded", "Dead", getCalculatedStatus(aliveAnimalId));
        for (String dataset : List.of("housing", "assignment", "protocolAssignment"))
        {
            assertEquals("End date is not updated for study." + dataset, LocalDateTime.now().format(_dateFormat),
                    getDatasetDay(dataset, aliveAnimalId, "enddate"));
        }
    }

    @Test
    public void testFinalRecordsIgnoreDrafts() throws Exception
    {
        String animalId = FINAL_RECORDS_ANIMAL;
        createAliveAnimals(new String[]{animalId});

        log("Giving the animal completed housing and assignments, then newer drafts");
        insertHousingAndAssignments(animalId, LocalDateTime.now().minusDays(10), CAGE_IN_R1, PROJECT_ID, PROTOCOL_ID, EHRQCState.COMPLETED);
        insertHousingAndAssignments(animalId, LocalDateTime.now().minusDays(2), CAGE_IN_R3, PROTOCOL_PROJECT_ID, DUMMY_PROTOCOL, EHRQCState.IN_PROGRESS);

        Map<String, Object> lastHousing = getSingleRowForAnimal("demographicsLastHousing", animalId, List.of("Id", "cage", "Location"));
        assertEquals("Final housing should skip the draft", CAGE_IN_R1, lastHousing.get("cage"));
        assertEquals("Final location should be the cage's location key, not prefixed with the room again", CAGE_IN_R1, lastHousing.get("Location"));
        assertEquals("Final project should skip the draft", PROJECT_ID,
                String.valueOf(getSingleRowForAnimal("demographicsLastProject", animalId, List.of("Id", "project")).get("project")));
        assertEquals("Final protocol should skip the draft", PROTOCOL_ID,
                getSingleRowForAnimal("demographicsLastProtocol", animalId, List.of("Id", "protocol")).get("protocol"));
    }

    private void insertHousingAndAssignments(String animalId, LocalDateTime date, String cage, String project, String protocol, EHRQCState qcState) throws IOException, CommandException
    {
        InsertRowsCommand housing = new InsertRowsCommand("study", "housing");
        housing.addRow(Map.of("Id", animalId, "date", date, "cage", cage, "QCStateLabel", qcState.label, "performedby", 1004));
        housing.execute(getApiHelper().getConnection(), getContainerPath());

        InsertRowsCommand assignment = new InsertRowsCommand("study", "assignment");
        assignment.addRow(Map.of("Id", animalId, "date", date, "project", project, "QCStateLabel", qcState.label, "performedby", 1004));
        assignment.execute(getApiHelper().getConnection(), getContainerPath());

        InsertRowsCommand protocolAssignment = new InsertRowsCommand("study", "protocolAssignment");
        protocolAssignment.addRow(Map.of("Id", animalId, "date", date, "protocol", protocol, "QCStateLabel", qcState.label, "performedby", 1004));
        protocolAssignment.execute(getApiHelper().getConnection(), getContainerPath());
    }

    private void assertDeathRejected(String animalId, String expectedError)
    {
        SimplePostCommand insert = getApiHelper().prepareInsertCommand("study", "deaths", "lsid",
                new String[]{"Id", "date", "performedby"},
                new Object[][]{{animalId, LocalDateTime.now(), 1004}});
        CommandException error = getApiHelper().doSaveRowsExpectingError(DATA_ADMIN.getEmail(), insert, getExtraContext());
        Map<String, List<String>> errors = getApiHelper().extractErrors(error.getProperties());
        Assert.assertTrue("Expected '" + expectedError + "' for " + animalId + ", got: " + errors,
                errors.getOrDefault("Id", List.of()).contains(expectedError));
    }

    // The request form can rebuild its sections while it loads, discarding what was typed, so re-enter until it sticks
    private void enterNecropsyRequest(String animalId)
    {
        for (int attempt = 1; ; attempt++)
        {
            waitForFormValidationToSettle();
            setFormElement(Locator.name("Id"), animalId);
            _helper.getExt4FieldForFormSection("Necropsy Request", "Clinical Differential Diagnoses").setValue("Respiratory distress");
            _helper.getExt4FieldForFormSection("Request", "Submitting PI").setValue("Dr. Investigator");
            _ext4Helper.selectComboBoxItem("Submitting Vet:", Ext4Helper.TextMatchTechnique.CONTAINS, NBRI_VET_NAME);
            waitForFormValidationToSettle();

            boolean kept = animalId.equals(getFormElement(Locator.name("Id")))
                    && _helper.getExt4FieldForFormSection("Necropsy Request", "Clinical Differential Diagnoses").getValue() != null
                    && _helper.getExt4FieldForFormSection("Request", "Submitting Vet").getValue() != null;
            if (kept)
                return;

            Assert.assertTrue("The necropsy request form kept discarding its values", attempt < 3);
            log("Necropsy request values were discarded, re-entering");
        }
    }

    // the necropsy submit buttons save and navigate without a confirmation window
    private void submitNecropsyStage(String buttonText)
    {
        waitForFormValidationToClear();
        Locator button = Locator.linkWithText(buttonText);
        shortWait().until(ExpectedConditions.elementToBeClickable(button));
        clickAndWait(button.findElement(getDriver()));
    }

    private String getNecropsyTaskValue(String animalId, String column) throws IOException, CommandException
    {
        SelectRowsCommand select = new SelectRowsCommand("nbri_ehr", "necropsyTask");
        select.setColumns(List.of("taskid", "animalId/Id", column));
        select.addFilter(new Filter("animalId/Id", animalId));
        SelectRowsResponse response = select.execute(getApiHelper().getConnection(), getContainerPath());
        assertEquals("Expected one necropsy task for " + animalId, 1, response.getRows().size());
        return (String) response.getRows().getFirst().get(column);
    }

    private String getNecropsyStatus(String animalId) throws IOException, CommandException
    {
        return (String) getSingleRowForAnimal("necropsyStatus", animalId, List.of("Id", "necropsy_status")).get("necropsy_status");
    }

    private String getNecropsyAlias(String animalId) throws IOException, CommandException
    {
        SelectRowsCommand select = new SelectRowsCommand("study", "alias");
        select.setColumns(List.of("Id", "alias"));
        select.addFilter(new Filter("Id", animalId));
        select.addFilter(new Filter("category", "A"));
        SelectRowsResponse response = select.execute(getApiHelper().getConnection(), getContainerPath());
        assertEquals("Expected one necropsy alias for " + animalId, 1, response.getRows().size());
        return (String) response.getRows().getFirst().get("alias");
    }

    /**
     * Deleting a death record has to hand the status back to the shared recalc rather than assume the animal is alive:
     * an animal that also has a departure is Shipped, not Alive. Also covers demographics.death being cleared.
     */
    @Test
    public void testDeathDeleteRestoresDepartedStatus() throws Exception
    {
        String animalId = "DD9001";
        LocalDateTime now = LocalDateTime.now();

        log("Creating an animal via a birth record");
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), getApiHelper().prepareInsertCommand("study", "birth", "lsid",
                new String[]{"Id", "Date", "gender", "QCStateLabel", "performedby"},
                new Object[][]{{animalId, now.minusDays(30), getMale(), "Completed", 1004}}
        ), getExtraContext());

        // the death has to be recorded before the departure: the deaths trigger rejects an animal that has shipped
        log("Recording the death");
        InsertRowsCommand deaths = new InsertRowsCommand("study", "deaths");
        deaths.addRow(Map.of("Id", animalId, "date", now.minusDays(10), "QCStateLabel", "Completed", "performedby", 1004));
        deaths.execute(getApiHelper().getConnection(), getContainerPath());

        assertEquals("Demographics death date does not match the death record",
                now.minusDays(10).format(_dateFormat), getDatasetDay("demographics", animalId, "death"));
        assertEquals("Animal should be Dead while the death record exists", "Dead", getCalculatedStatus(animalId));

        log("Departing the animal, so the deleted death has a departure to fall back to");
        InsertRowsCommand departure = new InsertRowsCommand("study", "departure");
        departure.addRow(Map.of("Id", animalId, "date", now.minusDays(5), "destination", "ORPRC", "QCStateLabel", "Completed", "performedby", 1004));
        departure.execute(getApiHelper().getConnection(), getContainerPath());

        assertEquals("A death outranks a departure", "Dead", getCalculatedStatus(animalId));

        log("Deleting the death record");
        getApiHelper().deleteAllRecords("study", "deaths", new Filter("Id", animalId));

        Assert.assertNull("Demographics death date should be cleared when the death record is deleted",
                getDatasetDay("demographics", animalId, "death"));
        assertEquals("Deleting the death should fall back to the departure, not to Alive",
                "Shipped", getCalculatedStatus(animalId));
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
        waitForValidationToClear("Remark: WARN: Must enter at least one comment");

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
        waitForValidationToClear("Id is required");
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
        waitForValidationToClear("Id is required");
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
    public void testRoomKeyDerivation() throws Exception
    {
        log("Verifying a room derives its key from its building and name");
        SelectRowsCommand selectCmd = new SelectRowsCommand("ehr_lookups", "rooms");
        selectCmd.setColumns(List.of("room", "name", "building"));
        selectCmd.addFilter(new Filter("name", "R1"));
        SelectRowsResponse response = selectCmd.execute(getApiHelper().getConnection(), getContainerPath());

        Assert.assertEquals("Expected exactly one room named R1", 1, response.getRows().size());
        Map<String, Object> room = response.getRows().get(0);
        Assert.assertEquals("Room key should combine the building and the name", roomKey("R1"), room.get("room"));
        Assert.assertEquals("Room should belong to the seeded building", BUILDING_ID, room.get("building"));
    }

    @Test
    public void testRoomRequiresBuilding()
    {
        log("Verifying a room cannot be created without a building");
        InsertRowsCommand insertCmd = new InsertRowsCommand("ehr_lookups", "rooms");
        insertCmd.addRow(Map.of("name", "NOBUILDING"));

        try
        {
            insertCmd.execute(getApiHelper().getConnection(), getContainerPath());
            Assert.fail("Room insert should have been rejected when no building was supplied");
        }
        catch (IOException | CommandException e)
        {
            Assert.assertTrue("Unexpected failure inserting a room without a building: " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains("Building is required"));
        }
    }

    @Test
    public void testDuplicateBuildingRejected()
    {
        log("Verifying a second building cannot reuse an existing description");
        // The description alone is the building key now, so reusing the seeded building's description would collide.
        InsertRowsCommand insertCmd = new InsertRowsCommand("ehr_lookups", "buildings");
        insertCmd.addRow(Map.of("description", BUILDING_ID, "area", BUILDING_AREA));

        try
        {
            insertCmd.execute(getApiHelper().getConnection(), getContainerPath());
            Assert.fail("Building insert should have been rejected when the description was already in use");
        }
        catch (IOException | CommandException e)
        {
            Assert.assertTrue("Unexpected failure inserting a duplicate building: " + e.getMessage(),
                    e.getMessage() != null && e.getMessage().contains("already exists"));
        }
    }

    @Test
    public void testRoomDateDisabledCascade() throws Exception
    {
        String retiredRoom = roomKey(RETIRED_ROOM_NAME);
        String occupiedRoom = roomKey(OCCUPIED_ROOM_NAME);
        String draftRoom = roomKey(DRAFT_ROOM_NAME);
        String today = LocalDateTime.now().format(_dateFormat);
        String tomorrow = LocalDateTime.now().plusDays(1).format(_dateFormat);

        log("Creating a room to retire, one still holding an animal, and one holding only a draft record");
        InsertRowsCommand roomCmd = new InsertRowsCommand("ehr_lookups", "rooms");
        roomCmd.addRow(Map.of("name", RETIRED_ROOM_NAME, "building", BUILDING_ID));
        roomCmd.addRow(Map.of("name", OCCUPIED_ROOM_NAME, "building", BUILDING_ID));
        roomCmd.addRow(Map.of("name", DRAFT_ROOM_NAME, "building", BUILDING_ID));
        roomCmd.execute(getApiHelper().getConnection(), getContainerPath());

        InsertRowsCommand cageCmd = new InsertRowsCommand("ehr_lookups", "cage");
        cageCmd.addRow(Map.of("cage", FOLLOWING_CAGE_NAME, "room", retiredRoom));
        cageCmd.addRow(Map.of("cage", INDEPENDENT_CAGE_NAME, "room", retiredRoom, "dateDisabled", INDEPENDENT_CAGE_DATE));
        // No cage name, so the trigger derives each occupied room's location as the room key alone.
        cageCmd.addRow(Map.of("room", occupiedRoom));
        cageCmd.addRow(Map.of("room", draftRoom));
        cageCmd.execute(getApiHelper().getConnection(), getContainerPath());

        createAliveAnimals(OCCUPIED_ROOM_ANIMALS);
        houseAnimals(OCCUPIED_ROOM_ANIMALS, occupiedRoom);
        createAliveAnimals(DRAFT_ROOM_ANIMALS);
        houseAnimals(DRAFT_ROOM_ANIMALS, draftRoom, EHRQCState.IN_PROGRESS);

        log("Verifying a future disabled date is rejected");
        goToRoomsGrid().clickEditRow(retiredRoom)
                .setField("dateDisabled", tomorrow)
                .submitExpectingErrorContaining("A room cannot be disabled on a future date.");

        log("Verifying a room that still holds an animal is rejected, and names it");
        goToRoomsGrid().clickEditRow(occupiedRoom)
                .setField("dateDisabled", today)
                .submitExpectingErrorContaining("still housed there", OCCUPIED_ROOM_ANIMALS[0]);

        // A record still in data entry is not yet an occupancy, so it must not hold the room open.
        log("Verifying a draft housing record does not block the room");
        goToRoomsGrid().clickEditRow(draftRoom).update(Map.of("dateDisabled", today));
        assertRoomDateDisabled(draftRoom, today);

        log("Retiring the empty room from the grid");
        goToRoomsGrid().clickEditRow(retiredRoom).update(Map.of("dateDisabled", today));

        assertRoomDateDisabled(retiredRoom, today);
        assertCageDateDisabled(cageLocation(RETIRED_ROOM_NAME, FOLLOWING_CAGE_NAME), today);
        assertCageDateDisabled(cageLocation(RETIRED_ROOM_NAME, INDEPENDENT_CAGE_NAME), INDEPENDENT_CAGE_DATE);

        log("Verifying clearing the room's date undoes only the cascade it applied");
        goToRoomsGrid().clickEditRow(retiredRoom).update(Map.of("dateDisabled", ""));

        assertRoomDateDisabled(retiredRoom, null);
        assertCageDateDisabled(cageLocation(RETIRED_ROOM_NAME, FOLLOWING_CAGE_NAME), null);
        assertCageDateDisabled(cageLocation(RETIRED_ROOM_NAME, INDEPENDENT_CAGE_NAME), INDEPENDENT_CAGE_DATE);
    }

    /**
     * The editable rooms grid, which is where a room is retired.
     */
    private DataRegionTable goToRoomsGrid()
    {
        // The grid targets its links at a new tab unless told otherwise, leaving clickEditRow waiting on a
        // navigation the original window never makes.
        beginAt(WebTestHelper.buildURL("ehr", getContainerPath(), "updateTable",
                Map.of("schemaName", "ehr_lookups", "query.queryName", "rooms", "linkTarget", "_self")));
        return new DataRegionTable("query", this);
    }

    private void assertRoomDateDisabled(String room, @Nullable String expectedDay) throws Exception
    {
        assertDateDisabled("rooms", "room", room, expectedDay);
    }

    private void assertCageDateDisabled(String location, @Nullable String expectedDay) throws Exception
    {
        assertDateDisabled("cage", "location", location, expectedDay);
    }

    /**
     * Asserts the row keyed by the given value carries the expected disabled date as yyyy-MM-dd, or none when
     * expectedDay is null.
     */
    private void assertDateDisabled(String queryName, String keyColumn, String key, @Nullable String expectedDay) throws Exception
    {
        SelectRowsCommand select = new SelectRowsCommand("ehr_lookups", queryName);
        select.setColumns(List.of(keyColumn, "dateDisabled"));
        select.addFilter(new Filter(keyColumn, key));
        SelectRowsResponse response = select.execute(getApiHelper().getConnection(), getContainerPath());

        assertEquals("Expected exactly one " + queryName + " row keyed " + key, 1, response.getRows().size());
        assertEquals("Unexpected disabled date on " + key, expectedDay, toDay(response.getRows().getFirst().get("dateDisabled")));
    }

    @Test
    public void testGroupPenCagemates() throws Exception
    {
        String penRoom = roomKey(PEN_ROOM_NAME);

        log("Creating a group pen, whose location is the room with no cage");
        InsertRowsCommand roomCmd = new InsertRowsCommand("ehr_lookups", "rooms");
        roomCmd.addRow(Map.of("name", PEN_ROOM_NAME, "building", BUILDING_ID));
        roomCmd.execute(getApiHelper().getConnection(), getContainerPath());

        // With no cage supplied the trigger derives the location as the room key alone.
        InsertRowsCommand penCmd = new InsertRowsCommand("ehr_lookups", "cage");
        penCmd.addRow(Map.of("room", penRoom));
        penCmd.execute(getApiHelper().getConnection(), getContainerPath());

        createAliveAnimals(PEN_ANIMALS);

        // A pen has no cage name, so the location the cage trigger derived for it is the room key alone. That key is
        // the location id the animals are housed against, and sharing it is what makes them cagemates.
        log("Housing two animals against the pen's location");
        houseAnimals(PEN_ANIMALS, penRoom);

        log("Verifying penned animals resolve as each other's cagemates");
        assertCagemates(PEN_ANIMALS[0], 2, PEN_ANIMALS[1]);
    }

    @Test
    public void testCagematesWithoutRoom() throws Exception
    {
        log("Creating an unoccupied cage");
        InsertRowsCommand cageCmd = new InsertRowsCommand("ehr_lookups", "cage");
        cageCmd.addRow(Map.of("cage", ROOMLESS_CAGE_NAME, "room", roomKey("R1")));
        cageCmd.execute(getApiHelper().getConnection(), getContainerPath());

        createAliveAnimals(ROOMLESS_ANIMALS);

        // This location id names a cage within a room, the other shape a location takes. Cagemates must resolve for it
        // the same way they do for a pen's room-level id.
        log("Housing two animals against the same cage location");
        houseAnimals(ROOMLESS_ANIMALS, ROOMLESS_CAGE);

        log("Verifying animals sharing a cage location resolve as each other's cagemates");
        assertCagemates(ROOMLESS_ANIMALS[0], 2, ROOMLESS_ANIMALS[1]);
    }

    @Test
    public void testSnapshotShowsFullLocation() throws Exception
    {
        createAliveAnimals(LOCATION_ANIMALS);

        log("Housing an animal against a cage location");
        houseAnimals(LOCATION_ANIMALS, CAGE_IN_R1);

        log("Verifying Animal Details reports the room-qualified location rather than the bare cage");
        // The panel appends the housing date, so the location is a prefix of the field rather than the whole of it.
        String location = getSnapshotFieldValue(LOCATION_ANIMALS[0], "Location");
        Assert.assertTrue("Animal Details reported an unexpected location: " + location,
                location.startsWith(CAGE_IN_R1));
    }

    @Test
    public void testHousingTransferLocationColumns() throws Exception
    {
        createAliveAnimals(TRANSFER_ANIMALS);
        houseAnimals(new String[]{TRANSFER_ANIMALS[0]}, CAGE_IN_R1);
        houseAnimals(new String[]{TRANSFER_ANIMALS[1]}, CAGE_IN_R2);

        gotoEnterData();
        waitAndClickAndWait(Locator.linkWithText("Housing Transfers"));
        lockForm();

        Ext4GridRef housing = _helper.getExt4GridForFormSection("Housing Transfers");
        _helper.addRecordToGrid(housing);

        log("Verifying the location columns sit between the Id and the In Date");
        List<String> columns = getVisibleColumns(housing);
        int idIdx = columns.indexOf("Id");
        Assert.assertTrue("Housing grid has no Id column: " + columns, idIdx > -1);
        Assert.assertEquals("Unexpected column order after the Id",
                List.of("Id", "Id/curLocation/location", "cage", "date"),
                columns.subList(idIdx, Math.min(idIdx + 4, columns.size())));
        Assert.assertEquals("Wrong header over the current location",
                "Current Location", getColumnProperty(housing, "Id/curLocation/location", "text"));
        Assert.assertEquals("Wrong header over the new location",
                "New Location", getColumnProperty(housing, "cage", "text"));
        Assert.assertEquals("Current Location should not be editable",
                false, getColumnProperty(housing, "Id/curLocation/location", "editable"));

        log("Verifying the current location follows the animal's active housing");
        // the full location id, not the room alone that the shared EHR location lookup would build
        housing.setGridCell(1, "Id", TRANSFER_ANIMALS[0]);
        waitFor(() -> CAGE_IN_R1.equals(housing.getFieldValue(1, "Id/curLocation/location")),
                "Current location did not show " + CAGE_IN_R1 + ", was: " + housing.getFieldValue(1, "Id/curLocation/location"), WAIT_FOR_JAVASCRIPT);

        housing.setGridCell(1, "Id", TRANSFER_ANIMALS[1]);
        waitFor(() -> CAGE_IN_R2.equals(housing.getFieldValue(1, "Id/curLocation/location")),
                "Current location did not follow a changed Id to " + CAGE_IN_R2 + ", was: " + housing.getFieldValue(1, "Id/curLocation/location"), WAIT_FOR_JAVASCRIPT);

        // every check above reads the unsaved row, so discard rather than move the animal
        _helper.discardForm();
    }

    private List<String> getVisibleColumns(Ext4GridRef grid)
    {
        return (List<String>) grid.getFnEval("var ret = []; for (var i=0;i<this.columns.length;i++){if (!this.columns[i].hidden) ret.push(this.columns[i].dataIndex);} return ret;");
    }

    /**
     * Creates living demographics records for the given animals, replacing any left behind by an earlier run.
     */
    private void createAliveAnimals(String[] animalIds) throws Exception
    {
        log("Creating animals " + StringUtils.join(animalIds, ", "));
        String[] fields = new String[]{"Id", "Species", "Birth", "Gender", "date", "calculated_status", "objectid", "performedby"};
        Object[][] data = new Object[animalIds.length][];
        for (int i = 0; i < animalIds.length; i++)
        {
            data[i] = new Object[]{animalIds[i], "Rhesus", (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004};
        }

        SimplePostCommand insertCommand = getApiHelper().prepareInsertCommand("study", "demographics", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "demographics", new Filter("Id", StringUtils.join(animalIds, ";"), Filter.Operator.IN));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());
    }

    /**
     * Opens a completed housing record for each animal at the given location, replacing any left behind by an earlier
     * run. The location id in 'cage' is the only location housing stores: 'room' is derived from it, is read-only, and
     * would be discarded if it were posted here.
     */
    private void houseAnimals(String[] animalIds, String cage) throws Exception
    {
        houseAnimals(animalIds, cage, EHRQCState.COMPLETED);
    }

    private void houseAnimals(String[] animalIds, String cage, EHRQCState qcState) throws Exception
    {
        String[] fields = new String[]{"Id", "date", "enddate", "cage", "QCStateLabel", "performedby"};
        Object[][] data = new Object[animalIds.length][];
        for (int i = 0; i < animalIds.length; i++)
        {
            data[i] = new Object[]{animalIds[i], new Date(), null, cage, qcState.label, 1004};
        }

        SimplePostCommand insertCommand = getApiHelper().prepareInsertCommand("study", "Housing", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "Housing", new Filter("Id", StringUtils.join(animalIds, ";"), Filter.Operator.IN));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());
    }

    /**
     * Asserts the cagemates report resolves the expected companions for an animal. A null total means the join matched
     * nothing at all, which is reported as its own failure rather than as an unexpected count.
     */
    private void assertCagemates(String animalId, int expectedTotal, String expectedCompanion) throws Exception
    {
        SelectRowsCommand selectCmd = new SelectRowsCommand("study", "demographicsCagemates");
        selectCmd.addFilter(new Filter("Id", animalId));
        SelectRowsResponse response = selectCmd.execute(getApiHelper().getConnection(), getContainerPath());

        Assert.assertEquals("Expected one cagemates row for " + animalId, 1, response.getRows().size());
        Map<String, Object> cagemates = response.getRows().get(0);
        Assert.assertNotNull("Cagemates resolved no location for " + animalId, cagemates.get("total"));
        Assert.assertEquals("Unexpected cagemate count for " + animalId, expectedTotal, ((Number) cagemates.get("total")).intValue());
        Assert.assertTrue("Cagemate list should name " + expectedCompanion + ", was: " + cagemates.get("animals"),
                String.valueOf(cagemates.get("animals")).contains(expectedCompanion));
    }

    /**
     * Entering a group membership has to close the one the animal already holds, which the module asks for by listing
     * animal_group_members in datasetsToCloseOnNewEntry. That option lives in a single server-wide map when registered
     * from Java, so on a server carrying more than one EHR module it was whichever module started last that decided
     * the list. Entering the memberships through the API rather than the form keeps this on the trigger script.
     */
    @Test
    public void testGroupMembershipClosedOnNewEntry() throws Exception
    {
        String animalId = "GROUP0001";
        String firstGroup = "P";  // Project Breeding
        String secondGroup = "T"; // Time-Mated
        LocalDateTime joinedFirst = LocalDateTime.now().minusDays(10);
        LocalDateTime joinedSecond = LocalDateTime.now().minusDays(3);

        createAliveAnimals(new String[]{animalId});

        log("Assigning " + animalId + " to its first group");
        insertGroupMembership(animalId, firstGroup, joinedFirst);
        Assert.assertNull("A newly entered group membership should be left open",
                getGroupMembershipEnd(animalId, firstGroup));

        log("Assigning " + animalId + " to a second group");
        insertGroupMembership(animalId, secondGroup, joinedSecond);

        log("Verifying the earlier membership was closed at the new one's start date");
        assertEquals("Entering a group membership did not close the one the animal already held",
                joinedSecond.format(_dateFormat), getGroupMembershipEnd(animalId, firstGroup));
        Assert.assertNull("The membership just entered should be left open",
                getGroupMembershipEnd(animalId, secondGroup));
    }

    private void insertGroupMembership(String animalId, String groupId, LocalDateTime date)
    {
        String[] fields = new String[]{"Id", "date", "groupId", FIELD_QCSTATELABEL, FIELD_OBJECTID, "performedby"};
        Object[][] data = new Object[][]{
                {animalId, Date.from(date.atZone(ZoneId.systemDefault()).toInstant()), groupId,
                        EHRQCState.COMPLETED.label, UUID.randomUUID().toString(), 1004}
        };
        SimplePostCommand insertCommand = getApiHelper().prepareInsertCommand("study", "animal_group_members", "lsid", fields, data);
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());
    }

    /** @return the day the animal's membership of the given group ended, or null while it is still open */
    private String getGroupMembershipEnd(String animalId, String groupId) throws IOException, CommandException
    {
        SelectRowsCommand select = new SelectRowsCommand("study", "animal_group_members");
        select.setColumns(List.of("Id", "groupId", "enddate"));
        select.addFilter(new Filter("Id", animalId));
        select.addFilter(new Filter("groupId", groupId));
        SelectRowsResponse response = select.execute(getApiHelper().getConnection(), getContainerPath());

        assertEquals("Expected exactly one " + groupId + " membership for " + animalId, 1, response.getRows().size());
        return toDay(response.getRows().getFirst().get("enddate"));
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
        waitForValidationToClear("Id is required");
        _helper.setDataEntryField("remark", "Closing the case");
        waitForValidationToClear("Subjective: WARN: Must enter at least one comment");
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

    // Creates the parents of a conception. They need an ehr_lookups.species_codes code because the Start with
    // Conception window copies the dam's species onto the newborn, and the test asserts the resulting record.
    // Adds a birth row through the Start with Conception window, the only way to fill the conception fields on a row
    // that does not exist yet.
    private void startWithConception(Ext4GridRef births, String conceptId, int expectedRowCount)
    {
        births.clickTbarButton("Start with Conception");
        Window<?> conceptionWindow = new Window.WindowFinder(getDriver()).withTitle("Start with Conception").waitFor();
        Ext4ComboRef conceptionCombo = _ext4Helper.queryOne("window #conceptionField", Ext4ComboRef.class);
        Assert.assertNotNull("Conception Id field not found in the Start with Conception window", conceptionCombo);
        conceptionCombo.waitForStoreLoad();
        conceptionCombo.setComboByDisplayValue(conceptId);
        conceptionWindow.clickButton("Submit", 0);
        births.waitForRowCount(expectedRowCount);
    }

    // Fills in everything a birth row needs beyond what the conception supplies, so the form can be submitted.
    // Generation is not set here: the conception supplies it from the dam. Each row takes its own cage so that a
    // co-housing or capacity rule can never be what fails these tests.
    private void fillBirthRow(Ext4GridRef births, int rowIdx, String animalId, LocalDateTime birthDate,
                              String socialCode, String animalGroup, String cage)
    {
        births.setGridCellJS(rowIdx, "date", birthDate.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_STRING)));
        births.setGridCell(rowIdx, "Id", animalId);
        births.setGridCell(rowIdx, "cage", cage);
        births.setGridCell(rowIdx, "Id/demographics/gender", "Female");
        births.setGridCell(rowIdx, "Id/demographics/socialCode", socialCode);
        births.setGridCell(rowIdx, "project", "795644");
        births.setGridCell(rowIdx, "birthProtocol", protocolChoice(PROTOCOL_ID, INVES_LAST_NAME));
        births.setGridCell(rowIdx, "groupId", animalGroup);
    }

    private void createBreedingPair(String damId, String sireId, String species) throws Exception
    {
        createBreedingPair(damId, sireId, species, null);
    }

    // A null damGeneration leaves the dam with no generation of her own, which is what makes her offspring generation 1.
    private void createBreedingPair(String damId, String sireId, String species, Integer damGeneration) throws Exception
    {
        String[] fields = new String[]{"Id", "Species", "Birth", "Gender", "date", "calculated_status", "objectid", "performedby", "generation"};
        Object[][] data = new Object[][]{
                {damId, species, (new Date()).toString(), getFemale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004, damGeneration},
                {sireId, species, (new Date()).toString(), getMale(), new Date(), "Alive", UUID.randomUUID().toString(), 1004, null}
        };
        SimplePostCommand insertCommand = getApiHelper().prepareInsertCommand("study", "demographics", "lsid", fields, data);
        getApiHelper().deleteAllRecords("study", "demographics", new Filter("Id", damId + ";" + sireId, Filter.Operator.IN));
        getApiHelper().doSaveRows(DATA_ADMIN.getEmail(), insertCommand, getExtraContext());
    }

    // Asserts the Births columns appear in the expected left to right order. Relative position is checked rather
    // than absolute index so that hidden and system columns can come and go without breaking the test.
    private void verifyBirthColumnOrder(Ext4GridRef births)
    {
        List<String> expectedOrder = List.of("Id", "date", "conceptId", "Id/demographics/species", "Id/demographics/gender",
                "Id/demographics/dam", "Id/demographics/sire", "cage", "Id/demographics/socialCode",
                "Id/demographics/generation", "project",
                "birthProtocol", "groupId", "type", "pregnancyType", "remark", "performedby");

        int previousIdx = 0;
        String previousCol = null;
        for (String col : expectedOrder)
        {
            int idx = births.getIndexOfColumn(col, true);
            Assert.assertTrue("Births column '" + col + "' should appear to the right of '" + previousCol + "'", idx > previousIdx);
            previousIdx = idx;
            previousCol = col;
        }
    }

    private void waitForFormError(String message)
    {
        waitFor(() -> isTextPresent(message), "Form did not report: " + message, WAIT_FOR_JAVASCRIPT);
    }

    /**
     * Reads a date field for one animal through the API rather than off a grid, so assertions compare stored values
     * instead of formatted display text, and normalizes to the day: event dates are entered with the time stripped,
     * but values reaching demographics by other paths can carry a time component.
     *
     * @return the date as yyyy-MM-dd, or null when the field is empty
     */
    private String getDatasetDay(String queryName, String animalId, String column) throws IOException, CommandException
    {
        return toDay(getSingleRowForAnimal(queryName, animalId, List.of("Id", column)).get(column));
    }

    /** @return a stored date value as yyyy-MM-dd, or null when it is empty */
    private String toDay(Object value)
    {
        if (value == null)
            return null;

        if (value instanceof Date)
            return _dateFormat.format(((Date)value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        String text = String.valueOf(value);
        return text.length() >= 10 ? text.substring(0, 10) : text;
    }

    private String getCalculatedStatus(String animalId) throws IOException, CommandException
    {
        return (String)getSingleRowForAnimal("demographics", animalId, List.of("Id", "calculated_status")).get("calculated_status");
    }

    private Map<String, Object> getSingleRowForAnimal(String queryName, String animalId, List<String> columns) throws IOException, CommandException
    {
        SelectRowsCommand select = new SelectRowsCommand("study", queryName);
        select.setColumns(columns);
        select.addFilter(new Filter("Id", animalId));
        SelectRowsResponse response = select.execute(getApiHelper().getConnection(), getContainerPath());

        Assert.assertEquals("Expected exactly one study." + queryName + " row for " + animalId, 1, response.getRows().size());
        return response.getRows().get(0);
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
        submitForm(buttonText, windowTitle, true);
    }

    private void submitForm(String buttonText, String windowTitle, boolean expectNavigation)
    {
        waitForFormValidationToClear();
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
        // a successful save navigates to the form's success URL, but a refused one leaves the browser on the form,
        // so only the former can wait for a page load
        if (expectNavigation)
            msgWindow.clickButton("Yes");
        else
            msgWindow.clickButton("Yes", 0);
    }

    /**
     * Submits the form expecting the save to be refused, and asserts the given message is reported. Dismisses the
     * alert the panel raises so the form can be corrected and submitted again. A refused save is logged as a server
     * error, which the caller has to account for through checkExpectedErrors.
     */
    private void submitFormExpectingError(String message)
    {
        submitForm("Submit Final", "Finalize", false);
        waitForFormError(message);
        new Window.WindowFinder(getDriver()).withTitle("Error").waitFor().clickButton("OK", true);
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

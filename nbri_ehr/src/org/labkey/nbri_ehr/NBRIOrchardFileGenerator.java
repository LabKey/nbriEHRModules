/*
 * Copyright (c) 2024-2026 LabKey Corporation
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

import org.apache.logging.log4j.Logger;
import org.labkey.api.data.CompareType;
import org.labkey.api.data.Container;
import org.labkey.api.data.DbScope;
import org.labkey.api.data.SimpleFilter;
import org.labkey.api.data.TableInfo;
import org.labkey.api.data.TableSelector;
import org.labkey.api.module.Module;
import org.labkey.api.module.ModuleLoader;
import org.labkey.api.module.ModuleProperty;
import org.labkey.api.query.FieldKey;
import org.labkey.api.query.QueryService;
import org.labkey.api.query.UserSchema;
import org.labkey.api.security.User;
import org.labkey.api.study.StudyService;
import org.labkey.api.util.JobRunner;
import org.labkey.api.util.PageFlowUtil;
import org.labkey.api.util.logging.LogHelper;
import org.labkey.api.writer.PrintWriters;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class NBRIOrchardFileGenerator
{
    public static final String NBRIOrchardFileLocation = "NBRIOrchardFileLocation";
    private static final Logger _log = LogHelper.getLogger(NBRIOrchardFileGenerator.class, "Orchard update on trigger.");

    public void generateOrchardFile(Container c, User u, String taskid)
    {
        String orchardFileLocation = getOrchardFileLocation(c);
        // Generate the Orchard file
        if (orchardFileLocation == null)
        {
            return;
        }

        try (DbScope.Transaction transaction = StudyService.get().getDatasetSchema().getScope().ensureTransaction())
        {
            // This will add a post commit task to write the file. The post commit task will only be added once for every taskid
            transaction.addCommitTask(new FileWriteTask(c, u, taskid, orchardFileLocation), DbScope.CommitTaskOption.POSTCOMMIT);
            transaction.commit();
        }
    }

    public String getOrchardFileLocation(Container c)
    {
        Module ehr = ModuleLoader.getInstance().getModule(NBRI_EHRModule.NAME);
        if (ehr == null)
        {
            throw new IllegalStateException("Could not find the NBRI EHR module");
        }
        ModuleProperty mp = ehr.getModuleProperties().get(NBRIOrchardFileLocation);
        return mp.getEffectiveValue(c);
    }

    private static class FileWriteTask implements Runnable
    {
        private final Container c;
        private final User u;
        private final String taskid;
        private final String orchardFileLocation;

        public FileWriteTask(Container c, User u, String taskid, String orchardFileLocation)
        {
            this.c = c;
            this.u = u;
            this.taskid = taskid;
            this.orchardFileLocation = orchardFileLocation;
        }

        @Override
        public void run()
        {
            if (orchardFileLocation == null)
            {
                // Don't run if we don't have a location to write the file
                _log.warn("Orchard file location is null, cannot generate Orchard file for taskid: {}", taskid);
                return;
            }

            JobRunner.getDefault().execute(() ->
            {
                TableInfo ti = getTableInfo(c, u, "study", "orchardData");
                SimpleFilter filter = new SimpleFilter(FieldKey.fromString("Id"), Arrays.asList(getAnimalIds(c, u, taskid).toArray(new String[0])), CompareType.IN);
                StringBuilder sb = new StringBuilder();
                String curDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                SimpleDateFormat orcFormat = new SimpleDateFormat("yyyyMMdd");
                String orchardFileName = "orchardFile" + curDate + ".txt";

                sb.append("MSH|^~\\&|LabKey|ARS|Orchard|Lab|");
                sb.append(curDate);
                sb.append("||ADT^A01|12345|P|2.3");
                sb.append(System.lineSeparator());

                new TableSelector(ti, PageFlowUtil.set("Id", "gender", "birth", "species", "protocol", "PI", "Vet", "cage", "alive"), filter, null).forEachResults(rs -> {
                    sb.append("PID|1|");
                    sb.append(rs.getString("Id")).append("|");
                    sb.append(rs.getString("Id")).append("||");
                    sb.append(rs.getString("Id")).append("^").append(rs.getString("species")).append("||");
                    sb.append(orcFormat.format(rs.getDate("birth"))).append("|");
                    if (Objects.equals(rs.getString("gender"), "2"))
                        sb.append("F");
                    else if (Objects.equals(rs.getString("gender"), "3"))
                        sb.append("M");
                    else sb.append("U");
                    sb.append("||NHP|"); //could change if more species
                    sb.append(rs.getString("cage"));
                    sb.append("^New Iberia^LA^70506|||||||||||");
                    sb.append(rs.getString("species")).append("|");
                    sb.append(rs.getString("protocol")).append("|||||||");
                    if (Objects.equals(rs.getString("alive"), "Alive"))
                        sb.append("N");
                    else
                        sb.append("Y");
                    sb.append(System.lineSeparator());
                    sb.append("ZCP|Veterinarian|").append(rs.getString("Vet")).append("|");
                    sb.append(System.lineSeparator());
                    sb.append("PD1||||").append(rs.getString("PI")).append("|");
                    sb.append(System.lineSeparator());
                });

                try (PrintWriter writer = PrintWriters.getPrintWriter(new File(orchardFileLocation + File.separator + orchardFileName))) {
                    writer.write(sb.toString());
                }
                catch (IOException e)
                {
                    throw new RuntimeException("Error generating Orchard file", e);
                }
            });
        }

        // Overwritten to prevent duplicate tasks being added for a given taskid
        @Override
        public boolean equals(Object obj)
        {
            if (obj instanceof FileWriteTask fwt)
            {
                return taskid.equals(fwt.taskid);
            }

            return super.equals(obj);
        }

        // Overwritten to prevent duplicate tasks being added for a given taskid
        @Override
        public int hashCode()
        {
            return Objects.hash(taskid);
        }

        private TableInfo getTableInfo(Container c, User u, String schemaName, String queryName)
        {
            UserSchema us = QueryService.get().getUserSchema(u, c, schemaName);
            if (us == null)
                throw new IllegalArgumentException("Unable to find schema: " + schemaName);

            TableInfo ti = us.getTable(queryName);
            if (ti == null)
                throw new IllegalArgumentException("Unable to find table: " + schemaName + "." + queryName);

            return ti;
        }

        // Gets a list of animal ids for a given taskid using the query orchardIdsForTaskid
        private List<String> getAnimalIds(Container c, User u, String taskid)
        {
            TableInfo ti = getTableInfo(c, u, "study", "orchardIdsForTaskid");
            TableSelector selector = new TableSelector(ti, null, null);

            Map<String, Object> params = new HashMap<>();
            params.put("MYTASKID", taskid);
            selector.setNamedParameters(params);

            return selector.getArrayList(String.class);
        }
    }
}

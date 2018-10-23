/*
 * Copyright (c) 2014-2018 T-Systems Multimedia Solutions GmbH
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

package de.tsystems.mms.apm.performancesignature.dynatrace;

import de.tsystems.mms.apm.performancesignature.dynatrace.configuration.ConfigurationTestCase;
import de.tsystems.mms.apm.performancesignature.dynatrace.configuration.Dashboard;
import de.tsystems.mms.apm.performancesignature.dynatrace.configuration.GenericTestCase;
import de.tsystems.mms.apm.performancesignature.dynatrace.model.Alert;
import de.tsystems.mms.apm.performancesignature.dynatrace.model.DashboardReport;
import de.tsystems.mms.apm.performancesignature.dynatrace.rest.DTServerConnection;
import de.tsystems.mms.apm.performancesignature.dynatrace.rest.xml.RESTErrorException;
import de.tsystems.mms.apm.performancesignature.dynatrace.rest.xml.model.Agent;
import de.tsystems.mms.apm.performancesignature.dynatrace.util.PerfSigUtils;
import de.tsystems.mms.apm.performancesignature.dynatrace.util.TestUtils;
import de.tsystems.mms.apm.performancesignature.ui.PerfSigBuildAction;
import de.tsystems.mms.apm.performancesignature.ui.model.ClientLinkGenerator;
import hudson.AbortException;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import hudson.util.ListBoxModel;
import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class RecorderTest {

    @ClassRule
    public static final JenkinsRule j = new JenkinsRule();
    private static ListBoxModel dynatraceConfigurations;
    private DTServerConnection connection;

    @BeforeClass
    public static void setUp() throws Exception {
        dynatraceConfigurations = TestUtils.prepareDTConfigurations();
    }

    public RecorderTest() throws AbortException, RESTErrorException {
        connection = PerfSigUtils.createDTServerConnection(dynatraceConfigurations.get(0).name);
    }

    @Test
    public void testJenkinsConfiguration() throws Exception {
        String testCase = "RecorderTest";

        FreeStyleProject project = j.createFreeStyleProject();
        project.getBuildersList().add(new PerfSigStartRecording(dynatraceConfigurations.get(0).name, testCase));
        //wait some time to get some data into the session
        if (TestUtils.isWindows()) {
            project.getBuildersList().add(new BatchFile("ping -n 10 127.0.0.1 > NUL"));
        } else {
            project.getBuildersList().add(new Shell("sleep 10"));
        }
        project.getBuildersList().add(new PerfSigStopRecording(dynatraceConfigurations.get(0).name));
        ConfigurationTestCase configurationTestCase = new GenericTestCase(testCase,
                Collections.singletonList(new Dashboard("PerformanceSignature_singlereport")),
                Collections.singletonList(new Dashboard("PerformanceSignature_comparisonreport")),
                "PerformanceSignature_xml");
        configurationTestCase.setClientDashboard(ClientLinkGenerator.PUREPATH_OVERVIEW);

        PerfSigRecorder recorder = new PerfSigRecorder(dynatraceConfigurations.get(0).name, Collections.singletonList(configurationTestCase));
        recorder.setExportSessions(true);
        recorder.setDeleteSessions(true);
        project.getPublishersList().add(recorder);
        FreeStyleBuild build = j.assertBuildStatusSuccess(project.scheduleBuild2(0));

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("connection successful, getting reports for this build and testcase " + testCase));
        assertTrue(s.contains("getting PDF report: Singlereport")); //no Comparisonreport available
        assertTrue(s.contains("parsing XML report"));
        assertTrue(s.contains("session successfully downloaded"));

        PerfSigBuildAction buildAction = build.getAction(PerfSigBuildAction.class);
        assertNotNull(buildAction);
        assertNotNull(buildAction.getDashboardReports());
        DashboardReport dashboardReport = buildAction.getDashboardReports().get(0);
        assertNotNull(dashboardReport);
        assertNotNull(dashboardReport.getChartDashlets());
        assertEquals(7, dashboardReport.getChartDashlets().size());
    }

    @Test
    public void testPipelineConfiguration() throws Exception {
        String testCase = "RecorderTest";

        WorkflowJob p = j.createProject(WorkflowJob.class);
        p.setDefinition(new CpsFlowDefinition("node('master') {" +
                "startSession dynatraceProfile: 'easy Travel (admin) @ PoC PerfSig', testCase: 'RecorderTest'\n" +
                "sleep 10\n" +
                "stopSession 'easy Travel (admin) @ PoC PerfSig'\n" +
                "perfSigReports configurationTestCases: [[$class: 'GenericTestCase', clientDashboard: 'PurePath Overview'," +
                "name: 'RecorderTest', xmlDashboard: 'PerformanceSignature_xml']]," +
                "dynatraceProfile: 'easy Travel (fn_perfsig) @ PoC PerfSig', deleteSessions: true, exportSessions: false, removeConfidentialStrings: true\n" +
                "}", true));
        WorkflowRun b = j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        j.assertLogContains("connection successful, getting reports for this build and testcase " + testCase, b);
        j.assertLogContains("parsing XML report", b);

        PerfSigBuildAction buildAction = b.getAction(PerfSigBuildAction.class);
        assertNotNull(buildAction);
        assertNotNull(buildAction.getDashboardReports());
        DashboardReport dashboardReport = buildAction.getDashboardReports().get(0);
        assertNotNull(dashboardReport);
        assertNotNull(dashboardReport.getChartDashlets());
        assertEquals(7, dashboardReport.getChartDashlets().size());
    }

    @Test
    public void testEmptyConfiguration() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        project.getPublishersList().add(new PerfSigRecorder("", null));

        FreeStyleBuild build = j.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0));

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("failed to lookup Dynatrace server configuration"));
    }

    @Test
    public void testConfigurationValidation() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        ConfigurationTestCase configurationTestCase = new GenericTestCase("", null, null, "");

        project.getPublishersList().add(new PerfSigRecorder(dynatraceConfigurations.get(0).name, Collections.singletonList(configurationTestCase)));
        FreeStyleBuild build = j.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0));

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("TestCase can not be validated"));
    }

    @Test
    public void testMissingEnvInvisAction() throws Exception {
        FreeStyleProject project = j.createFreeStyleProject();
        ConfigurationTestCase configurationTestCase = new GenericTestCase("Loadtest", null, null,
                "PerformanceSignature_xml");

        project.getPublishersList().add(new PerfSigRecorder(dynatraceConfigurations.get(0).name, Collections.singletonList(configurationTestCase)));
        FreeStyleBuild build = j.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0));

        String s = FileUtils.readFileToString(build.getLogFile());
        assertTrue(s.contains("no sessionname found, aborting ..."));
    }

    @Test
    public void testGetDashboardViaRest() {
        List<de.tsystems.mms.apm.performancesignature.dynatrace.rest.xml.model.Dashboard> dashboardList = connection.getDashboards();
        assertTrue(!dashboardList.isEmpty());
    }

    @Test
    public void testGetAgentsViaRest() {
        List<Agent> agentList = connection.getAllAgents();
        assertTrue(!agentList.isEmpty());
    }

    @Test
    public void testHotSensorPlacementViaRest() {
        for (Agent agent : connection.getAgents()) {
            if ("java".equalsIgnoreCase(agent.getTechnologyType())) {
                assertTrue(connection.hotSensorPlacement(agent.getAgentId()));
            }
        }
    }

    @Test
    public void testIncidentsViaRest() throws Exception {
        DTServerConnection connection = PerfSigUtils.createDTServerConnection(dynatraceConfigurations.get(0).name);
        Date now = new Date();
        now.setTime(now.getTime() - 43200000L);
        List<Alert> alerts = connection.getIncidents(now, new Date());

        assertNotNull(alerts);
    }
}

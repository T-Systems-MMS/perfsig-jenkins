/*
 * Copyright (c) 2014 T-Systems Multimedia Solutions GmbH
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

package de.tsystems.mms.apm.performancesignature;

import de.tsystems.mms.apm.performancesignature.dynatrace.rest.DTServerConnection;
import de.tsystems.mms.apm.performancesignature.dynatrace.rest.RESTErrorException;
import de.tsystems.mms.apm.performancesignature.model.DynatraceServerConfiguration;
import de.tsystems.mms.apm.performancesignature.util.PerfSigUtils;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;

public class PerfSigStopRecording extends Builder implements SimpleBuildStep {
    private static final int reanalyzeSessionTimeout = 60000; //==1 minute
    private static final int reanalyzeSessionPollingInterval = 5000; //==5 seconds
    private final String dynatraceServer;
    private boolean reanalyzeSession;

    @DataBoundConstructor
    public PerfSigStopRecording(final String dynatraceServer) {
        this.dynatraceServer = dynatraceServer;
    }

    @Override
    public void perform(@Nonnull final Run<?, ?> run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher, @Nonnull final TaskListener listener)
            throws InterruptedException, IOException {
        final PrintStream logger = listener.getLogger();

        logger.println(Messages.PerfSigStopRecording_StopSessionRecording());
        DynatraceServerConfiguration serverConfiguration = PerfSigUtils.getServerConfiguration(dynatraceServer);
        if (serverConfiguration == null)
            throw new AbortException("failed to lookup Dynatrace server configuration");

        final DTServerConnection connection = new DTServerConnection(serverConfiguration);

        String sessionName = connection.stopRecording(serverConfiguration.getProfile());
        if (StringUtils.isBlank(sessionName))
            throw new RESTErrorException(Messages.PerfSigStopRecording_InternalError());
        logger.println(String.format("Stopped recording on %s with SessionName %s", serverConfiguration.getProfile(), sessionName));

        if (getReanalyzeSession()) {
            logger.println("reanalyze session ...");
            boolean reanalyzeFinished = connection.reanalyzeSessionStatus(sessionName);
            if (connection.reanalyzeSession(sessionName)) {
                int timeout = reanalyzeSessionTimeout;
                while ((!reanalyzeFinished) && (timeout > 0)) {
                    logger.println("querying session analysis status");
                    try {
                        Thread.sleep(reanalyzeSessionPollingInterval);
                        timeout -= reanalyzeSessionPollingInterval;
                    } catch (InterruptedException ignored) {
                    }
                    reanalyzeFinished = connection.reanalyzeSessionStatus(sessionName);
                }
                if (reanalyzeFinished) {
                    logger.println("session reanalysis finished");
                } else {
                    throw new RESTErrorException("Timeout raised");
                }
            }
        }
    }

    public String getDynatraceServer() {
        return dynatraceServer;
    }

    public boolean getReanalyzeSession() {
        return reanalyzeSession;
    }

    @DataBoundSetter
    public void setReanalyzeSession(final boolean reanalyzeSession) {
        this.reanalyzeSession = reanalyzeSession;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public static final boolean defaultReanalyzeSession = false;

        public ListBoxModel doFillDynatraceServerItems() {
            return PerfSigUtils.listToListBoxModel(PerfSigUtils.getDTConfigurations());
        }

        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return Messages.PerfSigStopRecording_DisplayName();
        }
    }
}
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

import de.tsystems.mms.apm.performancesignature.dynatrace.rest.DTServerConnection;
import de.tsystems.mms.apm.performancesignature.dynatrace.rest.xml.RESTErrorException;
import de.tsystems.mms.apm.performancesignature.dynatrace.rest.xml.model.Agent;
import de.tsystems.mms.apm.performancesignature.dynatrace.util.PerfSigUtils;
import de.tsystems.mms.apm.performancesignature.ui.util.PerfSigUIUtils;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.analysis.util.PluginLogger;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;

import static de.tsystems.mms.apm.performancesignature.dynatrace.PerfSigMemoryDump.WAIT_FOR_DUMP_POLLING_INTERVAL;
import static de.tsystems.mms.apm.performancesignature.dynatrace.PerfSigMemoryDump.WAIT_FOR_DUMP_TIMEOUT;

public class PerfSigThreadDump extends Builder implements SimpleBuildStep {
    private final String dynatraceProfile;
    private final String agent;
    private final String host;
    private boolean lockSession;

    @DataBoundConstructor
    public PerfSigThreadDump(final String dynatraceProfile, final String agent, final String host) {
        this.dynatraceProfile = dynatraceProfile;
        this.agent = agent;
        this.host = host;
    }

    @Override
    public void perform(@Nonnull final Run<?, ?> run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher, @Nonnull final TaskListener listener)
            throws InterruptedException, IOException {
        PluginLogger logger = PerfSigUIUtils.createLogger(listener.getLogger());
        DTServerConnection connection = PerfSigUtils.createDTServerConnection(dynatraceProfile);

        for (Agent availAgent : connection.getAgents()) {
            if (availAgent.getName().equals(this.agent) && availAgent.getSystemProfile().equals(connection.getCredProfilePair().getProfile()) &&
                    availAgent.getHost().equals(this.host)) {
                logger.log(Messages.PerfSigThreadDump_CreatingThreadDump(availAgent.getSystemProfile(), availAgent.getName(), availAgent.getHost(),
                        String.valueOf(availAgent.getProcessId())));

                String threadDump = connection.threadDump(availAgent.getName(), availAgent.getHost(), availAgent.getProcessId(), getLockSession());
                if (StringUtils.isBlank(threadDump)) {
                    throw new RESTErrorException(Messages.PerfSigThreadDump_ThreadDumpWasntTaken());
                }
                long timeout = WAIT_FOR_DUMP_TIMEOUT;
                boolean dumpFinished = connection.threadDumpStatus(threadDump);
                while ((!dumpFinished) && (timeout > 0)) {
                    Thread.sleep(WAIT_FOR_DUMP_POLLING_INTERVAL);
                    timeout -= WAIT_FOR_DUMP_POLLING_INTERVAL;
                    dumpFinished = connection.threadDumpStatus(threadDump);
                }
                if (dumpFinished) {
                    logger.log(Messages.PerfSigThreadDump_SuccessfullyCreatedThreadDump(availAgent.getName()));
                    return;
                } else {
                    throw new RESTErrorException(Messages.PerfSigStopRecording_TimeoutRaised());
                }
            }
        }
        throw new AbortException(Messages.PerfSigThreadDump_AgentNotConnected(agent));
    }

    public String getDynatraceProfile() {
        return dynatraceProfile;
    }

    public String getAgent() {
        return agent;
    }

    public String getHost() {
        return host;
    }

    public boolean getLockSession() {
        return lockSession;
    }

    @DataBoundSetter
    public void setLockSession(final boolean lockSession) {
        this.lockSession = lockSession;
    }

    @Symbol("createThreadDump")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public static final boolean defaultLockSession = false;

        @Restricted(NoExternalUse.class)
        public FormValidation doCheckAgent(@AncestorInPath Item item, @QueryParameter final String agent) {
            FormValidation validationResult = FormValidation.ok();
            if (PerfSigUIUtils.checkForMissingPermission(item)) {
                return validationResult;
            }

            if (StringUtils.isNotBlank(agent)) {
                return validationResult;
            } else {
                return FormValidation.error(Messages.PerfSigThreadDump_AgentNotValid());
            }
        }

        @Restricted(NoExternalUse.class)
        public FormValidation doCheckHost(@AncestorInPath Item item, @QueryParameter final String host) {
            FormValidation validationResult = FormValidation.ok();
            if (PerfSigUIUtils.checkForMissingPermission(item)) {
                return validationResult;
            }

            if (StringUtils.isNotBlank(host)) {
                return validationResult;
            } else {
                return FormValidation.error(Messages.PerfSigThreadDump_HostNotValid());
            }
        }

        @Nonnull
        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillDynatraceProfileItems(@AncestorInPath Item item) {
            if (PerfSigUIUtils.checkForMissingPermission(item)) {
                return new ListBoxModel();
            }
            return PerfSigUtils.listToListBoxModel(PerfSigUtils.getDTConfigurations());
        }

        @Nonnull
        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillAgentItems(@AncestorInPath Item item, @QueryParameter final String dynatraceProfile) {
            if (PerfSigUIUtils.checkForMissingPermission(item)) {
                return new ListBoxModel();
            }
            return PerfSigUtils.fillAgentItems(dynatraceProfile);
        }

        @Nonnull
        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillHostItems(@AncestorInPath Item item,
                                            @QueryParameter final String dynatraceProfile,
                                            @QueryParameter final String agent) {
            if (PerfSigUIUtils.checkForMissingPermission(item)) {
                return new ListBoxModel();
            }
            return PerfSigUtils.fillHostItems(dynatraceProfile, agent);
        }

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.PerfSigThreadDump_DisplayName();
        }
    }
}

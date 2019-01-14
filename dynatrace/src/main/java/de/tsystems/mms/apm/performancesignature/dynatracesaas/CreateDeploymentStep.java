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

package de.tsystems.mms.apm.performancesignature.dynatracesaas;

import com.google.common.collect.ImmutableSet;
import de.tsystems.mms.apm.performancesignature.dynatracesaas.model.CustomProperty;
import de.tsystems.mms.apm.performancesignature.dynatracesaas.model.EntityId;
import de.tsystems.mms.apm.performancesignature.dynatracesaas.model.TagMatchRule;
import de.tsystems.mms.apm.performancesignature.dynatracesaas.util.DynatraceUtils;
import de.tsystems.mms.apm.performancesignature.ui.util.PerfSigUIUtils;
import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import org.apache.commons.collections.CollectionUtils;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreateDeploymentStep extends Step {
    private final String envId;
    private List<EntityId> entityIds;
    private List<TagMatchRule> tagMatchRules;
    private List<CustomProperty> customProperties;

    @DataBoundConstructor
    public CreateDeploymentStep(String envId) {
        this.envId = envId;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new CreateDeploymentStepExecution(this, context);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getEnvId() {
        return envId;
    }

    public List<EntityId> getEntityIds() {
        return entityIds;
    }

    @DataBoundSetter
    public CreateDeploymentStep setEntityIds(List<EntityId> entityIds) {
        this.entityIds = entityIds;
        return this;
    }

    public List<CustomProperty> getCustomProperties() {
        return customProperties;
    }

    @DataBoundSetter
    public CreateDeploymentStep setCustomProperties(List<CustomProperty> customProperties) {
        this.customProperties = customProperties;
        return this;
    }

    public List<TagMatchRule> getTagMatchRules() {
        return tagMatchRules;
    }

    @DataBoundSetter
    public CreateDeploymentStep setTagMatchRules(List<TagMatchRule> tagMatchRules) {
        this.tagMatchRules = tagMatchRules;
        return this;
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "createDynatraceDeploymentEvent";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "create Dynatrace Deployment event";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(TaskListener.class, EnvVars.class);
        }

        @Nonnull
        @Restricted(NoExternalUse.class)
        public ListBoxModel doFillEnvIdItems(@AncestorInPath Item item) {
            if (PerfSigUIUtils.checkForMissingPermission(item)) {
                return new ListBoxModel();
            }
            return DynatraceUtils.listToListBoxModel(DynatraceUtils.getDynatraceConfigurations());
        }

        @Override
        public CreateDeploymentStep newInstance(final Map<String, Object> arguments) throws Exception {
            CreateDeploymentStep step = (CreateDeploymentStep) super.newInstance(arguments);
            if (CollectionUtils.isEmpty(step.getEntityIds()) && CollectionUtils.isEmpty(step.getTagMatchRules())) {
                throw new IllegalArgumentException("At least one of entityIds or tag match rules needs to be provided to " + getFunctionName());
            }
            return step;
        }

        public DescriptorExtensionList<EntityId, Descriptor<EntityId>> getEntityIdTypes() {
            return EntityId.EntityIdDescriptor.all();
        }
    }
}

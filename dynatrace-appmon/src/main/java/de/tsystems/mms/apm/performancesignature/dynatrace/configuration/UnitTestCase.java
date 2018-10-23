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

package de.tsystems.mms.apm.performancesignature.dynatrace.configuration;

import hudson.Extension;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.List;

public class UnitTestCase extends ConfigurationTestCase {

    @DataBoundConstructor
    public UnitTestCase(final String name, final List<Dashboard> singleDashboards, final List<Dashboard> comparisonDashboards, final String xmlDashboard) {
        super(name, singleDashboards, comparisonDashboards, xmlDashboard);
    }

    @Override
    @DataBoundSetter
    public void setClientDashboard(final String clientDashboard) {
        super.setClientDashboard(clientDashboard);
    }

    @Extension
    public static final class DescriptorImpl extends ConfigurationTestCaseDescriptor {
        @Nonnull
        @Override
        public String getDisplayName() {
            return Messages.UnitTestCase_DisplayName();
        }
    }

}

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

import hudson.model.InvisibleAction;

import java.util.Date;

public class PerfSigEnvInvisAction extends InvisibleAction {
    private final String testRunId;
    private final String testCase;
    private final Date timeframeStart;
    private final String sessionName;
    private String sessionId;
    private Date timeframeStop;

    PerfSigEnvInvisAction(final String testCase, final String sessionId, final String sessionName, final Date timeframeStart, final String testRunId) {
        this.sessionId = sessionId;
        this.timeframeStart = timeframeStart == null ? null : (Date) timeframeStart.clone();
        this.testCase = testCase;
        this.testRunId = testRunId;
        this.sessionName = sessionName;
    }

    public String getSessionName() {
        return sessionName;
    }

    public String getSessionId() {
        return sessionId;
    }

    void setSessionId(final String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTestRunId() {
        return testRunId;
    }

    public String getTestCase() {
        return testCase;
    }

    public Date getTimeframeStart() {
        return timeframeStart == null ? null : (Date) timeframeStart.clone();
    }

    public Date getTimeframeStop() {
        return timeframeStop == null ? null : (Date) timeframeStop.clone();
    }

    public boolean isContinuousRecording() {
        return (sessionId == null);
    }

    void setTimeframeStop(Date timeframeStop) {
        this.timeframeStop = timeframeStop;
    }
}

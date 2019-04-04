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

package de.tsystems.mms.apm.performancesignature.dynatrace.rest.json.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

import static de.tsystems.mms.apm.performancesignature.ui.util.PerfSigUIUtils.toIndentedString;

/**
 * ExternalTest
 */

public class ExternalTest {
    @SerializedName("testName")
    private final String testName = null;
    @SerializedName("timestamp")
    private final String timestamp = null;
    @SerializedName("measures")
    private final List<ExternalTestMeasure> measures = new ArrayList<>();

    /**
     * Get testName
     *
     * @return testName
     **/
    @ApiModelProperty(required = true)
    public String getTestName() {
        return testName;
    }

    /**
     * Timestamp in ISO 8601 compatible date/time of format: yyyy-MM-dd&#39;T&#39;HH:mm:ss.SSSXXX
     *
     * @return timestamp
     **/
    @ApiModelProperty(example = "2016-05-11T11:35:31.170+02:00", value = "Timestamp in ISO 8601 compatible date/time of format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Get measures
     *
     * @return measures
     **/
    @ApiModelProperty(required = true)
    public List<ExternalTestMeasure> getMeasures() {
        return measures;
    }

    @Override
    public String toString() {

        return "class ExternalTest {\n" +
                "    testName: " + toIndentedString(testName) + "\n" +
                "    timestamp: " + toIndentedString(timestamp) + "\n" +
                "    measures: " + toIndentedString(measures) + "\n" +
                "}";
    }
}



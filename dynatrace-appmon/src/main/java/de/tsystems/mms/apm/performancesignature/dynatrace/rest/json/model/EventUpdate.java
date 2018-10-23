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
import de.tsystems.mms.apm.performancesignature.ui.util.PerfSigUIUtils;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

import static de.tsystems.mms.apm.performancesignature.dynatrace.model.Alert.SeverityEnum;
import static de.tsystems.mms.apm.performancesignature.dynatrace.model.Alert.StateEnum;

/**
 * Event update description
 */
@ApiModel(description = "Event update description")

public class EventUpdate {
    @SerializedName("severity")
    private SeverityEnum severity;
    @SerializedName("state")
    private StateEnum state;
    @SerializedName("message")
    private String message;
    @SerializedName("description")
    private String description;
    @SerializedName("start")
    private Date start;
    @SerializedName("end")
    private final Date end;

    public EventUpdate(Date end) {
        this.end = end == null ? null : (Date) end.clone();
    }

    /**
     * The severity of the event
     *
     * @return severity
     **/
    @ApiModelProperty(value = "The severity of the event")
    public SeverityEnum getSeverity() {
        return severity;
    }

    /**
     * The state of the event
     *
     * @return state
     **/
    @ApiModelProperty(value = "The state of the event")
    public StateEnum getState() {
        return state;
    }

    /**
     * Get message
     *
     * @return message
     **/

    public String getMessage() {
        return message;
    }

    /**
     * Get description
     *
     * @return description
     **/

    public String getDescription() {
        return description;
    }

    /**
     * Start time in ISO 8601 compatible date/time of format: yyyy-MM-dd&#39;T&#39;HH:mm:ss.SSSXXX
     *
     * @return start
     **/
    @ApiModelProperty(example = "2016-05-11T11:35:31.170+02:00", value = "Start time in ISO 8601 compatible date/time of format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    public Date getStart() {
        return start == null ? null : (Date) start.clone();
    }

    /**
     * End time in ISO 8601 compatible date/time of format: yyyy-MM-dd&#39;T&#39;HH:mm:ss.SSSXXX
     *
     * @return end
     **/
    @ApiModelProperty(example = "2016-05-11T11:35:31.170+02:00", value = "End time in ISO 8601 compatible date/time of format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    public Date getEnd() {
        return end == null ? null : (Date) end.clone();
    }

    @Override
    public String toString() {
        return "class EventUpdate {\n" +
                "    severity: " + PerfSigUIUtils.toIndentedString(severity) + "\n" +
                "    state: " + PerfSigUIUtils.toIndentedString(state) + "\n" +
                "    message: " + PerfSigUIUtils.toIndentedString(message) + "\n" +
                "    description: " + PerfSigUIUtils.toIndentedString(description) + "\n" +
                "    start: " + PerfSigUIUtils.toIndentedString(start) + "\n" +
                "    end: " + PerfSigUIUtils.toIndentedString(end) + "\n" +
                "}";
    }
}

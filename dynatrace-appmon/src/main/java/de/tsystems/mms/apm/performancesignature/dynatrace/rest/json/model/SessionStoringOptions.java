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
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

/**
 * SessionStoringOptions
 */

public class SessionStoringOptions {
    @SerializedName("sessionname")
    private final String sessionname;
    @SerializedName("description")
    private final String description;
    @SerializedName("appendtimestamp")
    private final boolean appendtimestamp;
    @SerializedName("recordingoption")
    private final SessionRecordingOptions.RecordingoptionEnum recordingoption;
    @SerializedName("locksession")
    private final boolean locksession;
    @SerializedName("timeframestart")
    private final Date timeframestart;
    @SerializedName("timeframeend")
    private final Date timeframeend;

    public SessionStoringOptions(String sessionname, String description, boolean appendtimestamp, String recordingoption, boolean locksession, Date timeframestart, Date timeframeend) {
        this.sessionname = sessionname;
        this.description = description;
        this.appendtimestamp = appendtimestamp;
        this.recordingoption = SessionRecordingOptions.RecordingoptionEnum.fromValue(recordingoption);
        this.locksession = locksession;
        this.timeframestart = (Date) timeframestart.clone();
        this.timeframeend = (Date) timeframeend.clone();
    }

    /**
     * User-readable presentable name for the session to be stored
     *
     * @return sessionname
     **/
    @ApiModelProperty(value = "User-readable presentable name for the session to be stored")
    public String getSessionname() {
        return sessionname;
    }

    /**
     * Description for the session to be stored
     *
     * @return description
     **/
    @ApiModelProperty(value = "Description for the session to be stored")
    public String getDescription() {
        return description;
    }

    /**
     * true to append timestamp information to recorded session name, otherwise false (default is false)
     *
     * @return appendtimestamp
     **/
    @ApiModelProperty(example = "false", value = "true to append timestamp information to recorded session name, otherwise false (default is false)")
    public Boolean getAppendtimestamp() {
        return appendtimestamp;
    }

    /**
     * Recording option, possible values: &#39;all&#39;, &#39;violations&#39;, &#39;timeseries&#39;
     *
     * @return recordingoption
     **/
    @ApiModelProperty(value = "Recording option, possible values: 'all', 'violations', 'timeseries'")
    public SessionRecordingOptions.RecordingoptionEnum getRecordingoption() {
        return recordingoption;
    }

    /**
     * true to lock session, otherwise false (default is false)
     *
     * @return locksession
     **/
    @ApiModelProperty(example = "false", value = "true to lock session, otherwise false (default is false)")
    public Boolean getLocksession() {
        return locksession;
    }

    /**
     * Start time of the interval during which the data will be stored, in ISO 8601 compatible date/time of format: yyyy-MM-dd&#39;T&#39;HH:mm:ss.SSSXXX
     *
     * @return timeframestart
     **/
    @ApiModelProperty(example = "2016-05-11T11:35:31.170+02:00", value = "Start time of the interval during which the data will be stored, in ISO 8601 compatible date/time of format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    public Date getTimeframestart() {
        return timeframestart == null ? null : (Date) timeframestart.clone();
    }

    /**
     * End time of the interval during which the data will be stored, in ISO 8601 compatible date/time of format: yyyy-MM-dd&#39;T&#39;HH:mm:ss.SSSXXX
     *
     * @return timeframeend
     **/
    @ApiModelProperty(example = "2016-05-11T11:35:31.170+02:00", value = "End time of the interval during which the data will be stored, in ISO 8601 compatible date/time of format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    public Date getTimeframeend() {
        return timeframeend == null ? null : (Date) timeframeend.clone();
    }

    @Override
    public String toString() {
        return "class SessionStoringOptions {\n" +
                "    sessionname: " + PerfSigUIUtils.toIndentedString(sessionname) + "\n" +
                "    description: " + PerfSigUIUtils.toIndentedString(description) + "\n" +
                "    appendtimestamp: " + PerfSigUIUtils.toIndentedString(appendtimestamp) + "\n" +
                "    recordingoption: " + PerfSigUIUtils.toIndentedString(recordingoption) + "\n" +
                "    locksession: " + PerfSigUIUtils.toIndentedString(locksession) + "\n" +
                "    timeframestart: " + PerfSigUIUtils.toIndentedString(timeframestart) + "\n" +
                "    timeframeend: " + PerfSigUIUtils.toIndentedString(timeframeend) + "\n" +
                "}";
    }
}

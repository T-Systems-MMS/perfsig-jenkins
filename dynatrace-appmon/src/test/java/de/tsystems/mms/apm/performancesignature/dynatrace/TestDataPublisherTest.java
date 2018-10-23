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

import com.google.gson.Gson;
import de.tsystems.mms.apm.performancesignature.dynatrace.model.TestRun;
import hudson.FilePath;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestDataPublisherTest {

    @Test
    public void testJSONParser() throws IOException, InterruptedException {
        FilePath file = new FilePath(new File("src/test/resources/sampleTestRun.json"));
        TestRun testRun = new Gson().fromJson(file.readToString(), TestRun.class);

        assertEquals(305, testRun.getTestResults().size());
    }
}

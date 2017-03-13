/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.kubernetes.api.pipelines;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 */
public class JobEnvironmentTest {
    @Test
    public void testLoadJobEnvironment() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("BRANCH_NAME", "master");
        env.put("BUILD_ID", "2");
        env.put("JOB_NAME", "rawlingsj/spring-boot-webmvc/master");
        env.put("GIT_URL", "https://github.com/jstrachan/demo230.git");

        JobEnvironment jobEnvironment = JobEnvironment.create(env);

        assertEquals("getBranchName()", "master", jobEnvironment.getBranchName());
        assertEquals("getBuildId()", "2", jobEnvironment.getBuildId());
        assertEquals("getJobName()", "rawlingsj/spring-boot-webmvc/master", jobEnvironment.getJobName());
        assertEquals("getGitUrl()", "https://github.com/jstrachan/demo230.git", jobEnvironment.getGitUrl());
    }

}

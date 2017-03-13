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

import static org.junit.Assert.assertEquals;

/**
 */
public class PipelinesTest {
    public static void assertJobName(PipelineConfiguration configuration, JobEnvironment jobEnvironment, String jobName, PipelineKind expectedKind) {
        Pipeline pipeline = configuration.getPipeline(jobEnvironment);
        assertEquals("jobName", jobName, pipeline.getJobName());
        assertEquals("pipelineKind", expectedKind, pipeline.getKind());
    }

    public static void assertJobName(PipelineConfiguration configuration, String jobName, String branchName, PipelineKind expectedKind) {
        JobEnvironment jobEnvironment = createJobEnvironment(jobName, branchName);
        assertJobName(configuration, jobEnvironment, jobName, expectedKind);
    }

    public static void assertJobName(PipelineConfiguration configuration, String jobName, String branchName, String gitUrl, PipelineKind expectedKind) {
        JobEnvironment jobEnvironment = createJobEnvironment(jobName, branchName, gitUrl);
        assertJobName(configuration, jobEnvironment, jobName, expectedKind);
    }

    public static JobEnvironment createJobEnvironment(String jobName, String branchName, String gitUrl) {
        JobEnvironment answer = createJobEnvironment(jobName, branchName);
        answer.setGitUrl(gitUrl);
        return answer;
    }

    public static JobEnvironment createJobEnvironment(String jobName, String branchName) {
        JobEnvironment answer = new JobEnvironment();
        answer.setJobName(jobName);
        answer.setBranchName(branchName);
        return answer;
    }

    @Test
    public void testNotCDBuildsByDefault() throws Exception {
        PipelineConfiguration configuration = PipelineConfiguration.createDefault();
        assertJobName(configuration, "whatnot", "master", PipelineKind.Developer);

        // match CI if branch pattern matches
        assertJobName(configuration, "whatnot", null, PipelineKind.Developer);
        assertJobName(configuration, "whatnot", "PR-123", PipelineKind.CI);
    }

    @Test
    public void testEnableCDOnNamedBuilds() throws Exception {
        PipelineConfiguration configuration = PipelineConfiguration.createDefault().setJobNamesCD("foo", "bar");

        assertJobName(configuration, "foo", "master", PipelineKind.CD);
        assertJobName(configuration, "bar", "master", PipelineKind.CD);

        // otherwise is a developer build
        assertJobName(configuration, "whatnot", "master", PipelineKind.Developer);
    }

    @Test
    public void testEnableCDBuildsForOrganisation() throws Exception {
        PipelineConfiguration configuration = PipelineConfiguration.createDefault().setCDGitOrganisation("github.com/fabric8io", "master").setJobNamesDeveloper("whatnot");

        assertJobName(configuration, "foo", "master", "git@github.com:fabric8io/foo.git", PipelineKind.CD);
        assertJobName(configuration, "foo", "master", "https://github.com/fabric8io/foo.git", PipelineKind.CD);

        assertJobName(configuration, "bar", "master", "https://github.com/bar/foo.git", PipelineKind.Developer);
        assertJobName(configuration, "bar", "master", "git@github.com:bar/foo.git", PipelineKind.Developer);

        // lets show we can opt out of CD pipelines for specific builds in an organisation if required
        assertJobName(configuration, "whatnot", "master", "https://github.com/fabric8io/whatnot.git", PipelineKind.Developer);
    }
}

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.testing.jenkins;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.JobWithDetails;
import io.fabric8.utils.Asserts;
import io.fabric8.utils.Block;
import io.fabric8.utils.Millis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Helper class for assertions relating to jenkins jobs
 */
public class JenkinsAsserts {
    private static final transient Logger LOG = LoggerFactory.getLogger(JenkinsAsserts.class);
    private static long defaultBuildWaitTime = Millis.minutes(2);

    public static JenkinsServer createJenkinsServer(String url) throws URISyntaxException {
        return new JenkinsServer(new URI(url));
    }

    public static JobWithDetails assertJobExists(JenkinsServer jenkins, String jobName) {
        JobWithDetails job = tryFindJob(jenkins, jobName);
        if (job != null) {
            return job;
        }
        fail("No job found called `" + jobName + "` for jenkins at " + jenkins);
        return job;
    }

    public static void assertJobLastBuildIsSuccessful(final JenkinsServer jenkins, final String jobName) throws Exception {
        assertJobLastBuildIsSuccessful(defaultBuildWaitTime, jenkins, jobName);
    }

    public static void assertJobLastBuildIsSuccessful(long timeMillis, final JenkinsServer jenkins, final String jobName) throws Exception {
        Asserts.assertWaitFor(timeMillis, new Block() {
            @Override
            public void invoke() throws Exception {
                JobWithDetails job = assertJobExists(jenkins, jobName);
                Build lastBuild = job.getLastBuild();
                assertNotNull("No lastBuild for job `" + jobName + "`", lastBuild);
                System.out.println("Last build of `" + jobName + "` at " + lastBuild.getUrl());
                Build lastSuccessfulBuild = job.getLastSuccessfulBuild();
                assertNotNull("No lastSuccessfulBuild for job `" + jobName + "` at: " + lastBuild.getUrl(), lastSuccessfulBuild);
                assertEquals("Last successful build number was not the last build number: " + lastBuild.getUrl(), lastBuild.getNumber(), lastSuccessfulBuild.getNumber());
                System.out.println("Successful build of `" + jobName + "` at " + lastSuccessfulBuild.getUrl());
            }
        });
    }

    public static Build assertJobHasBuild(JenkinsServer jenkins, String jobName) {
        JobWithDetails job = assertJobExists(jenkins, jobName);
        Build lastBuild = job.getLastBuild();
        assertNotNull("No lastBuild for job `" + jobName + "`", lastBuild);
        return lastBuild;
    }

    protected static JobWithDetails tryFindJob(JenkinsServer jenkins, String jobName) {
        for (int i = 0; i < 15; i++) {
            try {
                return jenkins.getJob(jobName);
            } catch (IOException e) {
                LOG.info("Caught: " + e, e);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e1) {
                    // ignore
                }
            }
        }
        return null;
    }

}

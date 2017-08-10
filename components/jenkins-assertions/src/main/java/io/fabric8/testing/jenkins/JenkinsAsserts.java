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

import com.google.common.base.Optional;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.FolderJob;
import com.offbytwo.jenkins.model.Job;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueItem;
import com.offbytwo.jenkins.model.QueueReference;
import io.fabric8.utils.Asserts;
import io.fabric8.utils.Block;
import io.fabric8.utils.Millis;
import io.fabric8.utils.Strings;
import io.fabric8.utils.XmlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Helper class for assertions relating to jenkins jobs
 */
public class JenkinsAsserts {
    public static final String INDENT = "  ";
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

    public static void assertCreateJenkinsJob(JenkinsServer jenkinsServer, String xml, String jobName) {
        try {
            jenkinsServer.createJob(jobName, xml);
        } catch (IOException e) {
            fail("Failed to create Jenkins job " + jobName + " for XML `" + xml + "`. " + e, e);
        }
    }

    /**
     * Asserts that the Job exists and returns its XML
     */
    public static String assertJobXml(JenkinsServer jenkinsServer, String jobName) {
        try {
            return jenkinsServer.getJobXml(jobName);
        } catch (IOException e) {
            fail("Failed to find XML for Jenkins job " + jobName + ". " + e, e);
            return null;
        }
    }

    /**
     * Returns the Job XML for a Pipeline job for the inline jenkinsfile
     */
    public static String createJenkinsPipelineJobXml(String jenkinsfile) {
        return
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><org.jenkinsci.plugins.workflow.job.WorkflowJob plugin=\"workflow-job@1.15\">\n" +
                        "  <keepDependencies>false</keepDependencies>\n" +
                        "  <properties/>\n" +
                        "  <definition class=\"org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition\" plugin=\"workflow-cps@1.15\">\n" +
                        "    <script>" + jenkinsfile + "</script>\n" +
                        "    <sandbox>false</sandbox>\n" +
                        "  </definition>\n" +
                        "  <triggers/>\n" +
                        "</org.jenkinsci.plugins.workflow.job.WorkflowJob>";
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


    public static void deleteAllCurrentJobs(JenkinsServer jenkins) throws IOException {
        int numberOfAttempts = 2;

        for (int i = 1; i < numberOfAttempts; i++) {
            if (i > 1) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
            Map<String, Job> jobs = jenkins.getJobs();

            Set<Map.Entry<String, Job>> entries = jobs.entrySet();
            for (Map.Entry<String, Job> entry : entries) {
                String jobName = entry.getKey();
                Job job = entry.getValue();
                LOG.info("Deleting job " + jobName);
                try {
                    jenkins.deleteJob(jobName, true);
                } catch (IOException e) {
                    LOG.warn("Failed to delete job: " + jobName + ". " + e, e);
                }
            }


            if (numberOfJobs(jenkins) == 0) {
                return;
            }
        }
    }

    public static int numberOfJobs(JenkinsServer jenkins) throws IOException {
        return jenkins.getJobs().size();
    }

    public static void displayJobs(JenkinsServer jenkins) throws IOException {
        displayJobs(jenkins, jenkins.getJobs(), INDENT);
    }

    public static void displayJobs(JenkinsServer jenkins, Map<String, Job> jobs, String indent) throws IOException {
        Set<Map.Entry<String, Job>> entries = jobs.entrySet();
        for (Map.Entry<String, Job> entry : entries) {
            String jobName = entry.getKey();
            Job job = entry.getValue();
            String suffix = "";
            JobWithDetails details = job.details();
            if (details != null) {
                Build lastBuild = details.getLastBuild();
                if (lastBuild != null) {
                    BuildWithDetails buildDetails = lastBuild.details();
                    if (buildDetails != null) {
                        String buildId = buildDetails.getId();
                        if (buildId != null) {
                            suffix = ": #" + buildId;
                        }
                    }
                }
            }
            System.out.println(indent + jobName + suffix);
            Optional<FolderJob> optional = jenkins.getFolderJob(job);
            if (optional.isPresent()) {
                FolderJob folderJob = optional.get();
                Map<String, Job> children = folderJob.getJobs();
                displayJobs(jenkins, children, indent + INDENT);
            }
        }
    }

    /**
     * Asserts that a job exists with the given path
     */
    public static JobWithDetails assertJobPathExists(JenkinsServer jenkins, String... jobPath) throws IOException {
        JobWithDetails job = findJobPath(jenkins, jobPath);
        assertNotNull("Could not find Jenkins Job: " + fullJobPath(jobPath), job);
        LOG.info("Found job " + job.getUrl());
        return job;
    }

    /**
     * Asserts that we can trigger the job defined by the given path
     */
    public static QueueReference assertTriggerJobPath(JenkinsServer jenkins, String... jobPath) throws IOException {
        JobWithDetails jobWithDetails = assertJobPathExists(jenkins, jobPath);
        QueueReference build = jobWithDetails.build(true);
        assertNotNull("No build reference for job " + fullJobPath(jobPath), build != null);
        return build;
    }

    /**
     * Asserts that we can trigger the job defined by the given path
     */
    public static void assertWaitForNoRunningBuilds(JenkinsServer jenkins, long timeMillis) throws Exception {
        LOG.info("Waiting for no running Jenkins jobs");
        Asserts.assertWaitFor(timeMillis, new Block() {
            @Override
            public void invoke() throws Exception {
                List<QueueItem> items = jenkins.getQueue().getItems();
                assertTrue("Waiting for build queue to be empty but has " + items.size(), items.isEmpty());
            }
        });
    }

    /**
     * Waits for the given time period for the given job path to exist in Jenkins
     *
     * @return the job details
     */
    public static JobWithDetails assertWaitForJobPathExists(final JenkinsServer jenkins, long timeMillis, String... jobPath) throws Exception {
        final AtomicReference<JobWithDetails> holder = new AtomicReference<>(null);
        LOG.info("Waiting for Jenkins job " + fullJobPath(jobPath));
        Asserts.assertWaitFor(timeMillis, new Block() {
            @Override
            public void invoke() throws Exception {
                holder.set(assertJobPathExists(jenkins, jobPath));
            }
        });
        return holder.get();
    }

    public static void assertWaitForJobPathNotExist(final JenkinsServer jenkins, long timeMillis, String... jobPath) throws Exception {
        final String fullPath = fullJobPath(jobPath);
        LOG.info("Waiting for Jenkins job to no longer exist " + fullPath);
        Asserts.assertWaitFor(timeMillis, new Block() {
            @Override
            public void invoke() throws Exception {
                assertTrue("Jenkins job " + fullPath + " should not exist", findJobPath(jenkins, jobPath) == null);
            }
        });
    }

    public static String fullJobPath(String[] jobPath) {
        return Strings.join("/", jobPath);
    }

    /**
     * Tries to find a job via the given path
     */
    public static JobWithDetails findJobPath(JenkinsServer jenkins, String... jobPath) throws IOException {
        FolderJob folder = null;
        for (int i = 0, size = jobPath.length; i < size; i++) {
            String path = jobPath[i];
            if (size == 1 && i == 0) {
                return jenkins.getJob(path);
            }
            if (folder == null) {
                JobWithDetails jobDetails = jenkins.getJob(path);
                if (jobDetails == null) {
                    return null;
                }
                Job job = new Job(jobDetails.getName(), jobDetails.getUrl());
                Optional<FolderJob> optional = jenkins.getFolderJob(job);
                if (!optional.isPresent()) {
                    return null;
                }
                folder = optional.get();
                continue;
            }
            Job job = folder.getJob(path);
            if (job == null) {
                return null;
            }
            if (i == size - 1) {
                return job.details();
            } else {
                Optional<FolderJob> optional = jenkins.getFolderJob(job);
                if (!optional.isPresent()) {
                    return null;
                }
                folder = optional.get();
            }
        }
        return null;
    }


    public static Document assertJobXmlDocument(JenkinsServer jenkins, String jobName) {
        JenkinsAsserts.assertJobExists(jenkins, jobName);
        String xml;
        try {
            xml = jenkins.getJobXml(jobName);
        } catch (IOException e) {
            throw new AssertionError("Failed to load job XML for " + jobName + " due to " + e, e);
        }
        try {
            return XmlUtils.parseDoc(xml);
        } catch (Exception e) {
            throw new AssertionError("Failed to load parse XML for " + jobName + " due to " + e, e);
        }
    }
}

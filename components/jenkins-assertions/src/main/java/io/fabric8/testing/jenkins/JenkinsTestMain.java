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
import com.offbytwo.jenkins.model.Job;

import java.util.Map;
import java.util.Set;

/**
 * A simple class to test asserts on jenkins
 */
public class JenkinsTestMain {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: [jenkinsServerUrl] [jobName]");
            return;
        }
        String jenkinsUrl = "http://jenkins.vagrant.f8/";
        String job = null;
        if (args.length > 0) {
            jenkinsUrl = args[0];
        }
        if (args.length > 1) {
            job = args[1];
        }

        try {
            JenkinsServer jenkins = JenkinsAsserts.createJenkinsServer(jenkinsUrl);

            Map<String, Job> jobs = jenkins.getJobs();
            Set<Map.Entry<String, Job>> entries = jobs.entrySet();
            for (Map.Entry<String, Job> entry : entries) {
                System.out.println("Job " + entry.getKey() + " = " + entry.getValue());
            }

            if (job != null) {
                JenkinsAsserts.assertJobLastBuildIsSuccessful(jenkins, job);
            }
        } catch (Exception e) {
            logError(e.getMessage(), e);
        }

    }

    public static void logError(String message, Throwable e) {
        System.out.println("ERROR: " + message + e);
        e.printStackTrace();
        Throwable cause = e.getCause();
        if(cause != null && cause != e) {
            logError("Caused by: ", cause);
        }

    }

}

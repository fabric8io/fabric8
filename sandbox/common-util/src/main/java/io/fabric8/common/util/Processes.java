/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static java.lang.String.format;

/**
 */
public class Processes {
    private static final transient Logger LOG = LoggerFactory.getLogger(Processes.class);

    /**
     * Returns true if the given PID is still alive
     */
    public static boolean isProcessAlive(long pid) {
        List<Long> processIds = getProcessIds();
        if (processIds.isEmpty()) {
            // we must be on a platform that the PID list doesn't work like windows
            return true;
        }
        return processIds.contains(pid);
    }

    /**
     * Returns the list of current active PIDs on a platform that supports such a thing (e.g. unix)
     */
    public static List<Long> getProcessIds() {
        // TODO we should use a nice library like Sigar really
        // here's a simple unix only workaround for now...
        String commands = "ps -e";
        String message = commands;
        LOG.debug("Executing commands: " + message);
        List<Long> answer = new ArrayList<Long>();
        try {
            Process process = Runtime.getRuntime().exec(commands);
            parseProcesses(process.getInputStream(), answer, message, Filters.<String>trueFilter());
            processErrors(process.getErrorStream(), message);
        } catch (Exception e) {
            LOG.error("Failed to execute process " + "stdin" + " for " +
                    message +
                    ": " + e, e);
        }
        return answer;
    }

    /**
     * Returns the list of current active PIDs for any java based process
     * that has a main class which contains any of the given bits of text
     *
     */
    public static List<Long> getJavaProcessIds(String... classNameFilter) {
        String commands = "jps -l";
        String message = commands;
        LOG.debug("Executing commands: " + message);
        List<Long> answer = new ArrayList<Long>();
        Filter<String> filter = Filters.containsAnyString(classNameFilter);
        try {
            Process process = Runtime.getRuntime().exec(commands);
            parseProcesses(process.getInputStream(), answer, message, filter);
            processErrors(process.getErrorStream(), message);
        } catch (Exception e) {
            LOG.error("Failed to execute process " + "stdin" + " for " +
                    message +
                    ": " + e, e);
        }
        return answer;
    }

    /**
     * Kills all commonly created Java based processes created by fabric8 and its unit tests.
     *
     * This is handy for unit testing to ensure there's no stray karaf, wildfly, tomcat or java containers running.
     */
    public static int killJavaProcesses() {
        return killJavaProcesses("karaf", "jboss", "catalina", "spring.Main", "FabricSpringApplication");
    }

    /**
     * Kills all java processes found which include the classNameFilter in their main class
     */
    public static int killJavaProcesses(String... classNameFilters) {
        int count = 0;
        List<Long> javaProcessIds = getJavaProcessIds(classNameFilters);
        for (Long processId : javaProcessIds) {
            // lets log to the console too as this tends to show up in the junit output
            System.out.println("WARNING: Killing Java process " + processId);
            LOG.warn("Killing Java process " + processId);
            killProcess(processId, "-9");
            count++;
        }
        return count;
    }

    /**
     * Returns a list of active docker containers
     */
    public static List<String> getDockerContainerIds() {
        String commands = "docker ps -q";
        String message = "output of command: " + commands;
        LOG.debug("Executing commands: " + message);
        final List<String> answer = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(commands);
            Function<String, Void> fn = new Function<String, Void>() {
                @Override
                public Void apply(String line) {
                    if (Strings.isNotBlank(line)) {
                        answer.add(line.trim());
                    }
                    return null;
                }
            };
            processOutput(process.getInputStream(), fn, message);
            processErrors(process.getErrorStream(), message);
        } catch (Exception e) {
            LOG.error("Failed to execute process " + "stdin" + " for " +
                    message +
                    ": " + e, e);
        }
        return answer;
    }

    /**
     * Returns a list of active docker containers
     */
    public static int killDockerContainer(String containerId) {
        // lets log to the console too as this tends to show up in the junit output
        System.out.println("Killing Docker container " + containerId);
        LOG.warn("WARNING: Killing Docker container " + containerId);

        String commands = "docker kill " + containerId;
        String message =commands;
        LOG.debug("Executing commands: " + message);
        final List<String> answer = new ArrayList<>();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(commands);
            processInput(process.getInputStream(), commands);
            processErrors(process.getErrorStream(), commands);
        } catch (Exception e) {
            LOG.error("Failed to execute process " + "stdin" + " for " +
                    message +
                    ": " + e, e);
        }
        return process != null ? process.exitValue() : -1;
    }

    /**
     * Kills all docker containers on the current host
     */
    public static void killDockerContainers() {
        // lets run this in a background thread in case the command blocks due to the docker daemon not running
        new Thread(new Runnable() {

            @Override
            public void run() {
                int count = 0;
                List<String> ids = getDockerContainerIds();
                for (String id : ids) {
                    if (killDockerContainer(id) == 0) {
                        count++;
                    }
                }
            }
        }).run();
    }

    /**
     * Attempts to kill the given process
     */
    public static int killProcess(Long pid, String params) {
        if (pid == null || !isProcessAlive(pid)) {
            return 0;
        }

        // TODO we should use a nice library like Sigar really
        // here's a simple unix only workaround for now...
        String commands = "kill " + (params != null ? params + " " : "") + pid;
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        LOG.debug("Executing commands: " + commands);
        try {
            process = runtime.exec(commands);
            processInput(process.getInputStream(), commands);
            processErrors(process.getErrorStream(), commands);
        } catch (Exception e) {
            LOG.error("Failed to execute process " + "stdin" + " for " +
                    commands +
                    ": " + e, e);
        }
        try {
            return process != null ? process.waitFor() : 1;
        } catch (InterruptedException e) {
            String message = format("Interrupted while waiting for 'kill %d ' command to finish", pid);
            throw new RuntimeException(message, e);
        }
    }

    protected static void parseProcesses(InputStream inputStream, List<Long> answer, String message, Filter<String> lineFilter) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                if (lineFilter.matches(line)) {
                    StringTokenizer tokenizer = new StringTokenizer(line);
                    if (tokenizer.hasMoreTokens()) {
                        String pidText = tokenizer.nextToken();
                        try {
                            long pid = Long.parseLong(pidText);
                            answer.add(pid);
                        } catch (NumberFormatException e) {
                            LOG.debug("Could not parse pid " + pidText + " from command: " + message);
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOG.debug("Failed to process stdin for " +
                    message +
                    ": " + e, e);
            throw e;
        } finally {
            Closeables.closeQuitely(reader);
        }
    }

    protected static void processInput(InputStream inputStream, String message) throws Exception {
        readProcessOutput(inputStream, "stdout for ", message);
    }

    protected static void processErrors(InputStream inputStream, String message) throws Exception {
        readProcessOutput(inputStream, "stderr for ", message);
    }

    protected static void readProcessOutput(InputStream inputStream, final String prefix, final String message) throws Exception {
        Function<String, Void> function = new Function<String, Void>() {
            @Override
            public Void apply(String line) {
                LOG.debug("Error " +
                        prefix +
                                message +
                        ": " + line);
                return null;
            }
        };
        processOutput(inputStream, function, prefix +
                message);
    }

    protected static void processOutput(InputStream inputStream, Function<String, Void> function, String errrorMessage) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                function.apply(line);
            }

        } catch (Exception e) {
            LOG.error("Failed to process " + errrorMessage + ": " + e, e);
            throw e;
        } finally {
            Closeables.closeQuitely(reader);
        }
    }
}

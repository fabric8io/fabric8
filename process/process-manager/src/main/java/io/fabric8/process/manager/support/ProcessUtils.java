/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.process.manager.support;

import com.google.common.collect.Maps;
import io.fabric8.api.Profile;
import io.fabric8.common.util.Closeables;
import io.fabric8.common.util.Filter;
import io.fabric8.common.util.Filters;
import io.fabric8.common.util.Function;
import io.fabric8.common.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;

import static java.lang.String.format;

public class ProcessUtils {
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ProcessUtils.class);


    /**
     * Lets find the install dir, which may be the root dir or could be a child directory (as typically untarring will create a new child directory)
     */
    public static File findInstallDir(File rootDir) {
        if (installExists(rootDir)) {
            return rootDir;
        }
        File[] files = rootDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (installExists(file)) {
                    return file;
                }
            }
        }
        return rootDir;
    }

    public static boolean installExists(File file) {
        if (file.isDirectory()) {
            File binDir = new File(file, "bin");
            return binDir.exists() && binDir.isDirectory();
        }
        return false;
    }

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
        LOGGER.debug("Executing commands: " + message);
        List<Long> answer = new ArrayList<Long>();
        try {
            Process process = Runtime.getRuntime().exec(commands);
            parseProcesses(process.getInputStream(), answer, message, Filters.<String>trueFilter());
            processErrors(process.getErrorStream(), message);
        } catch (Exception e) {
            LOGGER.error("Failed to execute process " + "stdin" + " for " +
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
        LOGGER.debug("Executing commands: " + message);
        List<Long> answer = new ArrayList<Long>();
        Filter<String> filter = Filters.containsAnyString(classNameFilter);
        try {
            Process process = Runtime.getRuntime().exec(commands);
            parseProcesses(process.getInputStream(), answer, message, filter);
            processErrors(process.getErrorStream(), message);
        } catch (Exception e) {
            LOGGER.error("Failed to execute process " + "stdin" + " for " +
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
        return killJavaProcesses("karaf", "jboss", "catalina", "spring.Main");
    }

    /**
     * Kills all java processes found which include the classNameFilter in their main class
     */
    public static int killJavaProcesses(String... classNameFilters) {
        int count = 0;
        List<Long> javaProcessIds = getJavaProcessIds(classNameFilters);
        for (Long processId : javaProcessIds) {
            LOGGER.warn("Killing Java process " + processId);
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
        LOGGER.debug("Executing commands: " + message);
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
            LOGGER.error("Failed to execute process " + "stdin" + " for " +
                    message +
                    ": " + e, e);
        }
        return answer;
    }


    /**
     * Returns a list of active docker containers
     */
    public static int killDockerContainer(String containerId) {
        String commands = "docker kill " + containerId;
        String message =commands;
        LOGGER.debug("Executing commands: " + message);
        final List<String> answer = new ArrayList<>();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(commands);
            processInput(process.getInputStream(), commands);
            processErrors(process.getErrorStream(), commands);
        } catch (Exception e) {
            LOGGER.error("Failed to execute process " + "stdin" + " for " +
                    message +
                    ": " + e, e);
        }
        return process != null ? process.exitValue() : -1;
    }

    /**
     * Kills all docker containers on the current host
     */
    public static int killDockerContainers() {
        int count = 0;
        List<String> ids = getDockerContainerIds();
        for (String id : ids) {
            LOGGER.warn("Killing Docker container " + id);
            if (killDockerContainer(id) == 0) {
                count++;
            }
        }
        return count;
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
        LOGGER.debug("Executing commands: " + commands);
        try {
            process = runtime.exec(commands);
            processInput(process.getInputStream(), commands);
            processErrors(process.getErrorStream(), commands);
        } catch (Exception e) {
            LOGGER.error("Failed to execute process " + "stdin" + " for " +
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
                            LOGGER.debug("Could not parse pid " + pidText + " from command: " + message);
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.debug("Failed to process stdin for " +
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
                LOGGER.debug("Error " +
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
            LOGGER.error("Failed to process " + errrorMessage + ": " + e, e);
            throw e;
        } finally {
            Closeables.closeQuitely(reader);
        }
    }


    public static Map<String, String> getProcessLayout(List<Profile> profiles, String layoutPath) {
        Map<String, String> answer = new HashMap<String, String>();
        for (Profile profile : profiles) {
            Map<String, String> map = getProcessLayout(profile, layoutPath);
            answer.putAll(map);
        }
        return answer;
    }

    public static Map<String, String> getProcessLayout(Profile profile, String layoutPath) {
        return ByteToStringValues.INSTANCE.apply(Maps.filterKeys(profile.getOverlay().getFileConfigurations(), new LayOutPredicate(layoutPath)));
    }

}

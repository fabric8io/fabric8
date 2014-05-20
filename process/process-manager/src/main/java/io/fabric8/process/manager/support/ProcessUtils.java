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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        String message = commands;
        LOGGER.debug("Executing commands: " + message);
        List<Long> answer = new ArrayList<Long>();
        try {
            process = runtime.exec(commands);
            parseProcesses(process.getInputStream(), answer, message);
            processErrors(process.getErrorStream(), message);
        } catch (Exception e) {
            LOGGER.error("Failed to execute process " + "stdin" + " for " +
                    message +
                    ": " + e, e);
        }
        return answer;
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

    protected static void parseProcesses(InputStream inputStream, List<Long> answer, String message) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
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

    protected static void readProcessOutput(InputStream inputStream, String prefix, String message) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                LOGGER.debug("Error " +
                        prefix +
                                message +
                        ": " + line);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to process " + prefix +
                    message + ": " + e, e);
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

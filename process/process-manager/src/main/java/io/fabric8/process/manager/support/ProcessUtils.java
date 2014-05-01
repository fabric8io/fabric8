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

import io.fabric8.common.util.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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

    protected static void processErrors(InputStream inputStream, String message) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                LOGGER.debug("Error from " + message + ": " + line);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to process stderr for " +
                    message +
                    ": " + e, e);
            throw e;
        } finally {
            Closeables.closeQuitely(reader);
        }
    }
}

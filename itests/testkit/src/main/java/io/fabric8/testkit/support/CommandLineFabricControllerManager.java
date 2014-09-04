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
package io.fabric8.testkit.support;

import io.fabric8.common.util.Closeables;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.IOHelpers;
import io.fabric8.common.util.Processes;
import io.fabric8.testkit.FabricAssertions;
import io.fabric8.testkit.FabricController;
import io.fabric8.testkit.jolokia.JolokiaFabricController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import static io.fabric8.common.util.Strings.join;
import static io.fabric8.testkit.FabricAssertions.waitForValidValue;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * An implementation of {@link io.fabric8.testkit.FabricControllerManager} which uses a binary distribution, unpacks it
 * and runs shell commands to create a fabric.
 */
public class CommandLineFabricControllerManager extends FabricControllerManagerSupport {

    private static final transient Logger LOG = LoggerFactory.getLogger(CommandLineFabricControllerManager.class);

    private File installDir;
    private String startFabricScriptName = "bin/fabric8-start";

    public CommandLineFabricControllerManager() {
    }

    @Override
    public FabricController createFabric() throws Exception {
        if (workDirectory == null) {
            workDirectory = createTempDirectory();
        }
        String version = System.getProperty("fabric8-version", "1.2.0-SNAPSHOT");
        String home = System.getProperty("user.home", "~");
        String repo = home + "/.m2/repository";
        File distro = new File(repo, "io/fabric8/fabric8-karaf/" + version + "/fabric8-karaf-" + version + ".tar.gz");
        FabricAssertions.assertFileExists(distro);

        installDir = new File(workDirectory, "fabric8-karaf-" + version);
        killInstanceProcesses(getInstancesFile());
        if (workDirectory.exists()) {
            Files.recursiveDelete(workDirectory);
        }
        workDirectory.mkdirs();

        executeCommand(workDirectory, "tar", "zxf", distro.getAbsolutePath());

        FabricAssertions.assertDirectoryExists(installDir);

        assertTrue("install dir does not exist: " + installDir.getAbsolutePath(), installDir.exists());
        assertTrue("install dir is not a directory: " + installDir.getAbsolutePath(), installDir.isDirectory());

        System.out.println("About to boot up the fabric8 at: " + installDir.getAbsolutePath());

        File shellScript = new File(installDir, startFabricScriptName);
        FabricAssertions.assertFileExists(shellScript);

        executeCommand(installDir, "./" + startFabricScriptName);

        final FabricController restApi = createFabricController();
        return restApi;
    }

    @Override
    public void destroy() throws Exception {
        if (installDir == null) {
            return;
        }
        boolean killProcesses = FabricAssertions.shouldKillProcessesAfterTestRun();
        if (!killProcesses) {
            String message = installDir == null ? "" : " at: " + installDir.getAbsolutePath();
            System.out.println("Not destroying the fabric" + message + " due to system property " + FabricAssertions.KILL_CONTAINERS_FLAG + " being " + System.getProperty(FabricAssertions.KILL_CONTAINERS_FLAG));
            return;
        }
        System.out.println("Destroying the fabric at: " + installDir.getAbsolutePath());

        File instancesFile = waitForInstancesFile(20 * 1000);
        killInstanceProcesses(instancesFile);
    }

    protected FabricController createFabricController() {
        //return new SimpleFabricRestApi();
        return new JolokiaFabricController();
    }

    protected File createTempDirectory() throws IOException {
        File tempFile = File.createTempFile("fabric8-testkit", ".dir");
        tempFile.delete();
        tempFile.mkdirs();
        return tempFile;
    }

    protected void killInstanceProcesses(File instancesFile) throws IOException {
        if (instancesFile != null && instancesFile.exists() && instancesFile.isFile()) {
            Properties properties = new Properties();
            properties.load(new FileInputStream(instancesFile));
            Set<Map.Entry<Object, Object>> entries = properties.entrySet();
            for (Map.Entry<Object, Object> entry : entries) {
                Object key = entry.getKey();
                if (key != null) {
                    String text = key.toString();
                    if (text.startsWith("item.") && text.endsWith(".pid")) {
                        Object value = entry.getValue();
                        if (value instanceof String) {
                            String pidText = value.toString();
                            Long pid = Long.parseLong(pidText);
                            if (pid != null) {
                                System.out.println("Killing process " + pid);
                                int status = Processes.killProcess(pid, "-9");
                                if (status != 0) {
                                    System.err.println("Failed to kill process " + pid + ". Got " + status);
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    protected File waitForInstancesFile(long timeout) throws Exception {
        if (installDir != null) {
            return waitForValidValue(timeout, new Callable<File>() {
                @Override
                public File call() throws Exception {
                    return getInstancesFile();
                }
            }, new FileExistsFilter());
        } else {
            return null;
        }
    }

    protected File getInstancesFile() {
        return new File(installDir, "instances/instance.properties");
    }


    protected String executeCommand(File workDir, String... commands) throws IOException {
        String errors = null;
        String answer = null;
        String message = join(asList(commands), " ");
        try {
            System.out.println("Executing " + message);
            ProcessBuilder builder = new ProcessBuilder().command(commands).directory(workDir);
            Map<String, String> env = builder.environment();
            Map<String, String> envVars = createChildEnvironmentVariables();
            env.putAll(envVars);
            logEnvironmentVariables(env);
            Process process = builder.start();
            answer = readProcessOutput(process.getInputStream(), message);
            errors = processErrors(process.getErrorStream(), message);
            int status = process.waitFor();
            assertEquals("Command " + message + "; " + answer + " Status", 0, status);
        } catch (Exception e) {
            fail("Failed to execute command " +
                    message +
                    ": " + e);
        }
        errors = errors.trim();
        if (errors.length() > 0) {
            fail("Command: " + message + " got errors: " + errors);
        }
        return answer;
    }

    protected void logEnvironmentVariables(Map<String, String> env) {
        if (LOG.isDebugEnabled()) {
            TreeMap<String, String> sorted = new TreeMap<String, String>(env);
            Set<Map.Entry<String, String>> entries = sorted.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                LOG.debug("Setting " + entry.getKey() + "=" + entry.getValue());
            }
        }
    }


    protected String readProcessOutput(InputStream inputStream, String message) throws Exception {
        return IOHelpers.readFully(inputStream);
    }

    protected String processErrors(InputStream inputStream, String message) throws Exception {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) break;
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(line);
                LOG.info(line);
            }
            return builder.toString();

        } catch (Exception e) {
            LOG.error("Failed to process stderr for " +
                    message +
                    ": " + e, e);
            throw e;
        } finally {
            Closeables.closeQuitely(reader);
        }
    }

}

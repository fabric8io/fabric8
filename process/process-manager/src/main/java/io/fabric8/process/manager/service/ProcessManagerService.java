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
package io.fabric8.process.manager.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.fabric8.common.util.Objects;
import io.fabric8.common.util.Strings;
import io.fabric8.process.manager.DownloadStrategy;
import io.fabric8.process.manager.InstallContext;
import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.config.ProcessConfig;
import io.fabric8.process.manager.support.JarInstaller;
import io.fabric8.process.manager.support.command.Command;
import io.fabric8.process.manager.support.command.Duration;
import io.fabric8.process.manager.InstallOptions;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.ProcessController;
import io.fabric8.process.manager.config.JsonHelper;
import io.fabric8.process.manager.support.DefaultProcessController;
import io.fabric8.process.manager.support.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.fabric8.process.manager.support.ProcessUtils.findInstallDir;

public class ProcessManagerService implements ProcessManagerServiceMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessManagerService.class);
    private static final String INSTALLED_BINARY = "install.bin";

    private Executor executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("fabric-process-manager-%s").build());
    private File storageLocation;
    private int lastId = 0;
    private final Duration untarTimeout = Duration.valueOf("1h");
    private final Duration postUnpackTimeout = Duration.valueOf("1h");
    private final Duration postInstallTimeout = Duration.valueOf("1h");
    private SortedMap<String, Installation> installations = Maps.newTreeMap();
    private final ObjectName objectName;

    private MBeanServer mbeanServer;

    public ProcessManagerService() throws MalformedObjectNameException {
        this(new File(System.getProperty("karaf.processes", System.getProperty("karaf.base") + File.separatorChar + "processes")));
    }

    public ProcessManagerService(File storageLocation) throws MalformedObjectNameException {
        this.storageLocation = storageLocation;
        this.objectName = new ObjectName("io.fabric8:type=LocalProcesses");
    }

    public void bindMBeanServer(MBeanServer mbeanServer) {
        unbindMBeanServer(this.mbeanServer);
        this.mbeanServer = mbeanServer;
        if (mbeanServer != null) {
            registerMBeanServer(mbeanServer);
        }
    }

    public void unbindMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            unregisterMBeanServer(mbeanServer);
            this.mbeanServer = null;
        }
    }

    public void registerMBeanServer(MBeanServer mbeanServer) {
        try {
            if (!mbeanServer.isRegistered(objectName)) {
                mbeanServer.registerMBean(this, objectName);
            }
        } catch (Exception e) {
            LOGGER.warn("An error occurred during mbean server registration: " + e, e);
        }
    }

    public void unregisterMBeanServer(MBeanServer mbeanServer) {
        if (mbeanServer != null) {
            try {
                ObjectName name = objectName;
                if (mbeanServer.isRegistered(name)) {
                    mbeanServer.unregisterMBean(name);
                }
            } catch (Exception e) {
                LOGGER.warn("An error occurred during mbean server registration: " + e, e);
            }
        }
    }

    public void init() throws Exception {
        // lets find the largest number in the current directory as we are on startup
        lastId = 0;
        File[] files = storageLocation.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    String name = file.getName();
                    if (name.startsWith(".")) {
                        LOGGER.debug("Ignoring deleted installation at folder " + name);
                        continue;
                    }
                    if (name.matches("\\d+")) {
                        try {
                            int id = Integer.parseInt(name);
                            if (id > lastId) {
                                lastId = id;
                            }
                        } catch (NumberFormatException e) {
                            // should never happen :)
                        }
                    }
                    // String url = "TODO";
                    ProcessConfig config = JsonHelper.loadProcessConfig(file);
                    createInstallation(name, findInstallDir(file), config);
                }
            }
        }

    }


    @Override
    public String toString() {
        return "ProcessManager(" + storageLocation + ")";
    }

    @Override
    public ImmutableList<Installation> listInstallations() {
        return ImmutableList.copyOf(installations.values());
    }

    @Override
    public ImmutableMap<String, Installation> listInstallationMap() {
        return ImmutableMap.copyOf(installations);
    }

    @Override
    public Installation getInstallation(String id) {
        return installations.get(id);
    }

    @Override
    public Installation install(final InstallOptions options, final InstallTask postInstall) throws Exception {
        @SuppressWarnings("serial")
        InstallTask installTask = new InstallTask() {
            @Override
            public void install(InstallContext installContext, ProcessConfig config, String id, File installDir) throws Exception {
                config.setName(options.getName());
                File archive = getDownloadStrategy(options).downloadContent(options.getUrl(), installDir);
                if (archive == null) {
                    archive = new File(installDir, INSTALLED_BINARY);
                }
                File nestedProcessDirectory = null;
                if (options.getExtractCmd() != null && archive.exists()) {
                    String extractCmd = options.getExtractCmd();
                    FileUtils.extractArchive(archive, installDir, extractCmd, untarTimeout, executor);
                    nestedProcessDirectory = findInstallDir(installDir);
                    exportInstallDirEnvVar(options, nestedProcessDirectory);
                    String[] postUnpackCmds = options.getPostUnpackCmds();
                    if (postUnpackCmds != null) {
                        for (String postUnpackCmd : postUnpackCmds) {
                            if (Strings.isNotBlank(postUnpackCmd)) {
                                String[] args = FileUtils.splitCommands(postUnpackCmd);
                                LOGGER.info("Running post unpack command " + Arrays.asList(args) + " in folder " + nestedProcessDirectory);
                                new Command(args)
                                        .addEnvironment(options.getEnvironment())
                                        .setDirectory(nestedProcessDirectory)
                                        .setTimeLimit(postUnpackTimeout)
                                        .execute(executor);
                            }
                        }
                    }
                    writeJvmConfig(new File(nestedProcessDirectory, "etc"), options.getJvmOptions());
                }
                if (postInstall != null) {
                    postInstall.install(installContext, config, id, installDir);
                }
                String[] postInstallCmds = options.getPostInstallCmds();
                if (postInstallCmds != null) {
                    for (String postInstallCmd : postInstallCmds) {
                        if (Strings.isNotBlank(postInstallCmd)) {
                            String[] args = FileUtils.splitCommands(postInstallCmd);
                            LOGGER.info("Running post install command " + Arrays.asList(args) + " in folder " + nestedProcessDirectory);
                            new Command(args)
                                    .addEnvironment(options.getEnvironment())
                                    .setDirectory(nestedProcessDirectory)
                                    .setTimeLimit(postInstallTimeout)
                                    .execute(executor);
                        }
                    }
                }
            }
        };
        return installViaScript(options, installTask);
    }

    protected DownloadStrategy getDownloadStrategy(InstallOptions options) {
        DownloadStrategy answer = options.getDownloadStrategy();
        if (answer == null) {
            answer = createDeafultDownloadStrategy();
        }
        return answer;
    }

    protected void exportInstallDirEnvVar(InstallOptions options, File nestedProcessDirectory) {
        options.getEnvironment().put("FABRIC8_PROCESS_INSTALL_DIR", nestedProcessDirectory.getAbsolutePath());
        substituteEnvironmentVariableExpressions(options.getEnvironment(), options.getEnvironment());
    }

    @Override
    public Installation installJar(final InstallOptions parameters, final InstallTask postInstall) throws Exception {
        @SuppressWarnings("serial")
        InstallTask installTask = new InstallTask() {
            @Override
            public void install(InstallContext installContext, ProcessConfig config, String id, File installDir) throws Exception {
                config.setName(parameters.getName());
                // lets untar the process launcher
                String resourceName = "process-launcher.tar.gz";
                final InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName);
                Preconditions.checkNotNull(in, "Could not find " + resourceName + " on the file system");
                File tmpFile = File.createTempFile("process-launcher", ".tar.gz");
                Files.copy(new InputSupplier<InputStream>() {
                    @Override
                    public InputStream getInput() throws IOException {
                        return in;
                    }
                }, tmpFile);
                FileUtils.extractArchive(tmpFile, installDir, "tar zxf", untarTimeout, executor);

                // lets generate the etc configs
                File etc = new File(installDir, "etc");
                if(etc.mkdirs()) {
                    LOGGER.debug("Creating etc directory {} of process {}.", etc, id);
                } else {
                    LOGGER.debug("Directory etc {} of process {} exists. Skipping.", etc, id);
                }
                Files.write("", new File(etc, "config.properties"), Charsets.UTF_8);
                writeJvmConfig(etc, parameters.getJvmOptions());

                JarInstaller installer = new JarInstaller(parameters, executor);
                installer.install(installContext, config, id, installDir);
                if (postInstall != null) {
                    postInstall.install(installContext, config, id, installDir);
                }
            }
        };
        return installViaScript(parameters, installTask);
    }

    @Override
    public void uninstall(Installation installation) {
        installation.getController().uninstall();
        installations.remove(installation.getId());
    }

    private void writeJvmConfig(File etc, String[] jvmOptions) throws IOException {
        File jvmConfigFile = new File(etc, "jvm.config");
        if (jvmConfigFile.exists() && jvmConfigFile.length() > 0) {
            LOGGER.debug("Non empty {} file exists. Skipping writing of the following jvmOptions: {}", jvmConfigFile, Arrays.toString(jvmOptions));
        } else {
            if (etc.exists() && etc.isDirectory()) {
                LOGGER.debug("Writing the following jvmOptions to the {} file: {}", jvmConfigFile, Arrays.toString(jvmOptions));
                Files.write(generateJvmConfig(jvmOptions), jvmConfigFile, Charsets.UTF_8);
            } else {
                LOGGER.debug("No etc directory exists at {} so not writing jvm.config", etc);
            }
        }
    }

    private String generateJvmConfig(String[] jvmOptions) {
        StringBuilder jvmConfig = new StringBuilder();
        if (jvmOptions != null) {
            for (String jvmOption : jvmOptions) {
                jvmConfig.append(jvmOption).append(" ");
            }
        }
        return jvmConfig.toString();
    }

    @Override
    public ProcessConfig loadProcessConfig(InstallOptions options) throws IOException {
        ProcessConfig config = loadControllerJson(options);
        Map<String, String> configEnv = config.getEnvironment();
        Map<String, String> optionsEnv = options.getEnvironment();
        if (optionsEnv != null) {
            configEnv.putAll(optionsEnv);
        }
        return config;
    }


    // Properties
    //-------------------------------------------------------------------------
    public File getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(File storageLocation) {
        this.storageLocation = storageLocation;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    // Implementation
    //-------------------------------------------------------------------------

    protected Installation installViaScript(InstallOptions options, InstallTask installTask) throws Exception {
        String id = createNextId(options);
        File installDir = createInstallDir(id);
        installDir.mkdirs();
        exportInstallDirEnvVar(options, installDir);

        ProcessConfig config = loadProcessConfig(options);
        InstallContext installContext = new InstallContext(options.getContainer(), installDir, false);
        installTask.install(installContext, config, id, installDir);
        JsonHelper.saveProcessConfig(config, installDir);
        installContext.updateContainerChecksums();

        Installation installation = createInstallation(id, installDir, config);
        installation.getController().install();
        return installation;
    }

    protected DownloadStrategy createDeafultDownloadStrategy() {
        return new DownloadStrategy() {
            @Override
            public File downloadContent(final URL sourceUrl, final File installDir) throws IOException {
                // copy the URL to the install dir
                File archive = new File(installDir, INSTALLED_BINARY);
                Files.copy(new InputSupplier<InputStream>() {
                    @Override
                    public InputStream getInput() throws IOException {
                        return sourceUrl.openStream();
                    }
                }, archive);
                return archive;
            }
        };
    }

    protected ProcessConfig loadControllerJson(InstallOptions options) throws IOException {
        String controllerJson = options.getControllerJson();
        URL controllerUrl = options.getControllerUrl();
        if (Strings.isNotBlank(controllerJson)) {
            return JsonHelper.loadProcessConfig(controllerJson);
        } else if (controllerUrl != null) {
            return JsonHelper.loadProcessConfig(controllerUrl);
        } else {
            return new ProcessConfig();
        }
    }

    /**
     * Returns the next process ID
     * @param options
     */
    protected synchronized String createNextId(InstallOptions options) {
        String id = options.getId();
        if (Strings.isNotBlank(id)) {
            return id;
        }

        // lets double check it doesn't exist already
        File dir;
        String answer = null;
        do {
            lastId++;
            answer = "" + lastId;
            dir = createInstallDir(answer);
        }
        while (dir.exists());
        return answer;
    }

    protected File createInstallDir(String id) {
        return new File(storageLocation, id);
    }


    protected Installation createInstallation(String id, File rootDir, ProcessConfig config) {
        // TODO we should support different kinds of controller based on the kind of installation
        // we could maybe discover a descriptor file to describe how to control the process?
        // or generate this file on installation time?

        File installDir = findInstallDir(rootDir);
        ProcessController controller = createController(id, config, rootDir, installDir);
        // TODO need to read the URL from somewhere...
        Installation installation = new Installation(id, installDir, controller, config);
        installations.put(id, installation);
        return installation;
    }

    protected ProcessController createController(String id, ProcessConfig config, File rootDir, File installDir) {
        return new DefaultProcessController(id, config, installDir);
    }

    // TODO. This is been ripped from io.fabric8.container.process.JolokiaAgentHelper.substituteEnvironmentVariableExpressions()
    // requires a refactoring to not introduce circular dependencies
    public static void substituteEnvironmentVariableExpressions(Map<String, String> map, Map<String, String> environmentVariables) {
        Set<Map.Entry<String, String>> envEntries = environmentVariables.entrySet();
        for (String key : map.keySet()) {
            String text = map.get(key);
            String oldText = text;
            if (Strings.isNotBlank(text)) {
                for (Map.Entry<String, String> envEntry : envEntries) {
                    String envKey = envEntry.getKey();
                    String envValue = envEntry.getValue();
                    if (Strings.isNotBlank(envKey) && Strings.isNotBlank(envValue)) {
                        text = text.replace("${env:" + envKey + "}", envValue);
                    }
                }
                if (!Objects.equal(oldText, text)) {
                    map.put(key, text);
                }
            }
        }
    }

}
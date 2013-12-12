/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.process.manager.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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
import org.fusesource.process.manager.InstallOptions;
import org.fusesource.process.manager.InstallTask;
import org.fusesource.process.manager.Installation;
import org.fusesource.process.manager.ProcessController;
import org.fusesource.process.manager.config.JsonHelper;
import org.fusesource.process.manager.config.ProcessConfig;
import org.fusesource.process.manager.support.DefaultProcessController;
import org.fusesource.process.manager.support.FileUtils;
import org.fusesource.process.manager.support.JarInstaller;
import org.fusesource.process.manager.support.ProcessUtils;
import org.fusesource.process.manager.support.command.CommandFailedException;
import org.fusesource.process.manager.support.command.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessManagerService implements ProcessManagerServiceMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessManagerService.class);
    private static final String INSTALLED_BINARY = "install.bin";

    private Executor executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("fuse-process-manager-%s").build());
    private File storageLocation;
    private int lastId = 0;
    private final Duration untarTimeout = Duration.valueOf("1h");
    private SortedMap<Integer, Installation> installations = Maps.newTreeMap();
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
            LOGGER.warn("An error occured during mbean server registration: " + e, e);
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
                LOGGER.warn("An error occured during mbean server registration: " + e, e);
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
                    if (name.matches("\\d+")) {
                        try {
                            Integer value = Integer.parseInt(name);
                            if (value != null) {
                                int id = value.intValue();
                                if (id > lastId) {
                                    lastId = id;
                                }

                                // String url = "TODO";
                                ProcessConfig config = JsonHelper.loadProcessConfig(file);
                                createInstallation(id, ProcessUtils.findInstallDir(file), config);
                            }
                        } catch (NumberFormatException e) {
                            // should never happen :)
                        }
                    }
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
    public ImmutableMap<Integer, Installation> listInstallationMap() {
        return ImmutableMap.copyOf(installations);
    }

    @Override
    public Installation install(final InstallOptions options, final InstallTask postInstall) throws Exception {
        @SuppressWarnings("serial")
		InstallTask installTask = new InstallTask() {
            @Override
            public void install(ProcessConfig config, int id, File installDir) throws Exception {
                config.setName(options.getName());
                downloadContent(options.getUrl(), installDir);
                if (options.getExtractCmd() != null) {
                    File archive = new File(installDir, INSTALLED_BINARY);
                    FileUtils.extractArchive(archive, installDir, options.getExtractCmd(), untarTimeout, executor);
                }
                if (postInstall != null) {
                    postInstall.install(config, id, installDir);
                }
            }
        };
        return installViaScript(options.getControllerUrl(), installTask);
    }

    @Override
    public Installation installJar(final InstallOptions parameters) throws Exception {
        @SuppressWarnings("serial")
        InstallTask installTask = new InstallTask() {
            @Override
            public void install(ProcessConfig config, int id, File installDir) throws Exception {
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
                etc.mkdirs();
                Files.write("", new File(etc, "config.properties"), Charsets.UTF_8);
                Files.write("", new File(etc, "jvm.config"), Charsets.UTF_8);

                JarInstaller installer = new JarInstaller(executor);
                installer.unpackJarProcess(config, id, installDir, parameters);
            }
        };
        return installViaScript(parameters.getControllerUrl(), installTask);
    }

    // Properties
    //-------------------------------------------------------------------------
    public File getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(File storageLocation) {
        this.storageLocation = storageLocation;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    // Implementation
    //-------------------------------------------------------------------------

    protected Installation installViaScript(URL controllerUrl, InstallTask installTask) throws Exception {
        int id = createNextId();
        File installDir = createInstallDir(id);
        installDir.mkdirs();

        ProcessConfig config = loadControllerJson(controllerUrl);
        installTask.install(config, id, installDir);
        JsonHelper.saveProcessConfig(config, installDir);

        Installation installation = createInstallation(id, installDir, config);
        installation.getController().install();
        return installation;
    }

    protected void downloadContent(final URL url, File installDir) throws IOException, CommandFailedException {
        // copy the URL to the install dir
        File archive = new File(installDir, INSTALLED_BINARY);
        Files.copy(new InputSupplier<InputStream>() {
            @Override
            public InputStream getInput() throws IOException {
                return url.openStream();
            }
        }, archive);
    }

    protected ProcessConfig loadControllerJson(URL controllerJson) throws IOException {
        if (controllerJson == null) {
            return new ProcessConfig();
        } else {
            return JsonHelper.loadProcessConfig(controllerJson);
        }
    }


    /**
     * Returns the next process ID
     */
    protected synchronized int createNextId() {
        // lets double check it doesn't exist already
        File dir;
        do {
            lastId++;
            dir = createInstallDir(lastId);
        }
        while (dir.exists());
        return lastId;
    }

    protected File createInstallDir(int id) {
        return new File(storageLocation, "" + id);
    }


    protected Installation createInstallation(int id, File rootDir, ProcessConfig config) {
        // TODO we should support different kinds of controller based on the kind of installation
        // we could maybe discover a descriptor file to describe how to control the process?
        // or generate this file on installation time?

        File installDir = ProcessUtils.findInstallDir(rootDir);
        ProcessController controller = createController(id, config, rootDir, installDir);
        // TODO need to read the URL from somewhere...
        Installation installation = new Installation(id, installDir, controller, config);
        installations.put(id, installation);
        return installation;
    }

    protected ProcessController createController(int id, ProcessConfig config, File rootDir, File installDir) {
        return new DefaultProcessController(id, config, installDir);
    }


}

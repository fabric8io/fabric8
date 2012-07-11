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
package org.fusesource.process.manager.support;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.fusesource.process.manager.Installation;
import org.fusesource.process.manager.ProcessController;
import org.fusesource.process.manager.ProcessManager;
import org.fusesource.process.manager.config.ProcessConfig;
import org.fusesource.process.manager.config.JsonHelper;
import org.fusesource.process.manager.support.command.CommandFailedException;
import org.fusesource.process.manager.support.command.Duration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ProcessManagerImpl implements ProcessManager {
    private Executor executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("fuse-process-manager-%s").build());
    private File storageLocation;
    private int lastId = 0;
    private final Duration untarTimeout = Duration.valueOf("1h");
    private SortedMap<Integer, Installation> installations = Maps.newTreeMap();

    public ProcessManagerImpl() {
    }

    public ProcessManagerImpl(File storageLocation) {
        this.storageLocation = storageLocation;
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

                                String url = "TODO";
                                ProcessConfig config = JsonHelper.loadProcessConfig(file);
                                createInstallation(url, id, findInstallDir(file), config);
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
    public Installation install(final String url, URL controllerJson) throws IOException, CommandFailedException, InterruptedException {
        int id = createNextId();
        File installDir = createInstallDir(id);
        installDir.mkdirs();

        ProcessConfig config = loadControllerJson(controllerJson);
        config.setUrl(url);

        // copy the URL to the install dir
        // TODO should we use a temp file?
        File tarball = new File(installDir, "install.tar.gz");
        Files.copy(new InputSupplier<InputStream>() {
            @Override
            public InputStream getInput() throws IOException {
                return new URL(url).openStream();
            }
        }, tarball);

        FileUtils.extractTar(tarball, installDir, untarTimeout, executor);
        JsonHelper.saveProcessConfig(config, installDir);

        Installation installation = createInstallation(url, id, installDir, config);
        installation.getController().install();
        return installation;
    }

    private ProcessConfig loadControllerJson(URL controllerJson) throws IOException {
        if (controllerJson == null) {
            return new ProcessConfig();
        } else {
            return JsonHelper.loadProcessConfig(controllerJson);
        }
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


    protected Installation createInstallation(String url, int id, File rootDir, ProcessConfig config) {
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

    protected ProcessController createController(int id, ProcessConfig config, File rootDir, File installDir) {
        return new DefaultProcessController(id, config, executor, installDir);
    }

    /**
     * Lets find the install dir, which may be the root dir or could be a child directory (as typically untarring will create a new child directory)
     */
    protected File findInstallDir(File rootDir) {
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

    protected boolean installExists(File file) {
        if (file.isDirectory()) {
            File binDir = new File(file, "bin");
            return binDir.exists() && binDir.isDirectory();
        }
        return false;
    }
}

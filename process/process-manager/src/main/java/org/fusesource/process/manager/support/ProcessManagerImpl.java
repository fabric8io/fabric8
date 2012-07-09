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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.fusesource.process.manager.Installation;
import org.fusesource.process.manager.ProcessController;
import org.fusesource.process.manager.ProcessManager;
import org.fusesource.process.manager.support.command.CommandFailedException;
import org.fusesource.process.manager.support.command.Duration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ProcessManagerImpl implements ProcessManager {
    private Executor executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("fuse-process-manager-%s").build());
    private File storageLocation;
    private int lastId = 0;
    private final Duration untarTimeout = Duration.valueOf("1h");
    private List<Installation> installations = Lists.newArrayList();

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
                                int v = value.intValue();
                                if (v > lastId) {
                                    lastId = v;
                                }

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
    public List<Installation> listInstallations() {
        return ImmutableList.copyOf(installations);
    }

    @Override
    public Installation install(final String url) throws IOException, CommandFailedException {
        int id = createNextId();
        File installDir = createInstallDir(id);
        installDir.mkdirs();

        // copy the URL to the install dir
        // TODO we could use a temp file?
        File tarball = new File(installDir, "install.tar.gz");
        Files.copy(new InputSupplier<InputStream>() {
            @Override
            public InputStream getInput() throws IOException {
                return new URL(url).openStream();
            }
        }, tarball);

        //}, tarball.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

        FileUtils.extractTar(tarball, installDir, untarTimeout, executor);
        return createInstallation(id, installDir);
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


    protected Installation createInstallation(int id, File installDir) {
        // TODO we should support different kinds of controller based on the kind of installation
        // we could maybe discover a descriptor file to describe how to control the process?
        // or generate this file on installation time?
        ProcessController controller = new DefaultProcessController(executor, installDir);
        Installation installation = new Installation(id, installDir, controller);
        installations.add(installation);
        return installation;
    }
}

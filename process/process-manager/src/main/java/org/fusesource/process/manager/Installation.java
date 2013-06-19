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
package org.fusesource.process.manager;

import org.fusesource.process.manager.config.ProcessConfig;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

/**
 * Represents a locally installed managed process.
 */
public class Installation implements Serializable {

    private static final long serialVersionUID = 5127636210465637719L;

    private final int id;
    private final File installDir;
    private final ProcessController controller;
    private final ProcessConfig config;

    public Installation(int id, File installDir, ProcessController controller, ProcessConfig config) {
        this.id = id;
        this.installDir = installDir;
        this.controller = controller;
        this.config = config;
    }

    @Override
    public String toString() {
        return "Installation[" + getName() + " at " + installDir + "]";
    }

    public ProcessController getController() {
        return controller;
    }

    public int getId() {
        return id;
    }

    public File getInstallDir() {
        return installDir;
    }

    public String getName() {
        return config.getName();
    }

    public Map<String, String> getEnvironment() {
        return config.getEnvironment();
    }
}

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
package io.fabric8.process.manager;

import io.fabric8.process.manager.config.ProcessConfig;
import io.fabric8.process.manager.support.ProcessUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

/**
 * Represents a locally installed managed process.
 */
public class Installation implements Serializable {

    private static final long serialVersionUID = 5127636210465637719L;

    private final String id;
    private final File installDir;
    private final ProcessController controller;
    private final ProcessConfig config;

    public Installation(String id, File installDir, ProcessController controller, ProcessConfig config) {
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

    public String getId() {
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

    /**
     * Returns the PID for the process; checking first if its still active.
     *
     * @return null if the process is no longer active
     */
    public Long getActivePid() throws IOException {
        ProcessController aController = getController();
        Long answer = null;
        if (aController != null) {
            answer = aController.getPid();
        }
        if (answer != null) {
            if (!ProcessUtils.isProcessAlive(answer)) {
                answer = null;
            }
        }
        return answer;
    }
}

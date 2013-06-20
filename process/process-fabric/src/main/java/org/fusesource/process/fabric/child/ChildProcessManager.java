/**
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
package org.fusesource.process.fabric.child;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.google.common.base.Strings;

import org.fusesource.common.util.Objects;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.process.manager.InstallOptions;
import org.fusesource.process.manager.InstallTask;
import org.fusesource.process.manager.Installation;
import org.fusesource.process.manager.ProcessController;
import org.fusesource.process.manager.ProcessManager;
import org.fusesource.process.manager.config.ProcessConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ChildProcessManager {
    private static final transient Logger LOG = LoggerFactory.getLogger(ChildProcessManager.class);

    private FabricService fabricService;
    private ProcessManager processManager;

    private Runnable checkConfigurations = new Runnable() {
        public void run() {
            checkChildProcessConfigurationsChanged();
        }
    };

    public void init() throws Exception {
        Objects.notNull(processManager, "processManager");
        Objects.notNull(fabricService, "fabricService");
        fabricService.trackConfiguration(checkConfigurations);
        checkConfigurations.run();
    }

    public void destroy() throws Exception {
        Objects.notNull(processManager, "processManager");
        Objects.notNull(fabricService, "fabricService");
        fabricService.unTrackConfiguration(checkConfigurations);
    }

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public ProcessManager getProcessManager() {
        return processManager;
    }

    public void setProcessManager(ProcessManager processManager) {
        this.processManager = processManager;
    }

    protected void checkChildProcessConfigurationsChanged() {
        Container container = fabricService.getCurrentContainer();
        Profile profile = container.getOverlayProfile();
        Map<String,Map<String,String>> configurations = profile.getConfigurations();

        Map<String, String> map = configurations
                .get("org.fusesource.process.fabric.child");

        if (map != null) {
            // lets lets build a model for all the containers we think we should have
            Map<String, ProcessRequirements> requirementsMap = loadProcessRequirements(map);

            System.out.println("Require containers: " + requirementsMap + " for processManager " + processManager);

            // now for each container, lets either create it if its not already created,
            // or modify its configuration if its created (stopping it first for any removals
            // or changes ot the shared libraries

            // TODO

            if (processManager == null) {
                LOG.warn("No ProcessManager so cannot provision the child processes");
                return;
            }
            for (ProcessRequirements requirements : requirementsMap.values()) {
                try {
                    provisionProcess(requirements);
                } catch (Exception e) {
                    LOG.error("Failed to provision process " + requirements + ". " + e, e);
                }
            }
        }
    }

    protected Installation provisionProcess(ProcessRequirements requirements) throws Exception {
        String id = requirements.getId();

        Installation installation = findProcessInstallation(id);

        // TODO check that the installation is the same
        // for now lets remove it just in case! :)
        if (installation != null) {
            ProcessController controller = installation.getController();
            try {
                controller.stop();
            } catch (Exception e) {
                LOG.warn("Ignored exception while trying to stop process " + installation + " " + e);
            }
            controller.uninstall();
            controller = null;
        }

        // TODO now build up a list of all the files.....


        InstallOptions installOptions = requirements.createInstallOptions();
        InstallTask copyFiles = new InstallTask() {
            public void install(ProcessConfig config, int id, File installDir) throws Exception {
                // install the deploy or shared library files...
            }
        };
        installation = processManager.install(installOptions, copyFiles);
        if (installation != null) {
            installation.getController().start();
        }
        return installation;
    }

    protected Installation findProcessInstallation(String id) {
        List<Installation> installations = processManager.listInstallations();
        for (Installation installation : installations) {
            String name = installation.getName();
            if (Objects.equal(id, name)) {
                return installation;
            }
        }
        return null;
    }

    private Map<String, ProcessRequirements> loadProcessRequirements(Map<String, String> properties) {
        Map<String,ProcessRequirements> answer = new HashMap<String, ProcessRequirements>();

        Set<Map.Entry<String,String>> entries = properties.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();

            // lets build up the model of the containers
            String[] split = key.split("\\.");
            if (split != null && split.length > 1) {
                String containerId = split[0];
                String propertyName = split[1];

                ProcessRequirements container = answer.get(containerId);
                if (container == null){
                    container = new ProcessRequirements(containerId);
                    answer.put(containerId, container);
                }

                if (split.length == 2) {
                    if ("kind".equals(propertyName)) {
                        container.setKind(value);
                    } else if ("url".equals(propertyName)) {
                        container.setUrl(value);
                    } else {
                        LOG.warn("Unknown property " + propertyName + " for container process " + containerId);
                    }
                } else if (split.length == 3 && "profile".equals(propertyName)) {
                    StringTokenizer iter = new StringTokenizer(value);
                    while (iter.hasMoreElements()) {
                        String token = iter.nextToken();
                        if (!Strings.isNullOrEmpty(token)) {
                            container.addProfile(value);
                        }
                    }
                } else {
                    LOG.warn("Ignored invalid entry " + key + " = " + value);
                }
            } else {
                LOG.warn("Ignored invalid entry " + key + " = " + value);
            }
            // TODO make the containers...
        }

        return answer;
    }


}

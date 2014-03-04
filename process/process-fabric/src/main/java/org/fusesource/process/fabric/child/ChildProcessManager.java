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

import com.google.common.collect.Maps;
import org.fusesource.common.util.Objects;
import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.download.DownloadManagers;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.internal.ProfileOverlayImpl;
import org.fusesource.process.fabric.child.support.ByteToStringValues;
import org.fusesource.process.fabric.child.support.LayOutPredicate;
import org.fusesource.process.fabric.child.tasks.ApplyConfigurationTask;
import org.fusesource.process.fabric.child.tasks.CompositeTask;
import org.fusesource.process.fabric.child.tasks.DeploymentTask;
import org.fusesource.process.manager.InstallOptions;
import org.fusesource.process.manager.InstallTask;
import org.fusesource.process.manager.Installation;
import org.fusesource.process.manager.ProcessController;
import org.fusesource.process.manager.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class ChildProcessManager {
    private static final transient Logger LOG = LoggerFactory.getLogger(ChildProcessManager.class);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private FabricService fabricService;
    private ProcessManager processManager;

    public void destroy() {
        executorService.shutdown();
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

    Installation provisionProcess(ProcessRequirements requirements) throws Exception {
        // TODO check that the installation is the same
        uninstallProcess(requirements);

        String id = requirements.getId();
        InstallOptions installOptions = requirements.createInstallOptions();
        Profile processProfile = getProcessProfile(requirements, true);
        Profile deployProcessProfile = getProcessProfile(requirements, false);
        Map<String, String> configuration = getProcessLayout(processProfile, requirements.getLayout());

        DownloadManager downloadManager = DownloadManagers
                .createDownloadManager(fabricService, processProfile, executorService);
        InstallTask applyConfiguration = new ApplyConfigurationTask(configuration, installOptions.getProperties());
        InstallTask applyProfile = new DeploymentTask(downloadManager, deployProcessProfile);
        InstallTask compositeTask = new CompositeTask(applyConfiguration, applyProfile);
        Installation installation = processManager.install(installOptions, compositeTask);
        if (installation != null) {
            installation.getController().start();
        }
        return installation;
    }


    void uninstallProcess(ProcessRequirements requirements) throws Exception {
        String id = requirements.getId();

        Installation installation = findProcessInstallation(id);
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
    }

    protected Profile getProcessProfile(ProcessRequirements requirements, boolean includeController) {
        Container container = fabricService.getCurrentContainer();
        ProcessProfile profile = new ProcessProfile(container, requirements, fabricService, includeController);
        return new ProfileOverlayImpl(profile, fabricService.getEnvironment(), true, fabricService);
    }

    protected Map<String, String> getProcessLayout(Profile profile, String layoutPath) {
        return ByteToStringValues.INSTANCE.apply(Maps.filterKeys(profile.getFileConfigurations(), new LayOutPredicate(layoutPath)));
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


}

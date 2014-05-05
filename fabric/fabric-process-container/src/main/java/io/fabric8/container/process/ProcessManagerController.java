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
package io.fabric8.container.process;

import io.fabric8.api.Container;
import io.fabric8.api.CreateChildContainerMetadata;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.support.Strings;
import io.fabric8.common.util.Objects;
import io.fabric8.deployer.JavaContainers;
import io.fabric8.process.manager.InstallOptions;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessManager;
import io.fabric8.service.child.ChildConstants;
import io.fabric8.service.child.ChildContainerController;
import io.fabric8.service.child.ChildContainers;
import io.fabric8.service.child.JavaContainerEnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An implementation of {@link io.fabric8.service.child.ChildContainerController} which uses the {@link ProcessManager}
 */
public class ProcessManagerController implements ChildContainerController {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProcessManagerController.class);

    private final ProcessControllerFactoryService owner;
    private final Configurer configurer;
    private final ProcessManager processManager;
    private final FabricService fabricService;
    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();

    public ProcessManagerController(ProcessControllerFactoryService owner, Configurer configurer, ProcessManager processManager, FabricService fabricService) {
        this.owner = owner;
        this.configurer = configurer;
        this.processManager = processManager;
        this.fabricService = fabricService;
    }

    @Override
    public CreateChildContainerMetadata create(CreateChildContainerOptions options, CreationStateListener listener) throws Exception {
        CreateChildContainerMetadata metadata = new CreateChildContainerMetadata();

        String containerId = options.getName();
        metadata.setCreateOptions(options);
        metadata.setContainerName(containerId);

        Container container = null;
        try {
            container = fabricService.getContainer(containerId);
        } catch (Exception e) {
            LOG.debug("Could nto find container: " + containerId);
        }

        Map<String, String> environmentVariables = ChildContainers.getEnvironmentVariables(fabricService, options);
        Installation installation = null;
        InstallOptions parameters = null;
        try {
            if (ChildContainers.isJavaContainer(fabricService, options)) {
                parameters = createJavaInstallOptions(container, metadata, options, environmentVariables);
                Objects.notNull(parameters, "JavaInstall parameters");
                installation = processManager.installJar(parameters);
            } else {
                parameters = createProcessInstallOptions(container, metadata, options, environmentVariables);
                InstallTask postInstall = createProcessPostInstall(options);
                Objects.notNull(parameters, "process parameters");
                installation = processManager.install(parameters, postInstall);
            }
        } catch (Exception e) {
            handleException("Creating container " + containerId, e);
        }
        LOG.info("Creating process container with environment vars: " + environmentVariables);

        String defaultHost = fabricService.getCurrentContainer().getLocalHostname();
        if (Strings.isNullOrBlank(defaultHost)) {
            defaultHost = "localhost";
        }
        String jolokiaUrl = JolokiaAgentHelper.findJolokiaUrlFromEnvironmentVariables(environmentVariables, defaultHost);
        if (!Strings.isNullOrBlank(jolokiaUrl)) {
            JavaContainers.registerJolokiaUrl(container, jolokiaUrl);
        }
        if (installation != null) {
            installation.getController().start();
        }
        return metadata;
    }

    @Override
    public void start(Container container) {
        Installation installation = getInstallation(container);
        if (installation != null) {
            try {
                installation.getController().start();
            } catch (Exception e) {
                handleException("Starting container " + container.getId(), e);
            }
        }
    }

    @Override
    public void stop(Container container) {
        Installation installation = getInstallation(container);
        if (installation != null) {
            try {
                installation.getController().stop();
            } catch (Exception e) {
                handleException("Stopping container " + container.getId(), e);
            }
        }
    }

    @Override
    public void destroy(Container container) {
        Installation installation = getInstallation(container);
        if (installation != null) {
            try {
                installation.getController().stop();
            } catch (Exception e) {
                LOG.info("Failed to stop process for container " + container.getId() + ". " + e, e);
            }
            installation.getController().uninstall();
        }
    }

    protected InstallOptions createJavaInstallOptions(Container container, CreateChildContainerMetadata metadata, CreateChildContainerOptions options, Map<String, String> environmentVariables) throws Exception {
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();

        Map<String, ?> javaContainerConfig = Profiles.getOverlayConfiguration(fabricService, options.getProfiles(), options.getVersion(), ChildConstants.JAVA_CONTAINER_PID);
        JavaContainerConfig javaConfig = new JavaContainerConfig();
        configurer.configure(javaContainerConfig, javaConfig);
        javaConfig.updateEnvironmentVariables(environmentVariables);

        if (JolokiaAgentHelper.hasJolokiaAgent(environmentVariables)) {
            int jolokiaPort = owner.createJolokiaPort(container.getId());
            JolokiaAgentHelper.substituteEnvironmentVariables(javaConfig, environmentVariables, JolokiaAgentHelper.getJolokiaPortOverride(jolokiaPort),  JolokiaAgentHelper.getJolokiaAgentIdOverride(fabricService.getEnvironment()));
        } else {
            JolokiaAgentHelper.substituteEnvironmentVariables(javaConfig, environmentVariables, JolokiaAgentHelper.getJolokiaAgentIdOverride(fabricService.getEnvironment()));
        }

        List<Profile> profiles = Profiles.getProfiles(fabricService, profileIds, versionId);
        Map<String, File> javaArtifacts = JavaContainers.getJavaContainerArtifactsFiles(fabricService, profiles, downloadExecutor);

        if (container != null) {
            List<String> provisionList = new ArrayList<String>();
            for (String name : javaArtifacts.keySet()) {
                int idx = name.indexOf(":mvn:");
                if (idx > 0) {
                    name = name.substring(idx + 1);
                }
                provisionList.add(name);
            }
            Collections.sort(provisionList);
            container.setProvisionList(provisionList);
        }

        InstallOptions.InstallOptionsBuilder builder = InstallOptions.builder();
        builder.jarFiles(javaArtifacts.values());
        builder.id(options.getName());
        builder.environment(environmentVariables);
        String mainClass = environmentVariables.get(JavaContainerEnvironmentVariables.FABRIC8_JAVA_MAIN);
        String name = "java";
        if (!Strings.isNullOrBlank(mainClass)) {
            name += " " + mainClass;
        }
        builder.name(name);
        metadata.setContainerType(name);
        builder.mainClass(mainClass);
        return builder.build();
    }

    protected InstallOptions createProcessInstallOptions(Container container, CreateChildContainerMetadata metadata, CreateChildContainerOptions options, Map<String, String> environmentVariables) throws Exception {
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        Map<String, ?> configuration = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, ChildConstants.PROCESS_CONTAINER_PID);
        ProcessContainerConfig configObject = new ProcessContainerConfig();
        configurer.configure(configuration, configObject);
        return configObject.createProcessInstallOptions(fabricService, metadata, options, environmentVariables);
    }

    protected InstallTask createProcessPostInstall(CreateChildContainerOptions options) {
        return null;
    }


    protected Installation getInstallation(Container container) {
        return processManager.getInstallation(container.getId());
    }

    protected void handleException(String message, Exception cause) {
        throw new RuntimeException(message + ". " + cause, cause);
    }
}

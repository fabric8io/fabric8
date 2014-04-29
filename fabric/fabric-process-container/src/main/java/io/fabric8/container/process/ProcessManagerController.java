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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An implementation of {@link io.fabric8.service.child.ChildContainerController} which uses the {@link ProcessManager}
 */
public class ProcessManagerController implements ChildContainerController {
    private static final transient Logger LOG = LoggerFactory.getLogger(ProcessManagerController.class);

    private final Configurer configurer;
    private final ProcessManager processManager;
    private final FabricService fabricService;
    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();

    public ProcessManagerController(Configurer configurer, ProcessManager processManager, FabricService fabricService) {
        this.configurer = configurer;
        this.processManager = processManager;
        this.fabricService = fabricService;
    }

    @Override
    public CreateChildContainerMetadata create(CreateChildContainerOptions options, CreationStateListener listener) throws Exception {
        String containerName = options.getName();

        CreateChildContainerMetadata metadata = new CreateChildContainerMetadata();

        metadata.setCreateOptions(options);
        metadata.setContainerName(containerName);

        Map<String, String> environmentVariables = ChildContainers.getEnvironmentVariables(fabricService, options);
        LOG.info("Creating process container with environment vars: " + environmentVariables);
        Installation installation = null;
        try {
            if (ChildContainers.isJavaContainer(fabricService, options)) {
                InstallOptions parameters = createJavaInstallOptions(metadata, options, environmentVariables);
                Objects.notNull(parameters, "JavaInstall parameters");
                installation = processManager.installJar(parameters);
            } else {
                InstallOptions parameters = createProcessInstallOptions(metadata, options, environmentVariables);
                InstallTask postInstall = createProcessPostInstall(options);
                Objects.notNull(parameters, "process parameters");
                installation = processManager.install(parameters, postInstall);
            }
        } catch (Exception e) {
            handleException("Creating container " + containerName, e);
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

    protected InstallOptions createJavaInstallOptions(CreateChildContainerMetadata metadata, CreateChildContainerOptions options, Map<String, String> environmentVariables) throws Exception {
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();

        Map<String, ?> javaContainerConfig = Profiles.getOverlayConfiguration(fabricService, options.getProfiles(), options.getVersion(), ChildConstants.JAVA_CONTAINER_PID);
        JavaContainerConfig javaConfig = new JavaContainerConfig();
        configurer.configure(javaContainerConfig, javaConfig);
        javaConfig.updateEnvironmentVariables(environmentVariables);

        List<Profile> profiles = Profiles.getProfiles(fabricService, profileIds, versionId);
        Map<String, File> javaArtifacts = JavaContainers.getJavaContainerArtifactsFiles(fabricService, profiles, downloadExecutor);

        InstallOptions.InstallOptionsBuilder builder = InstallOptions.builder();
        builder.jarFiles(javaArtifacts.values());
        builder.id(options.getName());
        builder.environment(environmentVariables);
        String mainClass = environmentVariables.get(ChildConstants.JAVA_CONTAINER_ENV_VARS.FABRIC8_JAVA_MAIN);
        String name = "java";
        if (!Strings.isNullOrBlank(mainClass)) {
            name += " " + mainClass;
        }
        builder.name(name);
        metadata.setContainerType(name);
        builder.mainClass(mainClass);
        return builder.build();
    }

    protected InstallOptions createProcessInstallOptions(CreateChildContainerMetadata metadata, CreateChildContainerOptions options, Map<String, String> environmentVariables) throws Exception {
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

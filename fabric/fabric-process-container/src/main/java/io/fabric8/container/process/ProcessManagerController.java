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

import io.fabric8.agent.mvn.Parser;
import io.fabric8.api.Container;
import io.fabric8.api.CreateChildContainerMetadata;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.api.scr.Configurer;
import io.fabric8.common.util.Objects;
import io.fabric8.process.manager.InstallOptions;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessManager;
import io.fabric8.service.child.ChildConstants;
import io.fabric8.service.child.ChildContainerController;
import io.fabric8.service.child.ChildContainers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final ContainerInstallations installations;
    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();

    public ProcessManagerController(Configurer configurer, ProcessManager processManager, FabricService fabricService, ContainerInstallations installations) {
        this.configurer = configurer;
        this.processManager = processManager;
        this.fabricService = fabricService;
        this.installations = installations;
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
                InstallOptions parameters = createJavaInstallOptions(options, environmentVariables);
                Objects.notNull(parameters, "JavaInstall parameters");
                installation = processManager.installJar(parameters);
            } else {
                InstallOptions parameters = createProcessInstallOptions(options, environmentVariables);
                InstallTask postInstall = createProcessPostInstall(options);
                Objects.notNull(parameters, "process parameters");
                installation = processManager.install(parameters, postInstall);
            }
        } catch (Exception e) {
            handleException("Creating container " + containerName, e);
        }
        if (installation != null) {
            installations.add(containerName, installation);
            installation.getController().start();
        }
        return metadata;
    }

    @Override
    public void start(Container container) {
        Installation installation = installations.getInstallation(container);
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
        Installation installation = installations.getInstallation(container);
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
        Installation installation = installations.remove(container);
        if (installation != null) {
            try {
                installation.getController().stop();
            } catch (Exception e) {
                LOG.info("Failed to stop process for container " + container.getId() + ". " + e, e);
            }
            installation.getController().uninstall();
        }
    }

    protected InstallOptions createJavaInstallOptions(CreateChildContainerOptions options, Map<String, String> environmentVariables) throws Exception {
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();

/*
        Map<String, ?> javaContainerConfig = Profiles.getOverlayConfiguration(fabricService, options.getProfiles(), options.getVersion(), ChildConstants.JAVA_CONTAINER_PID);

        JavaContainerConfig javaConfig = new JavaContainerConfig();
        configurer.configure(javaContainerConfig, javaConfig);
*/


        List<Profile> profiles = Profiles.getProfiles(fabricService, profileIds, versionId);
        Map<String, Parser> javaArtifacts = JavaContainers.getJavaContainerArtifacts(fabricService, profiles, downloadExecutor);

        // TODO lets add all the java artifacts into the install options...

        InstallOptions.InstallOptionsBuilder builder = InstallOptions.builder();
        builder.environment(environmentVariables);
        builder.mainClass(environmentVariables.get(ChildConstants.JAVA_CONTAINER_ENV_VARS.FABRIC8_JAVA_MAIN));
        return builder.build();
    }


    protected InstallOptions createProcessInstallOptions(CreateChildContainerOptions options, Map<String, String> environmentVariables) throws Exception {
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        Map<String, ?> configuration = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, ChildConstants.PROCESS_CONTAINER_PID);
        ProcessContainerConfig configObject = new ProcessContainerConfig();
        configurer.configure(configuration, configObject);
        return configObject.createProcessInstallOptions(fabricService, options, environmentVariables);
    }

    protected InstallTask createProcessPostInstall(CreateChildContainerOptions options) {
        return null;
    }

    protected void handleException(String message, Exception cause) {
        throw new RuntimeException(message + ". " + cause, cause);
    }
}

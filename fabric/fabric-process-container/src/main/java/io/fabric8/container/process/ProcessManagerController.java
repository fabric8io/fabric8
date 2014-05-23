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

import com.google.common.collect.ImmutableMap;
import io.fabric8.api.Container;
import io.fabric8.api.CreateChildContainerMetadata;
import io.fabric8.api.CreateChildContainerOptions;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.EnvironmentVariables;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Profiles;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.support.Strings;
import io.fabric8.common.util.Objects;
import io.fabric8.deployer.JavaContainers;
import io.fabric8.process.manager.InstallContext;
import io.fabric8.process.manager.InstallOptions;
import io.fabric8.process.manager.InstallTask;
import io.fabric8.process.manager.Installation;
import io.fabric8.process.manager.ProcessController;
import io.fabric8.process.manager.ProcessManager;
import io.fabric8.process.manager.config.JsonHelper;
import io.fabric8.process.manager.config.ProcessConfig;
import io.fabric8.process.manager.support.ApplyConfigurationTask;
import io.fabric8.process.manager.support.CompositeTask;
import io.fabric8.process.manager.support.DownloadResourcesTask;
import io.fabric8.process.manager.support.InstallDeploymentsTask;
import io.fabric8.process.manager.support.JarInstaller;
import io.fabric8.process.manager.support.ProcessUtils;
import io.fabric8.service.child.ChildConstants;
import io.fabric8.service.child.ChildContainerController;
import io.fabric8.service.child.ChildContainers;
import io.fabric8.service.child.JavaContainerEnvironmentVariables;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.fabric8.deployer.JavaContainers.registerJolokiaUrl;

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
    private final CuratorFramework curator;

    public ProcessManagerController(ProcessControllerFactoryService owner, Configurer configurer, ProcessManager processManager, FabricService fabricService, CuratorFramework curator) {
        this.owner = owner;
        this.configurer = configurer;
        this.processManager = processManager;
        this.fabricService = fabricService;
        this.curator = curator;
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
        ProcessManager procManager = processManager;
        Map<String,String> initialEnvironmentVariables = new HashMap<String, String>();
        Installation installation = createInstallation(procManager, container, options, metadata, initialEnvironmentVariables);
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

    /**
     * A profile may have changed so lets double check that there have been no changes to the installation
     */
    public void updateInstallation(final Container container, final Installation installation) throws Exception {
        Map<String,String> initialEnvironmentVariables = new HashMap<String, String>();
        ProcessConfig currentConfig = getProcessConfig(installation);
        if (currentConfig != null) {
            // lets preserve the ports allocated
            Map<String, String> environment = currentConfig.getEnvironment();
            for (Map.Entry<String, String> entry : environment.entrySet()) {
                String key = entry.getKey();
                if (key.endsWith("_PROXY_PORT")) {
                    String value = entry.getValue();
                    initialEnvironmentVariables.put(key, value);
                }
            }
        }

        CreateContainerMetadata<?> containerMetadata = container.getMetadata();
        if (containerMetadata instanceof CreateChildContainerMetadata) {
            CreateChildContainerMetadata metadata = (CreateChildContainerMetadata) containerMetadata;
            CreateContainerOptions createOptions = metadata.getCreateOptions();
            if (createOptions instanceof CreateChildContainerOptions) {
                CreateChildContainerOptions options = (CreateChildContainerOptions) createOptions;
                ProcessManager procManager = new ProcessManager() {
                    @Override
                    public Installation install(InstallOptions parameters, InstallTask postInstall) throws Exception {
                        updateInstallation(container, installation, parameters, postInstall);
                        return null;
                    }

                    @Override
                    public Installation installJar(InstallOptions parameters) throws Exception {
                        updateInstallation(container, installation, parameters, null);
                        return null;
                    }

                    @Override
                    public Executor getExecutor() {
                        return processManager.getExecutor();
                    }

                    @Override
                    public List<Installation> listInstallations() {
                        return processManager.listInstallations();
                    }

                    @Override
                    public ImmutableMap<String, Installation> listInstallationMap() {
                        return processManager.listInstallationMap();
                    }

                    @Override
                    public Installation getInstallation(String id) {
                        return processManager.getInstallation(id);
                    }

                    @Override
                    public ProcessConfig loadProcessConfig(InstallOptions options) throws IOException {
                        return processManager.loadProcessConfig(options);
                    }
                };
                Installation newInstallation = createInstallation(procManager, container, options, metadata, initialEnvironmentVariables);
                if (newInstallation != null) {
                    // lets see if anything significant changed
                    // meaning we need to restart - e.g. env vars
                }
            }
        }
    }

    protected void updateInstallation(Container container, final Installation installation, InstallOptions parameters, InstallTask postInstall) throws Exception {
        boolean requiresRestart = false;
        ProcessConfig processConfig = processManager.loadProcessConfig(parameters);
        processConfig.setName(parameters.getName());
        ProcessConfig oldConfig = getProcessConfig(installation);
        String id = installation.getId();
        File installDir = installation.getInstallDir();
        InstallContext installContext = new InstallContext(parameters.getContainer(), installDir, true);
        if (processConfig != null && !oldConfig.equals(processConfig)) {
            installContext.addRestartReason("Environment Variables");
            if (LOG.isDebugEnabled()) {
                LOG.debug("Requires restart as config has changed: OLD: " + JsonHelper.toJson(oldConfig) + " and NEW: " + JsonHelper.toJson(processConfig));
            }
        }
        if (postInstall != null) {
            postInstall.install(installContext, processConfig, id, installDir);
            JsonHelper.saveProcessConfig(processConfig, installDir);
        } else {
            // lets do the Jar thing...
            JarInstaller installer = new JarInstaller(parameters, processManager.getExecutor());
            installer.install(installContext, processConfig, id, installDir);
        }
        installContext.updateContainerChecksums();
        if (installContext.isRestartRequired()) {
            LOG.info("Restarting " + container.getId() + " due to profile changes: " + installContext.getRestartReasons());
            ProcessController controller = installation.getController();
            if (controller != null && container != null && container.isAlive()) {
                controller.restart();
            }
        }
    }

    protected ProcessConfig getProcessConfig(Installation installation) {
        ProcessController controller = installation.getController();
        return controller.getConfig();
    }

    protected Installation createInstallation(ProcessManager procManager, Container container, CreateChildContainerOptions options, CreateChildContainerMetadata metadata, Map<String, String> initialEnvironmentVariables) throws Exception {
        String containerId = options.getName();
        Map<String, String> environmentVariables = ChildContainers.getEnvironmentVariables(fabricService, options);
        Set<Map.Entry<String, String>> initialEntries = initialEnvironmentVariables.entrySet();
        for (Map.Entry<String, String> entry : initialEntries) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!environmentVariables.containsKey(key)) {
                environmentVariables.put(key, value);
            }
        }
        ProcessContainerConfig processConfig = createProcessContainerConfig(options);
        if (container != null) {
            registerPorts(options, processConfig, container, environmentVariables);
        }
        JolokiaAgentHelper.substituteEnvironmentVariableExpressions(environmentVariables, environmentVariables);
        publishZooKeeperValues(options, processConfig, container, environmentVariables);

        Installation installation = null;
        InstallOptions parameters = null;
        try {
            if (ChildContainers.isJavaContainer(fabricService, options)) {
                parameters = createJavaInstallOptions(container, metadata, options, environmentVariables);
                Objects.notNull(parameters, "JavaInstall parameters");
                installation = procManager.installJar(parameters);
            } else {
                parameters = createProcessInstallOptions(container, metadata, options, processConfig, environmentVariables);
                InstallTask postInstall = createProcessPostInstall(container, options, processConfig, environmentVariables);
                Objects.notNull(parameters, "process parameters");
                installation = procManager.install(parameters, postInstall);
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
            registerJolokiaUrl(container, jolokiaUrl);
        }
        return installation;
    }

    protected InstallOptions createJavaInstallOptions(Container container, CreateChildContainerMetadata metadata, CreateChildContainerOptions options, Map<String, String> environmentVariables) throws Exception {
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();

        Map<String, ?> javaContainerConfig = Profiles.getOverlayConfiguration(fabricService, options.getProfiles(), options.getVersion(), ChildConstants.JAVA_CONTAINER_PID);
        JavaContainerConfig javaConfig = new JavaContainerConfig();
        configurer.configure(javaContainerConfig, javaConfig);
        boolean isJavaContainer = true;
        javaConfig.updateEnvironmentVariables(environmentVariables, isJavaContainer);

        if (JolokiaAgentHelper.hasJolokiaAgent(environmentVariables)) {
            String JOLOKIA_PROXY_PORT_ENV = "FABRIC8_JOLOKIA_PROXY_PORT";
            String portText = environmentVariables.get(JOLOKIA_PROXY_PORT_ENV);
            Integer portObject = null;
            if (portText != null) {
                try {
                    portObject = Integer.parseInt(portText);
                } catch (NumberFormatException e) {
                    LOG.warn("Ignoring bad port number for " + JOLOKIA_PROXY_PORT_ENV + " value '" + portText + ". " + e, e);
                }
            }
            int jolokiaPort = (portObject != null) ? portObject : owner.createJolokiaPort(container.getId());
            environmentVariables.put(JOLOKIA_PROXY_PORT_ENV, "" + jolokiaPort);
            JolokiaAgentHelper.substituteEnvironmentVariables(javaConfig, environmentVariables, isJavaContainer,
                    JolokiaAgentHelper.getJolokiaPortOverride(jolokiaPort), JolokiaAgentHelper.getJolokiaAgentIdOverride(fabricService.getEnvironment()));
        } else {
            JolokiaAgentHelper.substituteEnvironmentVariables(javaConfig, environmentVariables, isJavaContainer,
                    JolokiaAgentHelper.getJolokiaAgentIdOverride(fabricService.getEnvironment()));
        }

        List<Profile> profiles = Profiles.getProfiles(fabricService, profileIds, versionId);
        Map<String, File> javaArtifacts = JavaContainers.getJavaContainerArtifactsFiles(fabricService, profiles, downloadExecutor);
        setProvisionList(container, javaArtifacts);

        InstallOptions.InstallOptionsBuilder builder = InstallOptions.builder();
        builder.container(container);
        builder.jarFiles(javaArtifacts);
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

    protected InstallOptions createProcessInstallOptions(Container container, CreateChildContainerMetadata metadata, CreateChildContainerOptions options, ProcessContainerConfig configObject, Map<String, String> environmentVariables) throws Exception {
        return configObject.createProcessInstallOptions(fabricService, container, metadata, options, environmentVariables);
    }

    private ProcessContainerConfig createProcessContainerConfig(CreateChildContainerOptions options) throws Exception {
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        Map<String, ?> configuration = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, ChildConstants.PROCESS_CONTAINER_PID);
        ProcessContainerConfig configObject = new ProcessContainerConfig();
        configurer.configure(configuration, configObject);
        return configObject;
    }

    protected InstallTask createProcessPostInstall(Container container, CreateChildContainerOptions options, ProcessContainerConfig configObject, Map<String, String> environmentVariables) throws Exception {
        // lets see if there's a template configuration
        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        List<Profile> profiles = Profiles.getProfiles(fabricService, profileIds, versionId);
        String layout = configObject.getOverlayFolder();
        InstallTask answer = null;
        if (layout != null) {
            Map<String, String> configuration = ProcessUtils.getProcessLayout(profiles, layout);
            if (configuration != null && !configuration.isEmpty()) {
                Map variables = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, ChildConstants.TEMPLATE_VARIABLES_PID);
                if (variables == null) {
                    variables = new HashMap();
                } else {
                    JolokiaAgentHelper.substituteEnvironmentVariableExpressions(variables, environmentVariables);
                }
                variables.putAll(environmentVariables);
                LOG.info("Using template variables for MVEL: " + variables);
                answer = new ApplyConfigurationTask(configuration, variables);
            }
        }
        Map<String, String> overlayResources = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, ChildConstants.PROCESS_CONTAINER_OVERLAY_RESOURCES_PID);
        if (overlayResources != null && !overlayResources.isEmpty()) {
            answer = CompositeTask.combine(answer, new DownloadResourcesTask(overlayResources));
        }

        if (!configObject.isInternalAgent()) {
            Map<String, File> javaArtifacts = JavaContainers.getJavaContainerArtifactsFiles(fabricService, profiles, downloadExecutor);
            if (!javaArtifacts.isEmpty()) {
                answer = CompositeTask.combine(answer, new InstallDeploymentsTask(javaArtifacts));
                setProvisionList(container, javaArtifacts);
            }
        }
        return answer;
    }

    protected void setProvisionList(Container container, Map<String, File> javaArtifacts) {
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
    }

    /**
     * Generates mappings from logical ports to physically allocated dynamic ports and exposes them as environment variables
     */
    protected void registerPorts(CreateChildContainerOptions options, ProcessContainerConfig processConfig, Container container, Map<String, String> environmentVariables) {
        String containerId = options.getName();
        Map<String, Object> exposedPorts = new HashMap<String, Object>();
        Map<String, Integer> internalPorts = new HashMap<String, Integer>();
        Map<String, Integer> externalPorts = new HashMap<String, Integer>();

        // lets use the root container to find which ports are allocated as the contianer isn't created yet
        Set<Integer> usedPortByHost = fabricService.getPortService().findUsedPortByHost(fabricService.getCurrentContainer());
        Map<String, String> emptyMap = new HashMap<String, String>();

        Set<String> profileIds = options.getProfiles();
        String versionId = options.getVersion();
        Map<String, String> ports = Profiles.getOverlayConfiguration(fabricService, profileIds, versionId, ChildConstants.PORTS_PID);
        SortedMap<Integer, String> sortedInternalPorts = new TreeMap<Integer, String>();

        for (Map.Entry<String, String> portEntry : ports.entrySet()) {
            String portName = portEntry.getKey();
            String portText = portEntry.getValue();
            if (portText != null && !io.fabric8.common.util.Strings.isNullOrBlank(portText)) {
                Integer port = null;
                try {
                    port = Integer.parseInt(portText);
                } catch (NumberFormatException e) {
                    LOG.warn("Ignoring bad port number for " + portName + " value '" + portText + "' in PID: " + ChildConstants.PORTS_PID);
                }
                if (port != null) {
                    sortedInternalPorts.put(port, portName);
                    internalPorts.put(portName, port);
                    exposedPorts.put(portText + "/tcp", emptyMap);
                } else {
                    LOG.info("No port for " + portName);
                }
            }
        }

        String jolokiaUrl = null;
        Container currentContainer = fabricService.getCurrentContainer();
        String listenHost = currentContainer.getLocalIp();
        if (Strings.isNullOrBlank(listenHost)) {
            listenHost = currentContainer.getLocalHostname();
        }
        if (Strings.isNullOrBlank(listenHost)) {
            listenHost = "localhost";
        }
        if (!environmentVariables.containsKey(EnvironmentVariables.FABRIC8_LISTEN_ADDRESS)) {
            environmentVariables.put(EnvironmentVariables.FABRIC8_LISTEN_ADDRESS, listenHost);
        }

        Set<String> disableDynamicPorts = new HashSet<String>();
        String[] dynamicPortArray = processConfig.getDisableDynamicPorts();
        if (dynamicPortArray != null) {
            disableDynamicPorts.addAll(Arrays.asList(dynamicPortArray));
        }

        // lets create the ports in sorted order
        for (Map.Entry<Integer, String> entry : sortedInternalPorts.entrySet()) {
            Integer port = entry.getKey();
            String portName = entry.getValue();
            int externalPort = port;
            environmentVariables.put("FABRIC8_" + portName + "_PORT", "" + port);
            String proxyPortEnvName = "FABRIC8_" + portName + "_PROXY_PORT";

            // lets allow the proxy ports to be defined from the outside as an environment variable
            Integer currentProxyPort = null;
            String currentExternalPortText = environmentVariables.get(proxyPortEnvName);
            if (currentExternalPortText != null) {
                try {
                    currentProxyPort = Integer.parseInt(currentExternalPortText);
                } catch (NumberFormatException e) {
                    LOG.warn("Could not parse env var " + proxyPortEnvName + " of " + currentExternalPortText + " as a number: " + e, e);
                }
            }
            if (currentProxyPort != null) {
                externalPort = currentProxyPort;
            } else {
                if (!disableDynamicPorts.contains(portName)) {
                    externalPort = owner.createExternalPort(containerId, portName, usedPortByHost, options);
                }
                environmentVariables.put(proxyPortEnvName, "" + externalPort);
            }
            externalPorts.put(portName, externalPort);
            if (portName.equals(JolokiaAgentHelper.JOLOKIA_PORT_NAME)) {
                jolokiaUrl = "http://" + listenHost + ":" + externalPort + "/jolokia/";
                LOG.info("Found Jolokia URL: " + jolokiaUrl);
            }
        }
        if (processConfig.isCreateLocalContainerAddress()) {
            environmentVariables.put(EnvironmentVariables.FABRIC8_LOCAL_CONTAINER_ADDRESS, owner.createContainerLocalAddress(containerId, options));
        }

        if (jolokiaUrl != null) {
            registerJolokiaUrl(container, jolokiaUrl);
        }
    }

    protected void publishZooKeeperValues(CreateChildContainerOptions options, ProcessContainerConfig processConfig, Container container, Map<String, String> environmentVariables) {
        Map<String, Map<String, String>> publishConfigurations = Profiles.getOverlayFactoryConfigurations(fabricService, options.getProfiles(), options.getVersion(), ZooKeeperPublishConfig.PROCESS_CONTAINER_ZK_PUBLISH_PID);
        Set<Map.Entry<String, Map<String, String>>> entries = publishConfigurations.entrySet();
        for (Map.Entry<String, Map<String, String>> entry : entries) {
            String configName = entry.getKey();
            Map<String, String> exportConfig = entry.getValue();

            if (exportConfig != null && !exportConfig.isEmpty()) {
                JolokiaAgentHelper.substituteEnvironmentVariableExpressions(exportConfig, environmentVariables);
                ZooKeeperPublishConfig config = new ZooKeeperPublishConfig();
                try {
                    configurer.configure(exportConfig, config);
                    config.publish(curator, options, processConfig, container, environmentVariables);
                } catch (Exception e) {
                    LOG.warn("Failed to publish configuration " + configName + " of " + config + " due to: " + e, e);
                }
            }
        }
    }


    protected Installation getInstallation(Container container) {
        return processManager.getInstallation(container.getId());
    }

    protected void handleException(String message, Exception cause) {
        throw new RuntimeException(message + ". " + cause, cause);
    }
}

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
package io.fabric8.docker.provider;

import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.EnvironmentVariables;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Profiles;
import io.fabric8.api.Version;
import io.fabric8.api.ZkDefs;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.Configurer;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.common.util.Strings;
import io.fabric8.container.process.JavaContainerConfig;
import io.fabric8.container.process.JolokiaAgentHelper;
import io.fabric8.container.process.ZooKeeperPublishConfig;
import io.fabric8.docker.api.Docker;
import io.fabric8.docker.api.DockerFactory;
import io.fabric8.docker.api.Dockers;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.provider.customizer.CustomDockerContainerImageBuilder;
import io.fabric8.docker.provider.customizer.CustomDockerContainerImageOptions;
import io.fabric8.service.child.ChildContainers;
import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.scr.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Base class for docker centric docker container providers
 */
public abstract class DockerContainerProviderSupport extends AbstractComponent {
    private static final transient Logger LOG = LoggerFactory.getLogger(DockerContainerProviderSupport.class);


    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
    protected DockerFactory dockerFactory = new DockerFactory();
    protected Docker docker;

    protected abstract FabricService getFabricService();

    protected abstract CuratorFramework getCuratorFramework();

    protected abstract Configurer getConfigurer();

    protected abstract int createExternalPort(String containerId, String portKey, Set<Integer> usedPortByHost, DockerCreateOptions options);

    protected abstract Set<Integer> findUsedPortByHostAndDocker();

    public abstract String getDockerAddress();

    protected DockerProviderConfig createDockerProviderConfig(Map<String, String> dockerProviderConfig, Map<String, String> environmentVariables) throws Exception {
        FabricService service = getFabricService();
        JolokiaAgentHelper.substituteEnvironmentVariableExpressions(dockerProviderConfig, environmentVariables, service, getCuratorFramework());
        DockerProviderConfig dockerProviderConfigObject = new DockerProviderConfig();
        getConfigurer().configure(dockerProviderConfig, dockerProviderConfigObject);
        return dockerProviderConfigObject;
    }


    protected void publishZooKeeperValues(DockerCreateOptions options, Map<String, String> environmentVariables) {
        Map<String, Map<String, String>> publishConfigurations = Profiles.getOverlayFactoryConfigurations(getFabricService(), options.getProfiles(), options.getVersion(), ZooKeeperPublishConfig.PROCESS_CONTAINER_ZK_PUBLISH_PID);
        Set<Map.Entry<String, Map<String, String>>> entries = publishConfigurations.entrySet();
        for (Map.Entry<String, Map<String, String>> entry : entries) {
            String configName = entry.getKey();
            Map<String, String> exportConfig = entry.getValue();

            CuratorFramework curatorFramework = getCuratorFramework();
            if (exportConfig != null && !exportConfig.isEmpty() && curatorFramework != null) {
                JolokiaAgentHelper.substituteEnvironmentVariableExpressions(exportConfig, environmentVariables, getFabricService(), curatorFramework, true);
                ZooKeeperPublishConfig config = new ZooKeeperPublishConfig();
                try {
                    getConfigurer().configure(exportConfig, config);
                    config.publish(curatorFramework, null, null, null, environmentVariables);
                } catch (Exception e) {
                    LOG.warn("Failed to publish configuration " + configName + " of " + config + " due to: " + e, e);
                }
            }
        }
    }

    public static CreateDockerContainerMetadata newInstance(ContainerConfig containerConfig, DockerCreateOptions options, ContainerCreateStatus status, String containerType) {
        List<String> warnings = new ArrayList<String>();
        String[] warningArray = status.getWarnings();
        if (warningArray != null) {
            Collections.addAll(warnings, warningArray);
        }
        CreateDockerContainerMetadata metadata = new CreateDockerContainerMetadata(status.getId(), warnings);
        String containerId = options.getName();
        metadata.setContainerName(containerId);
        metadata.setContainerType(containerType);
        metadata.setOverridenResolver(ZkDefs.MANUAL_IP);
        metadata.setCreateOptions(options);
        return metadata;
    }

    protected ContainerConfig createContainerConfig(DockerCreateOptions options) {
        ContainerConfig containerConfig = new ContainerConfig();
        containerConfig.setImage(options.getImage());
        List<String> cmdList = options.getCmd();
        if (cmdList != null && cmdList.size() > 0) {
            containerConfig.setCmd(cmdList.toArray(new String[cmdList.size()]));
        }
        containerConfig.setEntrypoint(options.getEntrypoint());
        String workingDir = options.getWorkingDir();
        if (workingDir != null) {
            containerConfig.setWorkingDir(workingDir);
        }
        containerConfig.setAttachStdout(true);
        containerConfig.setAttachStderr(true);
        containerConfig.setTty(true);
        return containerConfig;
    }


    public class DockerCreateContainerParameters {
        private DockerCreateOptions options;
        private ContainerConfig containerConfig;
        private Map<String, String> environmentVariables;
        private String containerType;
        private String jolokiaUrl;

        public DockerCreateContainerParameters(DockerCreateOptions options) throws Exception {
            this.options = options;
            assertValid();

            String containerId = options.getName();
            ContainerConfig containerConfig = createContainerConfig(options);

            // allow values to be extracted from the profile configuration
            // such as the image
            Set<String> profileIds = options.getProfiles();
            String versionId = options.getVersion();
            FabricService service = getFabricService();
            Map<String, String> configOverlay = new HashMap<>();
            Map<String, String> ports = null;
            Map<String, String> dockerProviderConfig = new HashMap<>();

            List<Profile> profileOverlays = new ArrayList<>();
            Version version = null;
            if (profileIds != null && versionId != null) {
                ProfileService profileService = service.adapt(ProfileService.class);
                version = profileService.getVersion(versionId);
                if (version != null) {
                    for (String profileId : profileIds) {
                        Profile profile = version.getRequiredProfile(profileId);
                        if (profile != null) {
                            Profile overlay = profileService.getOverlayProfile(profile);
                            profileOverlays.add(overlay);
                            Map<String, String> dockerConfig = overlay.getConfiguration(DockerConstants.DOCKER_PROVIDER_PID);
                            if (dockerConfig != null) {
                                configOverlay.putAll(dockerConfig);
                            }
                            if (ports == null || ports.size() == 0) {
                                ports = overlay.getConfiguration(Constants.PORTS_PID);
                            }
                        }
                    }
                    if (version.hasProfile(DockerConstants.DOCKER_PROVIDER_PROFILE_ID)) {
                        Profile profile = version.getRequiredProfile(DockerConstants.DOCKER_PROVIDER_PROFILE_ID);
                        if (profile != null) {
                            Profile overlay = profileService.getOverlayProfile(profile);
                            Map<String, String> dockerConfig = overlay.getConfiguration(DockerConstants.DOCKER_PROVIDER_PID);
                            if (dockerConfig != null) {
                                dockerProviderConfig.putAll(dockerConfig);
                            }
                        }
                    }
                }
            }
            if (ports == null || ports.size() == 0) {
                // lets find the defaults from the docker profile
                if (version == null) {
                    version = service.getRequiredDefaultVersion();
                }
                Profile dockerProfile = version.getRequiredProfile("docker");
                ports = dockerProfile.getConfiguration(Constants.PORTS_PID);
                if (ports == null || ports.size() == 0) {
                    LOG.warn("Could not a docker ports configuration for: " + Constants.PORTS_PID);
                    ports = new HashMap<String, String>();
                }
            }
            LOG.info("Got port configuration: " + ports);

            environmentVariables = ChildContainers.getEnvironmentVariables(service, options, DockerConstants.SCHEME);

            DockerProviderConfig configOverlayDockerProvider = createDockerProviderConfig(configOverlay, environmentVariables);

            CuratorFramework curatorOptional = getCuratorFramework();
            String image = JolokiaAgentHelper.substituteVariableExpression(containerConfig.getImage(), environmentVariables, service, curatorOptional, true);

            if (Strings.isNullOrBlank(image)) {
                image = configOverlayDockerProvider.getImage();
                if (Strings.isNullOrBlank(image)) {
                    DockerProviderConfig dockerProviderConfigObject = createDockerProviderConfig(dockerProviderConfig, environmentVariables);
                    image = dockerProviderConfigObject.getImage();
                }
                if (Strings.isNullOrBlank(image)) {
                    image = System.getenv(DockerConstants.EnvironmentVariables.FABRIC8_DOCKER_DEFAULT_IMAGE);
                }
                if (Strings.isNullOrBlank(image)) {
                    image = DockerConstants.DEFAULT_IMAGE;
                }
                containerConfig.setImage(image);
            }
            containerType = "docker " + image;
            Container container = service.getContainer(containerId);
            if (container != null) {
                container.setType(containerType);
            }


            String[] cmd = containerConfig.getCmd();
            if (cmd == null || cmd.length == 0) {
                String value = configOverlayDockerProvider.getCmd();
                if (Strings.isNullOrBlank(value)) {
                    cmd = null;
                } else {
                    cmd = new String[]{value};
                }
                containerConfig.setCmd(cmd);
            }

            Map<String, Integer> internalPorts = options.getInternalPorts();
            Map<String, Integer> externalPorts = options.getExternalPorts();

            Map<String, Object> exposedPorts = new HashMap<>();
            Set<Integer> usedPortByHost = findUsedPortByHostAndDocker();
            Map<String, String> emptyMap = new HashMap<>();

            SortedMap<Integer, String> sortedInternalPorts = new TreeMap<>();
            for (Map.Entry<String, String> portEntry : ports.entrySet()) {
                String portName = portEntry.getKey();
                String portText = portEntry.getValue();
                if (portText != null && !Strings.isNullOrBlank(portText)) {
                    Integer port = null;
                    try {
                        port = Integer.parseInt(portText);
                    } catch (NumberFormatException e) {
                        LOG.warn("Ignoring bad port number for " + portName + " value '" + portText + "' in PID: " + Constants.PORTS_PID);
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

            String dockerHost = dockerFactory.getDockerHost();
            jolokiaUrl = null;

            Map<String, String> javaContainerConfig = Profiles.getOverlayConfiguration(service, profileIds, versionId, Constants.JAVA_CONTAINER_PID);
            JavaContainerConfig javaConfig = new JavaContainerConfig();
            getConfigurer().configure(javaContainerConfig, javaConfig);

            boolean isJavaContainer = ChildContainers.isJavaContainer(getFabricService(), options);

            // lets create the ports in sorted order
            for (Map.Entry<Integer, String> entry : sortedInternalPorts.entrySet()) {
                Integer port = entry.getKey();
                String portName = entry.getValue();
                int externalPort = createExternalPort(containerId, portName, usedPortByHost, options);
                externalPorts.put(portName, externalPort);
                environmentVariables.put("FABRIC8_" + portName + "_PORT", "" + port);
                environmentVariables.put("FABRIC8_" + portName + "_PROXY_PORT", "" + externalPort);

                if (portName.equals(JolokiaAgentHelper.JOLOKIA_PORT_NAME)) {
                    jolokiaUrl = "http://" + dockerHost + ":" + externalPort + "/jolokia/";
                    LOG.info("Found Jolokia URL: " + jolokiaUrl);

                    JolokiaAgentHelper.substituteEnvironmentVariables(javaConfig, environmentVariables, isJavaContainer, JolokiaAgentHelper.getJolokiaPortOverride(port), JolokiaAgentHelper.getJolokiaAgentIdOverride(getFabricService().getEnvironment()));
                } else {
                    JolokiaAgentHelper.substituteEnvironmentVariables(javaConfig, environmentVariables, isJavaContainer, JolokiaAgentHelper.getJolokiaAgentIdOverride(getFabricService().getEnvironment()));

                }
            }
            javaConfig.updateEnvironmentVariables(environmentVariables, isJavaContainer);


            LOG.info("Passing in manual ip: " + dockerHost);
            environmentVariables.put(EnvironmentVariables.FABRIC8_MANUALIP, dockerHost);
            if (container != null) {
                container.setManualIp(dockerHost);
            }
            if (!environmentVariables.containsKey(EnvironmentVariables.FABRIC8_LISTEN_ADDRESS)) {
                environmentVariables.put(EnvironmentVariables.FABRIC8_LISTEN_ADDRESS, dockerHost);
            }
            environmentVariables.put(EnvironmentVariables.FABRIC8_GLOBAL_RESOLVER, ZkDefs.MANUAL_IP);
            environmentVariables.put(EnvironmentVariables.FABRIC8_FABRIC_ENVIRONMENT, DockerConstants.SCHEME);

            // now the environment variables are all set lets see if we need to make a custom image
            String libDir = configOverlayDockerProvider.getJavaLibraryPath();
            String deployDir = configOverlayDockerProvider.getJavaDeployPath();
            String homeDir = configOverlayDockerProvider.getHomePath();
            if (Strings.isNotBlank(libDir) || Strings.isNotBlank(deployDir)) {
                if (container != null) {
                    container.setProvisionResult("preparing");
                    container.setAlive(true);
                }
                String imageRepository = configOverlayDockerProvider.getImageRepository();
                String entryPoint = configOverlayDockerProvider.getImageEntryPoint();
                List<String> names = new ArrayList<String>(profileIds);
                names.add(versionId);
                String tag = "fabric8-" + Strings.join(names, "-").replace('.', '-');

                CustomDockerContainerImageBuilder builder = new CustomDockerContainerImageBuilder();
                CustomDockerContainerImageOptions customDockerContainerImageOptions = new CustomDockerContainerImageOptions(image, imageRepository, tag, libDir, deployDir, homeDir, entryPoint, configOverlayDockerProvider.getOverlayFolder());

                String actualImage = builder.generateContainerImage(service, container, profileOverlays, docker, customDockerContainerImageOptions, javaConfig, options, downloadExecutor, environmentVariables);
                if (actualImage != null) {
                    containerConfig.setImage(actualImage);
                }
            }

            JolokiaAgentHelper.substituteEnvironmentVariableExpressions(environmentVariables, environmentVariables, service, curatorOptional, false);

            List<String> env = containerConfig.getEnv();
            if (env == null) {
                env = new ArrayList<>();
            }
            Dockers.addEnvironmentVariablesToList(env, environmentVariables);
            containerConfig.setExposedPorts(exposedPorts);
            containerConfig.setEnv(env);

            String name = options.getName();

            LOG.info("Creating container on docker: " + getDockerAddress() + " name: " + name + " env vars: " + env);
            LOG.info("Creating container with config: " + containerConfig);
        }

        public ContainerConfig getContainerConfig() {
            return containerConfig;
        }

        public Map<String, String> getEnvironmentVariables() {
            return environmentVariables;
        }

        public String getContainerType() {
            return containerType;
        }

        public String getJolokiaUrl() {
            return jolokiaUrl;
        }

        public DockerCreateContainerParameters invoke() throws Exception {
            assertValid();

            String containerId = options.getName();
            containerConfig = createContainerConfig(options);

            // allow values to be extracted from the profile configuration
            // such as the image
            Set<String> profileIds = options.getProfiles();
            String versionId = options.getVersion();
            FabricService service = getFabricService();
            Map<String, String> configOverlay = new HashMap<>();
            Map<String, String> ports = null;
            Map<String, String> dockerProviderConfig = new HashMap<>();

            List<Profile> profileOverlays = new ArrayList<>();
            Version version = null;
            if (profileIds != null && versionId != null) {
                ProfileService profileService = service.adapt(ProfileService.class);
                version = profileService.getVersion(versionId);
                if (version != null) {
                    for (String profileId : profileIds) {
                        Profile profile = version.getRequiredProfile(profileId);
                        if (profile != null) {
                            Profile overlay = profileService.getOverlayProfile(profile);
                            profileOverlays.add(overlay);
                            Map<String, String> dockerConfig = overlay.getConfiguration(DockerConstants.DOCKER_PROVIDER_PID);
                            if (dockerConfig != null) {
                                configOverlay.putAll(dockerConfig);
                            }
                            if (ports == null || ports.size() == 0) {
                                ports = overlay.getConfiguration(Constants.PORTS_PID);
                            }
                        }
                    }
                    if (version.hasProfile(DockerConstants.DOCKER_PROVIDER_PROFILE_ID)) {
                        Profile profile = version.getRequiredProfile(DockerConstants.DOCKER_PROVIDER_PROFILE_ID);
                        if (profile != null) {
                            Profile overlay = profileService.getOverlayProfile(profile);
                            Map<String, String> dockerConfig = overlay.getConfiguration(DockerConstants.DOCKER_PROVIDER_PID);
                            if (dockerConfig != null) {
                                dockerProviderConfig.putAll(dockerConfig);
                            }
                        }
                    }
                }
            }
            if (ports == null || ports.size() == 0) {
                // lets find the defaults from the docker profile
                if (version == null) {
                    version = service.getRequiredDefaultVersion();
                }
                Profile dockerProfile = version.getRequiredProfile("docker");
                ports = dockerProfile.getConfiguration(Constants.PORTS_PID);
                if (ports == null || ports.size() == 0) {
                    LOG.warn("Could not a docker ports configuration for: " + Constants.PORTS_PID);
                    ports = new HashMap<String, String>();
                }
            }
            LOG.info("Got port configuration: " + ports);

            environmentVariables = ChildContainers.getEnvironmentVariables(service, options, DockerConstants.SCHEME);

            DockerProviderConfig configOverlayDockerProvider = createDockerProviderConfig(configOverlay, environmentVariables);

            CuratorFramework curatorOptional = getCuratorFramework();
            String image = JolokiaAgentHelper.substituteVariableExpression(containerConfig.getImage(), environmentVariables, service, curatorOptional, true);

            if (Strings.isNullOrBlank(image)) {
                image = configOverlayDockerProvider.getImage();
                if (Strings.isNullOrBlank(image)) {
                    DockerProviderConfig dockerProviderConfigObject = createDockerProviderConfig(dockerProviderConfig, environmentVariables);
                    image = dockerProviderConfigObject.getImage();
                }
                if (Strings.isNullOrBlank(image)) {
                    image = System.getenv(DockerConstants.EnvironmentVariables.FABRIC8_DOCKER_DEFAULT_IMAGE);
                }
                if (Strings.isNullOrBlank(image)) {
                    image = DockerConstants.DEFAULT_IMAGE;
                }
                containerConfig.setImage(image);
            }
            containerType = "docker " + image;
            Container container = service.getContainer(containerId);
            if (container != null) {
                container.setType(containerType);
            }


            String[] cmd = containerConfig.getCmd();
            if (cmd == null || cmd.length == 0) {
                String value = configOverlayDockerProvider.getCmd();
                if (Strings.isNullOrBlank(value)) {
                    cmd = null;
                } else {
                    cmd = new String[]{value};
                }
                containerConfig.setCmd(cmd);
            }

            Map<String, Integer> internalPorts = options.getInternalPorts();
            Map<String, Integer> externalPorts = options.getExternalPorts();

            Map<String, Object> exposedPorts = new HashMap<>();
            Set<Integer> usedPortByHost = findUsedPortByHostAndDocker();
            Map<String, String> emptyMap = new HashMap<>();

            SortedMap<Integer, String> sortedInternalPorts = new TreeMap<>();
            for (Map.Entry<String, String> portEntry : ports.entrySet()) {
                String portName = portEntry.getKey();
                String portText = portEntry.getValue();
                if (portText != null && !Strings.isNullOrBlank(portText)) {
                    Integer port = null;
                    try {
                        port = Integer.parseInt(portText);
                    } catch (NumberFormatException e) {
                        LOG.warn("Ignoring bad port number for " + portName + " value '" + portText + "' in PID: " + Constants.PORTS_PID);
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

            String dockerHost = dockerFactory.getDockerHost();
            jolokiaUrl = null;

            Map<String, String> javaContainerConfig = Profiles.getOverlayConfiguration(service, profileIds, versionId, Constants.JAVA_CONTAINER_PID);
            JavaContainerConfig javaConfig = new JavaContainerConfig();
            getConfigurer().configure(javaContainerConfig, javaConfig);

            boolean isJavaContainer = ChildContainers.isJavaContainer(getFabricService(), options);

            // lets create the ports in sorted order
            for (Map.Entry<Integer, String> entry : sortedInternalPorts.entrySet()) {
                Integer port = entry.getKey();
                String portName = entry.getValue();
                int externalPort = createExternalPort(containerId, portName, usedPortByHost, options);
                externalPorts.put(portName, externalPort);
                environmentVariables.put("FABRIC8_" + portName + "_PORT", "" + port);
                environmentVariables.put("FABRIC8_" + portName + "_PROXY_PORT", "" + externalPort);

                if (portName.equals(JolokiaAgentHelper.JOLOKIA_PORT_NAME)) {
                    jolokiaUrl = "http://" + dockerHost + ":" + externalPort + "/jolokia/";
                    LOG.info("Found Jolokia URL: " + jolokiaUrl);

                    JolokiaAgentHelper.substituteEnvironmentVariables(javaConfig, environmentVariables, isJavaContainer, JolokiaAgentHelper.getJolokiaPortOverride(port), JolokiaAgentHelper.getJolokiaAgentIdOverride(getFabricService().getEnvironment()));
                } else {
                    JolokiaAgentHelper.substituteEnvironmentVariables(javaConfig, environmentVariables, isJavaContainer, JolokiaAgentHelper.getJolokiaAgentIdOverride(getFabricService().getEnvironment()));

                }
            }
            javaConfig.updateEnvironmentVariables(environmentVariables, isJavaContainer);


            LOG.info("Passing in manual ip: " + dockerHost);
            environmentVariables.put(EnvironmentVariables.FABRIC8_MANUALIP, dockerHost);
            if (container != null) {
                container.setManualIp(dockerHost);
            }
            if (!environmentVariables.containsKey(EnvironmentVariables.FABRIC8_LISTEN_ADDRESS)) {
                environmentVariables.put(EnvironmentVariables.FABRIC8_LISTEN_ADDRESS, dockerHost);
            }
            environmentVariables.put(EnvironmentVariables.FABRIC8_GLOBAL_RESOLVER, ZkDefs.MANUAL_IP);
            environmentVariables.put(EnvironmentVariables.FABRIC8_FABRIC_ENVIRONMENT, DockerConstants.SCHEME);

            // now the environment variables are all set lets see if we need to make a custom image
            String libDir = configOverlayDockerProvider.getJavaLibraryPath();
            String deployDir = configOverlayDockerProvider.getJavaDeployPath();
            String homeDir = configOverlayDockerProvider.getHomePath();
            if (Strings.isNotBlank(libDir) || Strings.isNotBlank(deployDir)) {
                if (container != null) {
                    container.setProvisionResult("preparing");
                    container.setAlive(true);
                }
                String imageRepository = configOverlayDockerProvider.getImageRepository();
                String entryPoint = configOverlayDockerProvider.getImageEntryPoint();
                List<String> names = new ArrayList<String>(profileIds);
                names.add(versionId);
                String tag = "fabric8-" + Strings.join(names, "-").replace('.', '-');

                CustomDockerContainerImageBuilder builder = new CustomDockerContainerImageBuilder();
                CustomDockerContainerImageOptions customDockerContainerImageOptions = new CustomDockerContainerImageOptions(image, imageRepository, tag, libDir, deployDir, homeDir, entryPoint, configOverlayDockerProvider.getOverlayFolder());

                String actualImage = builder.generateContainerImage(service, container, profileOverlays, docker, customDockerContainerImageOptions, javaConfig, options, downloadExecutor, environmentVariables);
                if (actualImage != null) {
                    containerConfig.setImage(actualImage);
                }
            }

            JolokiaAgentHelper.substituteEnvironmentVariableExpressions(environmentVariables, environmentVariables, service, curatorOptional, false);

            List<String> env = containerConfig.getEnv();
            if (env == null) {
                env = new ArrayList<>();
            }
            Dockers.addEnvironmentVariablesToList(env, environmentVariables);
            containerConfig.setExposedPorts(exposedPorts);
            containerConfig.setEnv(env);

            String name = options.getName();

            LOG.info("Creating container on docker: " + getDockerAddress() + " name: " + name + " env vars: " + env);
            LOG.info("Creating container with config: " + containerConfig);
            return this;
        }
    }
}

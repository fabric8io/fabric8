/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package io.fabric8.docker.provider;


import io.fabric8.api.Container;
import io.fabric8.api.ContainerAutoScaler;
import io.fabric8.api.ContainerAutoScalerFactory;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreationStateListener;
import io.fabric8.api.FabricService;
import io.fabric8.api.NameValidator;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.docker.api.Docker;
import io.fabric8.docker.api.DockerFactory;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.api.container.HostConfig;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.fusesource.insight.log.support.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ThreadSafe
@Component(name = "io.fabric8.container.provider.docker", label = "Fabric8 Docker Container Provider", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
@Service(ContainerProvider.class)
public final class DockerContainerProvider extends AbstractComponent implements ContainerProvider<CreateDockerContainerOptions, CreateDockerContainerMetadata>, ContainerAutoScalerFactory {

    private static final transient Logger LOG = LoggerFactory.getLogger(DockerContainerProvider.class);

    @Reference(referenceInterface = FabricService.class, bind = "bindFabricService", unbind = "unbindFabricService")
    private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();

    @Reference(referenceInterface = MBeanServer.class)
    private MBeanServer mbeanServer;

    private ObjectName objectName;
    private DockerFacade mbean;
    private DockerFactory dockerFactory = new DockerFactory();
    private Docker docker;


    @Activate
    void activate(Map<String, ?> configuration) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        updateConfiguration(configuration);
        activateComponent();
        if (mbeanServer != null) {
            objectName = new ObjectName("io.fabric8:type=Docker");
            mbean = new DockerFacade(this);
            if (!mbeanServer.isRegistered(objectName)) {
                mbeanServer.registerMBean(mbean, objectName);
            }
        } else {
            LOG.warn("No MBeanServer!");
        }
    }

    @Modified
    void modified(Map<String, ?> configuration) {
        updateConfiguration(configuration);
    }

    @Deactivate
    void deactivate() throws MBeanRegistrationException, InstanceNotFoundException {
        if (mbeanServer != null) {
            if (mbeanServer.isRegistered(objectName)) {
                mbeanServer.unregisterMBean(objectName);
            }
        }
        deactivateComponent();
    }

    private void updateConfiguration(Map<String, ?> configuration) {
        Object url = configuration.get("url");
        if (url != null) {
            dockerFactory.setAddress(url.toString());
        }
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            // Resteasy uses the TCCL to load the API
            Thread.currentThread().setContextClassLoader(Docker.class.getClassLoader());
            this.docker = dockerFactory.createDocker();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    FabricService getFabricService() {
        return fabricService.get();
    }

    @Override
    public CreateDockerContainerOptions.Builder newBuilder() {
        return CreateDockerContainerOptions.builder();
    }

    @Override
    public CreateDockerContainerMetadata create(CreateDockerContainerOptions options, CreationStateListener listener) throws Exception {
        assertValid();

        ContainerConfig containerConfig = options.createContainerConfig();

        // allow values to be extracted from the profile configuration
        // such as the image
        Set<String> profiles = options.getProfiles();
        String versionId = options.getVersion();
        FabricService service = fabricService.get();
        Map<String, String> configOverlay = new HashMap<String, String>();
        Map<String, String> envVarsOverlay = new HashMap<String, String>();
        if (profiles != null && versionId != null) {
            Version version = service.getVersion(versionId);
            if (version != null) {
                for (String profileId : profiles) {
                    Profile profile = version.getProfile(profileId);
                    if (profile != null) {
                        Profile overlay = profile.getOverlay();
                        Map<String, String> dockerConfig = overlay.getConfiguration(DockerConstants.DOCKER_PROVIDER_PID);
                        if (dockerConfig != null)  {
                            configOverlay.putAll(dockerConfig);
                        }
                        Map<String, String> envVars = overlay.getConfiguration(DockerConstants.ENVIRONMENT_VARIABLES_PID);
                        if (envVars != null)  {
                            envVarsOverlay.putAll(envVars);
                        }
                    }
                }
            }
        }
        String image = containerConfig.getImage();
        if (Strings.isEmpty(image)) {
            image = configOverlay.get(DockerConstants.PROPERTIES.IMAGE);
            if (Strings.isEmpty(image)) {
                image = DockerConstants.DEFAULT_IMAGE;
            }
            containerConfig.setImage(image);
        }
        String[] cmd = containerConfig.getCmd();
        if (cmd == null || cmd.length == 0) {
            String value = configOverlay.get(DockerConstants.PROPERTIES.CMD);
            if (Strings.isEmpty(value)) {
                cmd = null;
            } else {
                cmd = new String[]{value};
            }
            containerConfig.setCmd(cmd);

        }
        String zookeeperUrl = service.getZookeeperUrl();
        String zookeeperPassword = service.getZookeeperPassword();

        // Docker needs to use the local IP address not "localhost"
        String localIp = service.getCurrentContainer().getLocalIp();
        if (!Strings.isEmpty(localIp)) {
            int idx = zookeeperUrl.lastIndexOf(':');
            if (idx > 0) {
                localIp += zookeeperUrl.substring(idx);
            }
            zookeeperUrl = localIp;
        }

        envVarsOverlay.put(DockerConstants.ENV_VARS.KARAF_NAME, options.getName());
        if (!options.isEnsembleServer()) {
            if (envVarsOverlay.get(DockerConstants.ENV_VARS.ZOOKEEPER_URL) == null) {
                envVarsOverlay.put(DockerConstants.ENV_VARS.ZOOKEEPER_URL, zookeeperUrl);
            }
            if (envVarsOverlay.get(DockerConstants.ENV_VARS.ZOOKEEPER_PASSWORD) == null) {
                envVarsOverlay.put(DockerConstants.ENV_VARS.ZOOKEEPER_PASSWORD, zookeeperPassword);
            }
            if (envVarsOverlay.get(DockerConstants.ENV_VARS.ZOOKEEPER_PASSWORD_ENCODE) == null) {
                String zkPasswordEncode = System.getProperty("zookeeper.password.encode", "true");
                envVarsOverlay.put(DockerConstants.ENV_VARS.ZOOKEEPER_PASSWORD_ENCODE, zkPasswordEncode);
            }
        }
        List<String> env = containerConfig.getEnv();
        if (env == null) {
            env = new ArrayList<String>();
        }
        Set<Map.Entry<String, String>> entries = envVarsOverlay.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null && value != null) {
                env.add(key + "=" + value);
            }
        }
        containerConfig.setEnv(env);

        LOG.info("Creating container: " + containerConfig + " on docker " + getDockerAddress() + " with env vars: " + envVarsOverlay);

        ContainerCreateStatus status = docker.containerCreate(containerConfig);
        LOG.info("Got status: " + status);
        CreateDockerContainerMetadata metadata = CreateDockerContainerMetadata.newInstance(containerConfig, status);
        metadata.setCreateOptions(options);
        startDockerContainer(status.getId());
        return metadata;
    }

    @Override
    public void start(Container container) {
        assertValid();
        String id = getDockerContainerId(container);
        startDockerContainer(id);
    }

    protected void startDockerContainer(String id) {
        if (!Strings.isEmpty(id)) {
            LOG.info("starting container " + id);
            HostConfig hostConfig = new HostConfig();
            // TODO populate host config?
            docker.containerStart(id, hostConfig);
        }
    }

    @Override
    public void stop(Container container) {
        assertValid();
        String id = getDockerContainerId(container);
        if (!Strings.isEmpty(id)) {
            LOG.info("stopping container " + id);
            Integer timeToWait = null;
            docker.containerStop(id, timeToWait);
        }
    }

    @Override
    public void destroy(Container container) {
        assertValid();
        String id = getDockerContainerId(container);
        if (!Strings.isEmpty(id)) {
            LOG.info("destroying container " + id);
            Integer removeVolumes = 1;
            docker.containerRemove(id, removeVolumes);
        }
    }

    @Override
    public String getScheme() {
        assertValid();
        return DockerConstants.SCHEME;
    }

    @Override
    public Class<CreateDockerContainerOptions> getOptionsType() {
        assertValid();
        return CreateDockerContainerOptions.class;
    }

    @Override
    public Class<CreateDockerContainerMetadata> getMetadataType() {
        assertValid();
        return CreateDockerContainerMetadata.class;
    }

    public Docker getDocker() {
        return docker;
    }

    protected String getDockerContainerId(Container container) {
        CreateDockerContainerMetadata containerMetadata = getContainerMetadata(container);
        if (containerMetadata != null) {
            return containerMetadata.getId();
        }
        return container.getId();
    }

    protected static CreateDockerContainerMetadata getContainerMetadata(Container container) {
        CreateContainerMetadata<?> value = container.getMetadata();
        if (value instanceof CreateDockerContainerMetadata) {
            return (CreateDockerContainerMetadata)value;
        } else {
            return null;
        }
    }

    @Override
    public ContainerAutoScaler createAutoScaler() {
        return new DockerAutoScaler(this);
    }


    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }

    public String getDockerAddress() {
        return dockerFactory.getAddress();
    }
}

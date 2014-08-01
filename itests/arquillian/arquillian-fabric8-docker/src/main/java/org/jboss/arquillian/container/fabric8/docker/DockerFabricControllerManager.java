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
package org.jboss.arquillian.container.fabric8.docker;

import io.fabric8.api.EnvironmentVariables;
import io.fabric8.common.util.Strings;
import io.fabric8.docker.api.Docker;
import io.fabric8.docker.api.DockerFactory;
import io.fabric8.docker.api.Dockers;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.api.container.ContainerInfo;
import io.fabric8.docker.api.container.HostConfig;
import io.fabric8.docker.api.container.NetworkSettings;
import io.fabric8.testkit.FabricController;
import io.fabric8.testkit.jolokia.JolokiaFabricController;
import io.fabric8.testkit.support.FabricControllerManagerSupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Uses the Docker API to create a fabric
 */
public class DockerFabricControllerManager extends FabricControllerManagerSupport {
    private final Fabric8DockerContainerConfiguration config;
    private final DockerFactory dockerFactory;
    private Docker docker;
    private String rootDockerContainerId;

    public DockerFabricControllerManager(Fabric8DockerContainerConfiguration config) {
        this.config = config;
        dockerFactory = new DockerFactory();
        docker = dockerFactory.createDocker();
        setAllowInheritedEnvironmentVariables(new String[0]);
    }

    @Override
    public FabricController createFabric() throws Exception {
        try {
            // lets create the container
            String image = config.getFabricDockerImage();
            String containerName = "fabric1";
            //String dockerHost = dockerFactory.getDockerHost();
            String dockerHost = "localhost";

            ContainerConfig containerConfig = new ContainerConfig();
            containerConfig.setImage(image);
            containerConfig.setAttachStdout(true);
            containerConfig.setAttachStderr(true);
            containerConfig.setTty(true);
            containerConfig.setEntrypoint(null);

            Map<String, Object> exposedPorts = new HashMap<>();

            Map<String, String> emptyMap = new HashMap<>();
            int[] rootContainerExposedPorts = config.getRootContainerExposedPorts();
            for (int exposedPort : rootContainerExposedPorts) {
                String portText = "" + exposedPort + "/tcp";
                exposedPorts.put(portText, emptyMap);
            }
            containerConfig.setExposedPorts(exposedPorts);
            System.out.println("Exposing ports: " + exposedPorts);

            Map<String, String> envVars = createChildEnvironmentVariables();
            envVars.put(EnvironmentVariables.FABRIC8_GLOBAL_RESOLVER, "manualip");
            envVars.put(EnvironmentVariables.FABRIC8_MANUALIP, dockerHost);
            envVars.put(EnvironmentVariables.RUNTIME_ID, containerName);
            envVars.put(EnvironmentVariables.KARAF_NAME, containerName);
            List<String> envList = Dockers.toEnvList(envVars);
            containerConfig.setEnv(envList);
            System.out.println("Creating docker container name " + containerName
                    + " with config: " + containerConfig);
            ContainerCreateStatus status = docker.containerCreate(containerConfig, containerName);
            rootDockerContainerId = status.getId();
            String[] warnings = status.getWarnings();
            if (warnings != null) {
                for (String warning : warnings) {
                    System.out.println("WARNING: " + warning);
                }
            }
            System.out.println("Created docker container id " + rootDockerContainerId);
            assertTrue("Should not have a blank docker container id for fabric8 container " + containerName, Strings.isNotBlank(rootDockerContainerId));

            // lets start the container
            HostConfig hostConfig = new HostConfig();
            hostConfig.setPublishAllPorts(true);
            System.out.println("Starting docker container id " + rootDockerContainerId);
            docker.containerStart(rootDockerContainerId, hostConfig);

            ContainerInfo containerInfo = docker.containerInspect(rootDockerContainerId);
            System.out.println("Inspected container got: " + containerInfo);

            NetworkSettings networkSettings = containerInfo.getNetworkSettings();
            assertNotNull("No NetworkSettings available for docker container: " + rootDockerContainerId, networkSettings);
            Map<String, Object> ports = networkSettings.getPorts();
            String webPortText = "8181/tcp";
            Object bindingObject = ports.get(webPortText);
            if (bindingObject instanceof List) {
                List<Map<String, String>> bindingList = (List<Map<String, String>>) bindingObject;
                assertTrue("Binding list for port 8181/tcp is empty for docker container: " + rootDockerContainerId, bindingList.size() > 0);
                assertNotNull("No HostConfig binding for 8181/tcp for docker container: " + rootDockerContainerId, bindingList);
                Map<String, String> binding = bindingList.get(0);
                String bindingText = assertMandatoryEntry(binding, "HostPort", "Docker container " + rootDockerContainerId);
                assertTrue("Should have non blank bindingText for 8181/tcp", Strings.isNotBlank(bindingText));

                String jolokiaUrl = "http://" + dockerHost + ":" + bindingText + "/jolokia";
                System.out.println("Got jolokia URL: " + jolokiaUrl);

                return new JolokiaFabricController(jolokiaUrl);
            } else {
                fail("Could not find binding for port " + webPortText + " in ports + " + ports);
                return null;
            }
        } catch (Exception e) {
            String message = Dockers.dockerErrorMessage(e);
            if (Strings.isNotBlank(message)) {
                throw new Exception("Docker failure: " + message + ". " + e, e);
            } else {
                throw e;
            }
        }
    }

    public static String assertMandatoryEntry(Map<String, String> map, String key, String message) {
        String actual = map.get(key);
        assertNotNull("No entry for key: " + key + " for " + message + " in map: " + map, actual);
        return actual;
    }

    @Override
    public void destroy() throws Exception {
    }
}

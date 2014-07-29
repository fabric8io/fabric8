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

import io.fabric8.common.util.Strings;
import io.fabric8.docker.api.Docker;
import io.fabric8.docker.api.DockerFactory;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.api.container.ContainerInfo;
import io.fabric8.docker.api.container.HostConfig;
import io.fabric8.testkit.FabricController;
import io.fabric8.testkit.jolokia.JolokiaFabricController;
import io.fabric8.testkit.support.FabricControllerManagerSupport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    }

    @Override
    public FabricController createFabric() throws Exception {
        String image = config.getFabricDockerImage();

        ContainerConfig containerConfig = new ContainerConfig();
        containerConfig.setImage(image);
        containerConfig.setAttachStdout(true);
        containerConfig.setAttachStderr(true);
        containerConfig.setTty(true);

        Map<String, Object> exposedPorts = new HashMap<>();

        Map<String, String> emptyMap = new HashMap<>();
        int[] rootContainerExposedPorts = config.getRootContainerExposedPorts();
        for (int exposedPort : rootContainerExposedPorts) {
            String portText = "" + exposedPort + "/tcp";
            exposedPorts.put(portText, emptyMap);
        }
        containerConfig.setExposedPorts(exposedPorts);

        String containerName = "root";
        ContainerCreateStatus status = docker.containerCreate(containerConfig, containerName);
        rootDockerContainerId = status.getId();
        System.out.println("Created docker container id " + rootDockerContainerId);
        assertTrue("Should not have a blank docker container id for fabric8 container " + containerName, Strings.isNotBlank(rootDockerContainerId));

        // now lets find the Jolokia URL based on the exposed ports...
        ContainerInfo containerInfo = docker.containerInspect(rootDockerContainerId);
        HostConfig hostConfig = containerInfo.getHostConfig();
        assertNotNull("No HostConfig available for docker container: " + rootDockerContainerId, hostConfig);

        Map<String, List<Map<String, String>>> bindings = hostConfig.getPortBindings();
        List<Map<String, String>> bindingList = bindings.get("8181/tcp");
        if (bindingList == null) {
            bindingList =  bindings.get("8080/tcp");
        }
        assertNotNull("No HostConfig binding for 8181/tcp for docker container: " + rootDockerContainerId, bindingList);
        assertTrue("Binding list for port 8181/tcp is empty for docker container: " + rootDockerContainerId, bindingList.size() > 0);
        Map<String, String> binding = bindingList.get(0);
        String bindingText = assertMandatoryEntry(binding, "HostPort", "Docker container " + rootDockerContainerId);
        assertTrue("Should have non blank bindingText for 8181/tcp", Strings.isNotBlank(bindingText));

        String jolokiaUrl = "http://localhost:" + bindingText;
        System.out.println("Got jolokia URL: " + jolokiaUrl);

        return new JolokiaFabricController(jolokiaUrl);
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

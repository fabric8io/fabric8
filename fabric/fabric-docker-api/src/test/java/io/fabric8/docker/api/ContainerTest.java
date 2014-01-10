/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
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

package io.fabric8.docker.api;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;
import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.api.container.ContainerInfo;
import io.fabric8.docker.api.container.Top;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ContainerTest extends DockerBaseTest {

    @Test
    public void testListContainers() throws IOException {
        String json = Resources.toString(getResource("container/containers-all.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        List<Container> containers = docker.containers(1, 1, null, null, 1);
        assertNotNull(containers);
        assertEquals(containers.size(), 4);
        assertContainers(containers);
    }

    @Test
    public void testContainerInspect() throws IOException {
        String json = Resources.toString(getResource("container/inspect-4fa6e0f0c678.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        ContainerInfo containerInfo = docker.containerInspect("4fa6e0f0c678");
        assertNotNull(containerInfo);
    }


    @Test
    public void testContainerCreate() throws IOException {
        String json = Resources.toString(getResource("container/create-response.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);
        ContainerConfig cfg = new ContainerConfig();
        cfg.setImage("base");
        cfg.setCmd(new String[]{"date"});
        ContainerCreateStatus containerCreateStatus = docker.containerCreate(cfg);
        assertNotNull(containerCreateStatus);
        assertEquals(containerCreateStatus.getId(), "e90e34656806");
    }

    @Test
    public void testContainerTop() throws IOException {
        String json = Resources.toString(getResource("container/container-top.json"), Charsets.UTF_8);
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.play();
        Docker docker = createDockerForMock(server);

        Top top = docker.containerTop("e90e34656806");
        assertNotNull(top);
        assertEquals(top.getProcesses().length, 2);
    }


    private void assertContainers(List<Container> containers) {
        for (Container container : containers) {
            assertNotNull(container.getId());
            assertNotNull(container.getImage());
        }
    }
}

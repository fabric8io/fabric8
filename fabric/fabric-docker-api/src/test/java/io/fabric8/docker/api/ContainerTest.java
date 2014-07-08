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
package io.fabric8.docker.api;

import io.fabric8.docker.api.container.ContainerConfig;
import io.fabric8.docker.api.container.ContainerCreateStatus;
import io.fabric8.docker.api.container.ContainerInfo;
import io.fabric8.docker.api.container.Top;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ContainerTest extends DockerBaseTest {

    @Test
    @Ignore("[FABRIC-1092] Fix Docker API tests")
    public void testListContainers() throws IOException {
        recordResponse("container/containers-all");
        List<Container> containers = docker.containers(1, 1, null, null, 1);
        assertNotNull(containers);
        assertEquals(containers.size(), 4);
        assertContainers(containers);
    }

    @Test
    public void testContainerInspect() throws IOException {
        recordResponse("container/inspect-4fa6e0f0c678");
        ContainerInfo containerInfo = docker.containerInspect("4fa6e0f0c678");
        assertNotNull(containerInfo);
    }


    @Test
    public void testContainerCreate() throws IOException {
        recordResponse("container/create-response");
        ContainerConfig cfg = new ContainerConfig();
        cfg.setImage("base");
        cfg.setCmd(new String[]{"date"});
        ContainerCreateStatus containerCreateStatus = docker.containerCreate(cfg, null);
        assertNotNull(containerCreateStatus);
        assertEquals(containerCreateStatus.getId(), "e90e34656806");
    }

    @Test
    public void testContainerTop() throws IOException {
        recordResponse("container/container-top");

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

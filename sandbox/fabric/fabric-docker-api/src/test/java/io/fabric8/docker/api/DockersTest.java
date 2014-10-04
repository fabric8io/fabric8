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

import io.fabric8.docker.api.container.Port;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.ProcessingException;

import java.util.Arrays;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

public class DockersTest extends Assert {

    Docker docker = mock(Docker.class);

    @Test(expected = DockerApiConnectionException.class)
    public void shouldWrapJaxRsException() {
        given(docker.containers(anyInt(), anyInt(), anyString(), anyString(), anyInt())).
                willThrow(ProcessingException.class);

        Dockers.getUsedPorts(docker);
    }

    @Test
    public void shouldRetrievePublicPort() {
        Port port = new Port();
        port.setPrivatePort(1);
        port.setPublicPort(2);
        Container container = new Container();
        container.setPorts(Arrays.asList(port));
        given(docker.containers(anyInt(), anyInt(), anyString(), anyString(), anyInt())).
                willReturn(Arrays.asList(container));

        // When
        Set<Integer> ports = Dockers.getUsedPorts(docker);

        // Then
        assertEquals(1, ports.size());
        assertEquals(port.getPublicPort(), ports.iterator().next());
    }

}

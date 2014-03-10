/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
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
package io.fabric8.itests.paxexam.support;

import io.fabric8.api.Container;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

public class ProvisionTest {

    @Test
    public void testContainerStatus() throws Exception {
        Container container = createMock(Container.class);
        expect(container.getId()).andReturn("container").anyTimes();
        expect(container.isAlive()).andReturn(true).anyTimes();
        expect(container.getProvisionStatus()).andReturn("success").anyTimes();
        expect(container.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(container.getProvisionException()).andReturn(null).anyTimes();

        replay(container);

        assertTrue(Provision.containerStatus(Arrays.asList(container), 10000L));
        verify(container);
    }

    @Test
    public void testProvisionSuccess() throws Exception {
        Container container = createMock(Container.class);
        expect(container.getId()).andReturn("container").anyTimes();
        expect(container.isAlive()).andReturn(true).anyTimes();
        expect(container.isAliveAndOK()).andReturn(true).anyTimes();
        expect(container.getProvisionStatus()).andReturn("success").anyTimes();
        expect(container.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(container.getProvisionException()).andReturn(null).anyTimes();
        makeThreadSafe(container, true);

        replay(container);

        Provision.provisioningSuccess(Arrays.asList(container), 10000L, ContainerCallback.DO_NOTHING);
        verify(container);
    }

    @Test
    public void testMultipleContainerStatus() throws Exception {
        Container c1 = createMock(Container.class);
        expect(c1.getId()).andReturn("c1").anyTimes();
        expect(c1.isAlive()).andReturn(true).anyTimes();
        expect(c1.getProvisionStatus()).andReturn("success").anyTimes();
        expect(c1.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(c1.getProvisionException()).andReturn(null).anyTimes();

        Container c2 = createMock(Container.class);
        expect(c2.getId()).andReturn("c2").anyTimes();
        expect(c2.isAlive()).andReturn(true).anyTimes();
        expect(c2.getProvisionStatus()).andReturn("success").anyTimes();
        expect(c2.getSshUrl()).andReturn("container2:8181").anyTimes();
        expect(c2.getProvisionException()).andReturn(null).anyTimes();

        replay(c1, c2);

        assertTrue(Provision.containerStatus(Arrays.asList(c1, c2), 10000L));
        verify(c1, c2);
    }

    @Test
    public void testMultipleProvisionSuccess() throws Exception {
        Container c1 = createMock(Container.class);
        expect(c1.getId()).andReturn("c1").anyTimes();
        expect(c1.isAlive()).andReturn(true).anyTimes();
        expect(c1.getProvisionStatus()).andReturn("success").anyTimes();
        expect(c1.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(c1.getProvisionException()).andReturn(null).anyTimes();

        Container c2 = createMock(Container.class);
        expect(c2.getId()).andReturn("c2").anyTimes();
        expect(c2.isAlive()).andReturn(true).anyTimes();
        expect(c2.getProvisionStatus()).andReturn("success").anyTimes();
        expect(c2.getSshUrl()).andReturn("container2:8181").anyTimes();
        expect(c2.getProvisionException()).andReturn(null).anyTimes();
        makeThreadSafe(c1, true);
        makeThreadSafe(c2, true);
        replay(c1, c2);

        Provision.provisioningSuccess(Arrays.asList(c1, c2), 10000L, ContainerCallback.DO_NOTHING);
        verify(c1, c2);
    }


    @Test
    public void testMultipleContainerStatusWithStages() throws Exception {
        Container c1 = createMock(Container.class);
        expect(c1.getId()).andReturn("c1").anyTimes();
        expect(c1.isManaged()).andReturn(true).anyTimes();
        expect(c1.isAlive()).andReturn(true).anyTimes();
        expect(c1.getProvisionStatus()).andReturn("").times(3);
        expect(c1.getProvisionStatus()).andReturn("analyzing").times(3);
        expect(c1.getProvisionStatus()).andReturn("installing").times(3);
        expect(c1.getProvisionStatus()).andReturn("success").anyTimes();
        expect(c1.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(c1.getProvisionException()).andReturn(null).anyTimes();

        Container c2 = createMock(Container.class);
        expect(c2.getId()).andReturn("c2").anyTimes();
        expect(c2.isManaged()).andReturn(true).anyTimes();
        expect(c2.isAlive()).andReturn(false).times(3);
        expect(c2.isAlive()).andReturn(true).anyTimes();
        expect(c2.getProvisionStatus()).andReturn("").times(3);
        expect(c2.getProvisionStatus()).andReturn("analyzing").times(3);
        expect(c2.getProvisionStatus()).andReturn("installing").times(3);
        expect(c2.getProvisionStatus()).andReturn("success").anyTimes();
        expect(c2.getSshUrl()).andReturn("container2:8181").anyTimes();
        expect(c2.getProvisionException()).andReturn(null).anyTimes();

        replay(c1, c2);

        assertTrue(Provision.containerStatus(Arrays.asList(c1, c2), 10000L));
        verify(c1, c2);
    }

    @Test
    public void testMultipleContainerStatusWithFailure() throws Exception {
        Container c1 = createMock(Container.class);
        expect(c1.getId()).andReturn("c1").anyTimes();
        expect(c1.isManaged()).andReturn(true).anyTimes();
        expect(c1.isAlive()).andReturn(true).anyTimes();
        expect(c1.getProvisionStatus()).andReturn("").times(3);
        expect(c1.getProvisionStatus()).andReturn("analyzing").times(3);
        expect(c1.getProvisionStatus()).andReturn("installing").times(3);
        expect(c1.getProvisionStatus()).andReturn("success").anyTimes();
        expect(c1.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(c1.getProvisionException()).andReturn(null).anyTimes();

        Container c2 = createMock(Container.class);
        expect(c2.getId()).andReturn("c2").anyTimes();
        expect(c2.isManaged()).andReturn(true).anyTimes();
        expect(c2.isAlive()).andReturn(false).times(3);
        expect(c2.isAlive()).andReturn(true).anyTimes();
        expect(c2.getProvisionStatus()).andReturn("").times(3);
        expect(c2.getProvisionStatus()).andReturn("analyzing").times(3);
        expect(c2.getProvisionStatus()).andReturn("installing").times(3);
        expect(c2.getProvisionStatus()).andReturn("error").anyTimes();
        expect(c2.getSshUrl()).andReturn("container2:8181").anyTimes();
        expect(c2.getProvisionException()).andReturn(null).anyTimes();

        replay(c1, c2);

        assertFalse(Provision.containerStatus(Arrays.asList(c1, c2), 10000L));
        verify(c1, c2);
    }

    @Test(expected = ProvisionException.class)
    public void testMultipleProvisionSuccessWithFailure() throws Exception {
        Container c1 = createMock(Container.class);
        expect(c1.getId()).andReturn("c1").anyTimes();
        expect(c1.isManaged()).andReturn(true).anyTimes();
        expect(c1.isAlive()).andReturn(true).anyTimes();
        expect(c1.isAliveAndOK()).andReturn(true).anyTimes();
        expect(c1.getProvisionStatus()).andReturn("success").anyTimes();
        expect(c1.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(c1.getProvisionException()).andReturn(null).anyTimes();

        Container c2 = createMock(Container.class);
        expect(c2.getId()).andReturn("c2").anyTimes();
        expect(c2.isManaged()).andReturn(true).anyTimes();
        expect(c2.isAlive()).andReturn(false).times(3);
        expect(c2.isAlive()).andReturn(true).anyTimes();
        expect(c2.isAliveAndOK()).andReturn(true).anyTimes();
        expect(c2.getProvisionStatus()).andReturn("").times(3);
        expect(c2.getProvisionStatus()).andReturn("analyzing").times(3);
        expect(c2.getProvisionStatus()).andReturn("installing").times(3);
        expect(c2.getProvisionStatus()).andReturn("error").anyTimes();
        expect(c2.getSshUrl()).andReturn("container2:8181").anyTimes();
        expect(c2.getProvisionException()).andReturn(null).anyTimes();
        makeThreadSafe(c1, true);
        makeThreadSafe(c2, true);
        replay(c1, c2);

        Provision.provisioningSuccess(Arrays.asList(c1, c2), 10000L, ContainerCallback.DO_NOTHING);
        verify(c1, c2);
    }

    @Test
    public void testMultipleContainerStatusWithTimeout() throws Exception {
        Container c1 = createMock(Container.class);
        expect(c1.getId()).andReturn("c1").anyTimes();
        expect(c1.isManaged()).andReturn(true).anyTimes();
        expect(c1.isAlive()).andReturn(true).anyTimes();
        expect(c1.getProvisionStatus()).andReturn("").times(3);
        expect(c1.getProvisionStatus()).andReturn("analyzing").times(3);
        expect(c1.getProvisionStatus()).andReturn("installing").times(3);
        expect(c1.getProvisionStatus()).andReturn("success").anyTimes();
        expect(c1.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(c1.getProvisionException()).andReturn(null).anyTimes();

        Container c2 = createMock(Container.class);
        expect(c2.getId()).andReturn("c2").anyTimes();
        expect(c2.isManaged()).andReturn(true).anyTimes();
        expect(c2.isAlive()).andReturn(false).times(3);
        expect(c2.isAlive()).andReturn(true).anyTimes();
        expect(c2.getProvisionStatus()).andReturn("").times(3);
        expect(c2.getProvisionStatus()).andReturn("analyzing").times(3);
        expect(c2.getProvisionStatus()).andReturn("installing").times(3);
        expect(c2.getProvisionStatus()).andReturn("finalizing").anyTimes();
        expect(c2.getSshUrl()).andReturn("container2:8181").anyTimes();
        expect(c2.getProvisionException()).andReturn(null).anyTimes();

        replay(c1, c2);

        assertFalse(Provision.containerStatus(Arrays.asList(c1, c2), 10000L));
        verify(c1, c2);
    }
}

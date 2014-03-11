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
import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

public class WaitForProvisionTaskTest {

    @Test
    public void testStraightSuccess() throws Exception {
        Container container = createMock(Container.class);
        expect(container.getId()).andReturn("container").anyTimes();
        expect(container.isAlive()).andReturn(true).anyTimes();
        expect(container.isManaged()).andReturn(true).anyTimes();
        expect(container.getProvisionStatus()).andReturn("success").anyTimes();
        expect(container.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(container.getProvisionException()).andReturn(null).anyTimes();

        replay(container);

        WaitForProvisionTask task = new WaitForProvisionTask(container, "success", 10000L);
        assertTrue(task.call());
        verify(container);
    }

    @Test
    public void testSuccess() throws Exception {
        Container container = createMock(Container.class);
        expect(container.getId()).andReturn("container").anyTimes();
        expect(container.isAlive()).andReturn(false).times(6);
        expect(container.isAlive()).andReturn(true).anyTimes();
        expect(container.isManaged()).andReturn(true).anyTimes();
        expect(container.getProvisionStatus()).andReturn("").times(3);
        expect(container.getProvisionStatus()).andReturn("analyzing").times(3);
        expect(container.getProvisionStatus()).andReturn("success").anyTimes();
        expect(container.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(container.getProvisionException()).andReturn(null).anyTimes();

        replay(container);

        WaitForProvisionTask task = new WaitForProvisionTask(container, "success", 10000L);
        assertTrue(task.call());
        verify(container);
    }

    @Test
    public void testProvisioningException() throws Exception {
        Container container = createMock(Container.class);
        expect(container.getId()).andReturn("container").anyTimes();
        expect(container.isAlive()).andReturn(true).anyTimes();
        expect(container.isManaged()).andReturn(true).anyTimes();
        expect(container.getProvisionStatus()).andReturn("success").anyTimes();
        expect(container.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(container.getProvisionException()).andReturn(null).times(1);
        expect(container.getProvisionException()).andReturn("Resolution Exception").anyTimes();

        replay(container);

        WaitForProvisionTask task = new WaitForProvisionTask(container, "success", 10000L);
        assertFalse(task.call());
        verify(container);
    }

    @Test
    public void testProvisioningError() throws Exception {
        Container container = createMock(Container.class);
        expect(container.getId()).andReturn("container").anyTimes();
        expect(container.isAlive()).andReturn(true).anyTimes();
        expect(container.isManaged()).andReturn(true).anyTimes();
        expect(container.getProvisionStatus()).andReturn("").times(5);
        expect(container.getProvisionStatus()).andReturn(Container.PROVISION_ERROR).anyTimes();
        expect(container.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(container.getProvisionException()).andReturn(null).anyTimes();

        replay(container);

        WaitForProvisionTask task = new WaitForProvisionTask(container, "success", 10000L);
        assertFalse(task.call());
        verify(container);
    }

    @Test
    public void testProvisioningTimeout() throws Exception {
        Container container = createMock(Container.class);
        expect(container.getId()).andReturn("container").anyTimes();
        expect(container.isAlive()).andReturn(true).anyTimes();
        expect(container.isManaged()).andReturn(true).anyTimes();
        expect(container.getProvisionStatus()).andReturn("").anyTimes();
        expect(container.getSshUrl()).andReturn("container:8181").anyTimes();
        expect(container.getProvisionException()).andReturn(null).anyTimes();

        replay(container);

        WaitForProvisionTask task = new WaitForProvisionTask(container, "success", 4000L);
        assertFalse(task.call());
        verify(container);
    }
}

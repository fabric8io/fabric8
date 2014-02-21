/*
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
package io.fabric8.commands;

import io.fabric8.api.Container;
import io.fabric8.api.FabricException;
import io.fabric8.api.FabricService;
import io.fabric8.api.scr.support.ReflectionHelper;
import io.fabric8.boot.commands.support.FabricCommand;
import io.fabric8.internal.ContainerImpl;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

public class ContainerLifecycleCommandsTest {

    private ContainerStop stop;
    private FabricService fabricService;
    private CommandSession commandSession;
    private ByteArrayOutputStream result;

    @Before
    public void init() throws Exception {
        this.stop = new ContainerStop();
        this.fabricService = createStrictMock(FabricService.class);
        this.commandSession = createMock(CommandSession.class);
        this.result = new ByteArrayOutputStream();
        expect(this.commandSession.getConsole()).andReturn(new PrintStream(result)).anyTimes();

        ReflectionHelper.setField(FabricCommand.class.getDeclaredField("fabricService"), this.stop, this.fabricService);
    }

    @Test
    public void testStopSingleContainer() throws Exception {
        containers("c1");

        ContainerImpl c1 = newContainer("c1");
        expect(this.fabricService.getContainer("c1")).andReturn(c1);
        expect(this.fabricService.getContainers()).andReturn(new Container[] { c1 });
        this.fabricService.stopContainer(c1, false);

        replay(this.fabricService, this.commandSession);
        this.stop.execute(this.commandSession);
        verify(this.fabricService);
    }

    @Test
    public void testStopNonExistingContainer() throws Exception {
        containers("cNon1");

        expect(this.fabricService.getContainer("cNon1")).andThrow(
            new FabricException("Container 'cNon1' does not exist"));

        replay(this.fabricService, this.commandSession);
        try {
            this.stop.execute(this.commandSession);
            fail("Should throw FabricException");
        } catch (FabricException e) {
        }
        verify(this.fabricService);
    }

    @Test
    public void testStopNoContainers() throws Exception {
        containers();

        replay(this.fabricService, this.commandSession);
        this.stop.execute(this.commandSession);
        verify(this.fabricService);
        assertThat(new String(this.result.toByteArray()).trim(), is("Please specify container name(s)."));
    }

    @Test
    public void testStopNoGlobMatchedContainers() throws Exception {
        containers("d*");

        ContainerImpl c1 = newContainer("c1");
        ContainerImpl c2 = newContainer("c2");
        ContainerImpl c3 = newContainer("c3");
        expect(this.fabricService.getContainers()).andReturn(new Container[] { c1, c3, c2 });
        replay(this.fabricService, this.commandSession);
        this.stop.execute(this.commandSession);
        verify(this.fabricService);
        assertThat(new String(this.result.toByteArray()).trim(),
            is("Please specify container name(s). Your pattern didn't match any container name."));
    }

    @Test
    public void testStopAllMatchingContainers() throws Exception {
        // should preserve order
        containers("c1", "c3", "c2");

        ContainerImpl c1 = newContainer("c1");
        ContainerImpl c2 = newContainer("c2");
        ContainerImpl c3 = newContainer("c3");
        expect(this.fabricService.getContainer("c1")).andReturn(c1);
        expect(this.fabricService.getContainers()).andReturn(new Container[] { c1, c3, c2 });
        this.fabricService.stopContainer(c1, false);
        expect(this.fabricService.getContainer("c3")).andReturn(c3);
        expect(this.fabricService.getContainers()).andReturn(new Container[] { c1, c3, c2 });
        this.fabricService.stopContainer(c3, false);
        expect(this.fabricService.getContainer("c2")).andReturn(c2);
        expect(this.fabricService.getContainers()).andReturn(new Container[] { c1, c3, c2 });
        this.fabricService.stopContainer(c2, false);

        replay(this.fabricService, this.commandSession);
        this.stop.execute(this.commandSession);
        verify(this.fabricService);
    }

    @Test
    public void testStopGlobMatchingContainers() throws Exception {
        // should stop c2 once
        // should not touch d1
        containers("c*", "c2");

        ContainerImpl c1 = newContainer("c1");
        ContainerImpl c2 = newContainer("c2");
        ContainerImpl c3 = newContainer("c3");
        ContainerImpl d1 = newContainer("d1");
        expect(this.fabricService.getContainers()).andReturn(new Container[] { c1, c3, c2, d1 });
        expect(this.fabricService.getContainer("c1")).andReturn(c1);
        expect(this.fabricService.getContainers()).andReturn(new Container[] { c1, c3, c2, d1 });
        this.fabricService.stopContainer(c1, false);
        expect(this.fabricService.getContainer("c3")).andReturn(c3);
        expect(this.fabricService.getContainers()).andReturn(new Container[] { c1, c3, c2, d1 });
        this.fabricService.stopContainer(c3, false);
        expect(this.fabricService.getContainer("c2")).andReturn(c2);
        expect(this.fabricService.getContainers()).andReturn(new Container[] { c1, c3, c2, d1 });
        this.fabricService.stopContainer(c2, false);

        replay(this.fabricService, this.commandSession);
        this.stop.execute(this.commandSession);
        verify(this.fabricService);

        String result = new String(this.result.toByteArray());
        assertThat(result.contains("Container 'c1' stopped successfully."), is(true));
        assertThat(result.contains("Container 'c2' stopped successfully."), is(true));
        assertThat(result.contains("Container 'c3' stopped successfully."), is(true));
    }

    @Test
    public void testStopNoMatchingContainers() throws Exception {
        containers("c1", "c2");

        expect(this.fabricService.getContainer("c1")).andThrow(new FabricException("Container 'c1' does not exist"));

        replay(this.fabricService, this.commandSession);
        try {
            this.stop.execute(this.commandSession);
            fail("Should throw FabricException");
        } catch (FabricException e) {
        }
        verify(this.fabricService);
    }

    @Test
    public void testStopSomeMatchingContainers() throws Exception {
        containers("c1", "c2");

        ContainerImpl c1 = newContainer("c1");
        expect(this.fabricService.getContainer("c1")).andReturn(c1);
        expect(this.fabricService.getContainers()).andReturn(new Container[] { c1 }).once();
        this.fabricService.stopContainer(c1, false);
        expect(this.fabricService.getContainer("c2")).andThrow(new FabricException("Container 'c2' does not exist"));

        replay(this.fabricService, this.commandSession);
        try {
            this.stop.execute(this.commandSession);
            fail("Should throw FabricException");
        } catch (FabricException e) {
            assertThat(e.getMessage().contains("'c2'"), is(true));
        }
        verify(this.fabricService);
    }

    @Test
    public void globMatchingTest() throws Exception {
        assertThat(this.stop.matches("c*", "c"), is(true));
        assertThat(this.stop.matches("c*", "c1"), is(true));
        assertThat(this.stop.matches("c*", "cabc"), is(true));
        assertThat(this.stop.matches("d*", "cabc"), is(false));
        assertThat(this.stop.matches("*c*", "c"), is(true));
        assertThat(this.stop.matches("*c*", "abc"), is(true));
        assertThat(this.stop.matches("*c*", "bca"), is(true));
        assertThat(this.stop.matches("*c*", "bda"), is(false));

        assertThat(this.stop.matches("c?", "c1"), is(true));
        assertThat(this.stop.matches("c?", "c10"), is(false));

        assertThat(this.stop.matches("c.", "c1"), is(false));
        assertThat(this.stop.matches("c.", "c."), is(true));
    }

    /**
     * Configures command with a list of container names
     * 
     * @param names
     */
    private void containers(String... names) {
        try {
            ReflectionHelper.setField(ContainerLifecycleCommand.class.getDeclaredField("containers"), this.stop,
                Arrays.asList(names));
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Helper method to create mockish Container
     * 
     * @param id
     * @return
     */
    private ContainerImpl newContainer(String id) {
        return new ContainerImpl(null, id, this.fabricService) {
            @Override
            public boolean isAlive() {
                return true;
            }
        };
    }

}

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

package org.fusesource.fabric.itests.paxexam;

import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.CreateContainerMetadata;
import org.fusesource.fabric.api.CreateContainerOptions;
import org.fusesource.fabric.api.CreateContainerOptionsBuilder;
import org.fusesource.fabric.api.FabricService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CreateSshContainerTest extends FabricTestSupport {

    private String host;
    private String port;
    private String username;
    private String password;

    /**
     * Returns true if all the requirements for running this test are meet.
     * @return
     */
    public boolean isReady() {
        return
                host != null && port != null && username != null & password != null &&
                        !host.isEmpty() && !port.isEmpty() && !username.isEmpty() && !password.isEmpty();
    }

    @Before
    public void setUp() {
        host = System.getProperty("fabricitest.ssh.host");
        port = System.getProperty("fabricitest.ssh.port");
        username = System.getProperty("fabricitest.ssh.username");
        password = System.getProperty("fabricitest.ssh.password");
    }

    @After
    public void tearDown() {
        if (isReady()) {
            executeCommand("fabric:container-stop ssh2");
            executeCommand("fabric:container-delete ssh2");
            executeCommand("fabric:container-stop ssh1");
            executeCommand("fabric:container-delete ssh1");
        }
    }

    @Test
    public void testSshContainerProvider() throws Throwable {
        if (isReady()) {
            System.err.println(executeCommand("fabric:create -n"));

            CreateContainerOptions options = CreateContainerOptionsBuilder.ssh().name("ssh1")
                    .host(host)
                    .port(Integer.parseInt(port))
                    .username(username)
                    .password(password);
            CreateContainerMetadata[] metadata = getFabricService().createContainers(options);
            assertNotNull(metadata);
            assertEquals(1, metadata.length);
            if (metadata[0].getFailure() != null) {
                throw metadata[0].getFailure();
            }
            assertTrue("Expected successful creation of remote ssh container",metadata[0].isSuccess());
            assertNotNull("Expected successful creation of remote ssh container",metadata[0].getContainer());
            waitForProvisionSuccess(metadata[0].getContainer(), 3 * PROVISION_TIMEOUT);
            System.out.println(executeCommand("fabric:container-list -v"));
            System.out.println(executeCommand("fabric:container-resolver-list"));
            Container ssh1 = getFabricService().getContainer("ssh1");
            assertTrue(ssh1.isAlive());
            createAndAssertChildContainer("ssh2", "ssh1", "default");

            //Stop & Start Remote Child
            Container ssh2 = getFabricService().getContainer("ssh2");
            ssh2.stop();
            assertFalse(ssh2.isAlive());
            ssh2.start();
            waitForProvisionSuccess(ssh2,PROVISION_TIMEOUT);
            assertTrue(ssh2.isAlive());
            ssh2.stop();

            //Try stopping and starting the remote container.
            ssh1.stop();
            assertFalse(ssh1.isAlive());
            System.out.println(executeCommand("fabric:container-list -v"));
            ssh1.start();
            waitForProvisionSuccess(ssh1,PROVISION_TIMEOUT);
            System.out.println(executeCommand("fabric:container-list -v"));
            assertTrue(ssh1.isAlive());
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                copySystemProperty("fabricitest.ssh.username"),
                copySystemProperty("fabricitest.ssh.password"),
                copySystemProperty("fabricitest.ssh.host"),
                copySystemProperty("fabricitest.ssh.port"),
        };
    }
}

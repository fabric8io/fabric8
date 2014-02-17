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

package io.fabric8.itests.basic;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import io.fabric8.api.Container;
import io.fabric8.api.CreateContainerMetadata;
import io.fabric8.api.CreateContainerOptions;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import io.fabric8.service.ssh.CreateSshContainerOptions;

import java.util.Arrays;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class CreateSshContainerTest extends FabricTestSupport {

    private String host;
    private String port;
    private String username;
    private String password;

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

        Assume.assumeTrue(isReady());

        System.err.println(executeCommand("fabric:create -n"));

        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();

            CreateContainerOptions options = CreateSshContainerOptions.builder().name("ssh1").host(host).port(Integer.parseInt(port)).username(username).password(password).build();
            CreateContainerMetadata[] metadata = fabricService.createContainers(options);
            assertNotNull(metadata);
            assertEquals(1, metadata.length);
            if (metadata[0].getFailure() != null) {
                throw metadata[0].getFailure();
            }
            assertTrue("Expected successful creation of remote ssh container", metadata[0].isSuccess());
            assertNotNull("Expected successful creation of remote ssh container", metadata[0].getContainer());
            Provision.containersStatus(Arrays.asList(metadata[0].getContainer()), "success", PROVISION_TIMEOUT);
            System.out.println(executeCommand("fabric:container-list -v"));
            System.out.println(executeCommand("fabric:container-resolver-list"));
            Container ssh1 = fabricService.getContainer("ssh1");
            assertTrue(ssh1.isAlive());
            createAndAssertChildContainer(fabricService, "ssh2", "ssh1", "default");

            //Stop & Start Remote Child
            Container ssh2 = fabricService.getContainer("ssh2");
            ssh2.stop();
            assertFalse(ssh2.isAlive());
            ssh2.start();
            Provision.containersStatus(Arrays.asList(ssh2), "success", PROVISION_TIMEOUT);
            assertTrue(ssh2.isAlive());
            ssh2.stop();

            //Try stopping and starting the remote container.
            ssh1.stop();
            assertFalse(ssh1.isAlive());
            System.out.println(executeCommand("fabric:container-list -v"));
            ssh1.start();
            Provision.containersStatus(Arrays.asList(ssh1), "success", PROVISION_TIMEOUT);
            System.out.println(executeCommand("fabric:container-list -v"));
            assertTrue(ssh1.isAlive());
        } finally {
            fabricProxy.close();
        }
    }

    private boolean isReady() {
        return host != null && port != null && username != null & password != null && !host.isEmpty() && !port.isEmpty() && !username.isEmpty() && !password.isEmpty();
    }

    @Configuration
    public Option[] config() {
        return new Option[] { new DefaultCompositeOption(fabricDistributionConfiguration()), copySystemProperty("fabricitest.ssh.username"),
                copySystemProperty("fabricitest.ssh.password"), copySystemProperty("fabricitest.ssh.host"), copySystemProperty("fabricitest.ssh.port"), };
    }
}

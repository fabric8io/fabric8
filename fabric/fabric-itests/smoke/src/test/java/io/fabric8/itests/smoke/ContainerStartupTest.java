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
package io.fabric8.itests.smoke;

import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Arrays;
import java.util.Dictionary;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ContainerStartupTest extends FabricTestSupport {


    @Test
    public void testLocalFabricCluster() throws Exception {
        System.out.println(executeCommand("fabric:create --clean root"));
        //Wait for zookeeper service to become available.
        Container[] containers = getFabricService().getContainers();

        assertNotNull(containers);
        Provision.containersStatus(Arrays.asList(containers), "success", PROVISION_TIMEOUT);
        assertEquals("Expected to find 1 container", 1, containers.length);
        assertEquals("Expected to find the root container","root", containers[0].getId());

        //Test that a provided by commmand line password exists
        //We don't inject the configuration admin as it causes issues when the tracker gets closed.
        ConfigurationAdmin configurationAdmin = getOsgiService(ConfigurationAdmin.class);
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration(Constants.ZOOKEEPER_CLIENT_PID);
        Dictionary<String, Object> dictionary = configuration.getProperties();
        assertEquals("Expected provided zookeeper password", "systempassword", dictionary.get("zookeeper.password"));

    }

    @Test
    public void testLocalFabricClusterWithPassword() throws Exception {
        System.out.println(executeCommand("fabric:create --clean --zookeeper-password testpassword root"));

        //Wait for zookeeper service to become available.
        Container[] containers = getFabricService().getContainers();

        assertNotNull(containers);
        Provision.containersStatus(Arrays.asList(containers), "success", PROVISION_TIMEOUT);
        assertEquals("Expected to find 1 container",1, containers.length);
        assertEquals("Expected to find the root container","root", containers[0].getId());

        //Test that a provided by command line password exists
        //We don't inject the configuration admin as it causes issues when the tracker gets closed.
        ConfigurationAdmin configurationAdmin = getOsgiService(ConfigurationAdmin.class);
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration(Constants.ZOOKEEPER_CLIENT_PID);
        Dictionary<String, Object> dictionary = configuration.getProperties();
        assertEquals("Expected provided zookeeper password", "testpassword", dictionary.get("zookeeper.password"));
    }

    //Test that when a container is accidentally assigned a missing profile, it is properlly displayed and can be removed.
    @Test
    public void testLocalFabricWithMissignProfile() throws Exception {
        System.out.println(executeCommand("fabric:create -p missing"));

        String response = executeCommand("fabric:container-list");
        assertTrue(response.contains("The following profiles are assigned but not found: missing."));
        executeCommand("fabric:container-remove-profile root missing");
        response = executeCommand("fabric:container-list");
        assertFalse(response.contains("The following profiles are assigned but not found:"));

    }


    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(managedFabricDistributionConfiguration()),
                new VMOption("-D"+ CreateEnsembleOptions.ZOOKEEPER_PASSWORD +"=systempassword"),
                //KarafDistributionOption.debugConfiguration("5005", true)
        };
    }
}

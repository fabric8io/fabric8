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
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.ServiceProxy;
import io.fabric8.internal.ContainerImpl;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;

import java.util.Arrays;
import java.util.Dictionary;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Assert;
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


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class AutoClusterStartupTest extends FabricTestSupport {


    @Test
    public void testLocalFabricCluster() throws Exception {
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        ServiceProxy<CuratorFramework> curatorProxy = ServiceProxy.createServiceProxy(bundleContext, CuratorFramework.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CuratorFramework curator = curatorProxy.getService();
            curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
            Provision.containerAlive(Arrays.<Container>asList(new ContainerImpl(null, "root", fabricService)), PROVISION_TIMEOUT);
            Container[] containers = fabricService.getContainers();
            Assert.assertNotNull(containers);
            Assert.assertEquals("Expected to find 1 container", 1, containers.length);
            Assert.assertEquals("Expected to find the root container", "root", containers[0].getId());
        } finally {
            fabricProxy.close();
            curatorProxy.close();
        }
        //Test that a generated password exists
        //We don't inject the configuration admin as it causes issues when the tracker gets closed.
        ConfigurationAdmin configurationAdmin = ServiceLocator.awaitService(ConfigurationAdmin.class);
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration(Constants.ZOOKEEPER_CLIENT_PID);
        Dictionary<String, Object> dictionary = configuration.getProperties();
        Assert.assertNotNull("Expected a generated zookeeper password", dictionary.get("zookeeper.password"));
        Assert.assertTrue(String.valueOf(dictionary.get("zookeeper.url")).endsWith("2182"));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                new VMOption("-D" + CreateEnsembleOptions.ENSEMBLE_AUTOSTART + "=true"),
                new VMOption("-D" + CreateEnsembleOptions.AGENT_AUTOSTART + "=false"),
                new VMOption("-D" + CreateEnsembleOptions.ZOOKEEPER_SERVER_PORT + "=2182"),
                new VMOption("-D" + CreateEnsembleOptions.ZOOKEEPER_SERVER_CONNECTION_PORT + "=2182"),
                //KarafDistributionOption.debugConfiguration("5005", true)
        };
    }
}

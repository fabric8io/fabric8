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
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.utils.SystemProperties;
import org.fusesource.fabric.zookeeper.IZKClient;
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

import javax.inject.Inject;
import java.util.Dictionary;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class AutoClusterStartupTest extends FabricTestSupport {

    @Test
    public void testLocalFabricCluster() throws Exception {
        FabricService fabricService = getFabricService();
        //Test autostartup.
        assertNotNull(fabricService);
        Thread.sleep(DEFAULT_WAIT);
        Container[] containers = fabricService.getContainers();
        assertNotNull(containers);
        assertEquals("Expected to find 1 container", 1, containers.length);
        assertEquals("Expected to find the root container", "root", containers[0].getId());

        //Test that a generated password exists
        //We don't inject the configuration admin as it causes issues when the tracker gets closed.
        ConfigurationAdmin configurationAdmin = getOsgiService(ConfigurationAdmin.class);
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration("org.fusesource.fabric.zookeeper");
        Dictionary<String, Object> dictionary = configuration.getProperties();
        assertNotNull("Expected a generated zookeeper password", dictionary.get("zookeeper.password"));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                new VMOption("-D" + SystemProperties.ENSEMBLE_AUTOSTART + "=true"),
                new VMOption("-D" + SystemProperties.AGENT_AUTOSTART + "=false"),
        };
    }
}

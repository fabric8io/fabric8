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

import java.io.IOException;
import java.util.Dictionary;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.service.cm.ConfigurationAdmin;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class JaasRealmTest extends FabricTestSupport {

    //TODO: This test needs rewrite as significant changes has been done.
    @Test
    @Ignore
    public void testJaasRealm() throws Exception {
         //Wait for configAdmin service to become available.
        ConfigurationAdmin configAdmin = getOsgiService(ConfigurationAdmin.class);
        assertNotNull(configAdmin);
        FeaturesService featuresService = getOsgiService(FeaturesService.class);
        Feature feature = featuresService.getFeature("fabric-jaas");
        assertTrue(featuresService.isInstalled(feature));
        String sshRealm = readProperty(configAdmin,"org.apache.karaf.shell","sshRealm");
        String jmxRealm = readProperty(configAdmin,"org.apache.karaf.management","jmxRealm");
        assertEquals("karaf",sshRealm);
        assertEquals("karaf",jmxRealm);

        Thread.sleep(DEFAULT_WAIT);

        System.err.println(executeCommand("osgi:list"));
        System.err.println(executeCommand("fabric:create -n root"));

        sshRealm = readProperty(configAdmin,"org.apache.karaf.shell","sshRealm");
        jmxRealm = readProperty(configAdmin,"org.apache.karaf.management","jmxRealm");
        assertEquals("zookeeper",sshRealm);
        assertEquals("zookeeper",jmxRealm);

        featuresService.uninstallFeature("fabric-jaas");
        Thread.sleep(DEFAULT_WAIT);

        sshRealm = readProperty(configAdmin,"org.apache.karaf.shell","sshRealm");
        jmxRealm = readProperty(configAdmin,"org.apache.karaf.management","jmxRealm");
        assertEquals("karaf",sshRealm);
        assertEquals("karaf",jmxRealm);
    }

    /**
     * Reads a property from {@code ConfigurationAdmin}.
     * @param configAdmin
     * @param pid          The confiugration PID.
     * @param propertyName The name of the property to read.
     * @return             The property value or null if pid or propertyName does not exist.
     */
   public String readProperty(ConfigurationAdmin configAdmin, String pid, String propertyName) throws IOException {
        String value = null;
            org.osgi.service.cm.Configuration config = configAdmin.getConfiguration(pid);
            if (config != null) {
                Dictionary dict = config.getProperties();
                if (dict != null) {
                    value = (String) dict.get(propertyName);
                }
            }
        return value;
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}

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
package io.fabric8.test.fabric.runtime.embedded.support;

import io.fabric8.api.BootstrapComplete;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceListener;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test fabric-core servies
 */
public abstract class AbstractEmbeddedTest {

    private static String[] moduleNames = new String[] { 
    	"fabric-boot-commands",
        "fabric-core", 
        "fabric-git", 
        "fabric-zookeeper", 
        "fabric-process-container", 
        "fabric-runtime-embedded"
    };

    @BeforeClass
    public static void beforeClass() throws Exception {
        ModuleContext syscontext = EmbeddedUtils.getEmbeddedRuntime().getModuleContext();

        // Start listening on the {@link BootstrapComplete} service
        final CountDownLatch latch = new CountDownLatch(1);
        ServiceListener listener = new ServiceListener() {
            @Override
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED)
                    latch.countDown();
            }
        };
        syscontext.addServiceListener(listener, "(objectClass=" + BootstrapComplete.class.getName() + ")");

        // Install and start the bootstrap modules
        for (String name : moduleNames) {
            ClassLoader classLoader = AbstractEmbeddedTest.class.getClassLoader();
            try {
                EmbeddedUtils.installAndStartModule(classLoader, name);
            } catch (Exception e) {
                System.out.println("Failed to load module " + name + " on classloader " + classLoader + ". " + e);
                throw e;
            }
        }

        Assert.assertTrue("BootstrapComplete registered", latch.await(20, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        RuntimeLocator.releaseRuntime();
    }
}

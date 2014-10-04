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
package io.fabric8.itests.smoke.embedded.support;

import io.fabric8.api.BootstrapComplete;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.core.spi.context.ObjectStore;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;
import org.jboss.gravia.arquillian.container.embedded.EmbeddedRuntimeSetup;
import org.jboss.gravia.arquillian.container.embedded.EmbeddedSetupObserver;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceListener;
import org.jboss.gravia.utils.IllegalStateAssertion;

/**
 * A plugin point that is called from the {@link EmbeddedSetupObserver}
 * as part of the {@link BeforeSuite} processing
 *
 * @author thomas.diesler@jboss.com
 * @since 19-Jun-2014
 */
public class EmbeddedContainerSetup implements EmbeddedRuntimeSetup {

    private static String[] moduleNames = new String[] { 
        "fabric-boot-commands", 
        "fabric-core", 
        "fabric-git", 
        "fabric-zookeeper",
        "fabric-process-container", 
        "fabric-runtime-embedded"
    };

    @Override
    public void setupEmbeddedRuntime(ObjectStore suiteStore) throws Exception {
        
        Path basedir = Paths.get("").toAbsolutePath();
        Path modulesPath = basedir.resolve(Paths.get("target", "modules"));

        // Start listening on the {@link BootstrapComplete} service
        final Runtime runtime = RuntimeLocator.getRequiredRuntime();
        final ModuleContext syscontext = runtime.getModuleContext();
        final CountDownLatch latch = new CountDownLatch(1);
        ServiceListener listener = new ServiceListener() {
            @Override
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    syscontext.removeServiceListener(this);
                    latch.countDown();
                }
            }
        };
        syscontext.addServiceListener(listener, "(objectClass=" + BootstrapComplete.class.getName() + ")");
        
        // Install and start additional modules
        for (URL url : getInitialModuleLocations(modulesPath)) {
            ClassLoader classLoader = EmbeddedUtils.class.getClassLoader();
            EmbeddedUtils.installAndStartModule(classLoader, url);
        }
        
        IllegalStateAssertion.assertTrue(latch.await(20, TimeUnit.SECONDS), "BootstrapComplete registered");
    }

    private List<URL> getInitialModuleLocations(Path modulesPath) throws IOException {
        List<URL> urls = new ArrayList<>();
        for (String modname : moduleNames) {
            File modfile = modulesPath.resolve(modname + ".jar").toFile();
            urls.add(modfile.toURI().toURL());
        }
        return urls;
    }
}

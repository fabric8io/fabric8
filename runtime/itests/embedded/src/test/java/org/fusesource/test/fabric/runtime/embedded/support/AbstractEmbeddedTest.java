/*
 * #%L
 * Gravia :: Integration Tests :: OSGi
 * %%
 * Copyright (C) 2010 - 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.fusesource.test.fabric.runtime.embedded.support;

import io.fabric8.api.BootstrapComplete;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceListener;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

/**
 * Test fabric-core servies
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Oct-2013
 */
public abstract class AbstractEmbeddedTest {

    private static String[] moduleNames = new String[] { "fabric-boot-commands", "fabric-core", "fabric-git", "fabric-zookeeper" };

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
            EmbeddedUtils.installAndStartModule(classLoader, name);
        }

        Assert.assertTrue("BootstrapComplete registered", latch.await(20, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        RuntimeLocator.releaseRuntime();
    }
}

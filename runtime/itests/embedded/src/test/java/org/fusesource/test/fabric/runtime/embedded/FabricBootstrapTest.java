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
package org.fusesource.test.fabric.runtime.embedded;

import io.fabric8.api.Constants;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.PortService;
import io.fabric8.git.GitService;
import io.fabric8.zookeeper.bootstrap.BootstrapConfiguration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.ServiceEvent;
import org.jboss.gravia.runtime.ServiceListener;
import org.jboss.gravia.runtime.ServiceReference;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test fabric-core servies
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Oct-2013
 */
@RunWith(Arquillian.class)
public class FabricBootstrapTest {

    static String[] moduleNames = new String[] { "fabric-core", "fabric-git", "fabric-zookeeper" };
    static ModuleContext syscontext;

    @BeforeClass
    public static void beforeClass() throws Exception {
        syscontext = EmbeddedUtils.getSystemContext();

        // Start listening on the {@link FabricService}
        final CountDownLatch latch = new CountDownLatch(1);
        ServiceListener listener = new ServiceListener() {
            @Override
            public void serviceChanged(ServiceEvent event) {
                if (event.getType() == ServiceEvent.REGISTERED)
                    latch.countDown();
            }
        };
        syscontext.addServiceListener(listener, "(objectClass=" + ContainerProvider.class.getName() + ")");

        // Install and start the bootstrap modules
        for (String name : moduleNames) {
            ClassLoader classLoader = FabricBootstrapTest.class.getClassLoader();
            EmbeddedUtils.installAndStartModule(classLoader, name);
        }

        Assert.assertTrue("ContainerProvider registered", latch.await(20, TimeUnit.SECONDS));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        RuntimeLocator.releaseRuntime();
    }

    @Test
    public void testBootstrapConfiguration() {
        ServiceReference<BootstrapConfiguration> sref = syscontext.getServiceReference(BootstrapConfiguration.class);
        Assert.assertNotNull("BootstrapConfiguration ref not null", sref);
        BootstrapConfiguration service = syscontext.getService(sref);
        Assert.assertNotNull("BootstrapConfiguration not null", service);
        CreateEnsembleOptions options = service.getBootstrapOptions();
        Assert.assertTrue("Ensemble start", options.isEnsembleStart());
    }

    @Test
    public void testConfigurations() throws Exception {
        ConfigurationAdmin configAdmin = EmbeddedUtils.getSystemService(ConfigurationAdmin.class);
        Configuration config = configAdmin.listConfigurations("(service.pid=" + Constants.ZOOKEEPER_CLIENT_PID + ")")[0];
        Assert.assertNotNull("Configuration not null", config);
        Assert.assertNotNull("zookeeper.password not null", config.getProperties().get("zookeeper.password"));
        Assert.assertNotNull("zookeeper.url not null", config.getProperties().get("zookeeper.url"));
        config = configAdmin.listConfigurations("(service.factoryPid=" + Constants.ZOOKEEPER_SERVER_PID + ")")[0];
        Assert.assertNotNull("Configuration not null", config);
        Assert.assertNotNull("dataDir not null", config.getProperties().get("dataDir"));
        config = configAdmin.listConfigurations("(service.pid=" + Constants.DATASTORE_TYPE_PID + ")")[0];
        Assert.assertNotNull("Configuration not null", config);
        Assert.assertNotNull("gitpullperiod not null", config.getProperties().get("gitpullperiod"));
        Assert.assertNotNull("type not null", config.getProperties().get("type"));
    }

    @Test
    public void testCuratorFramework() {
        ServiceReference<CuratorFramework> sref = syscontext.getServiceReference(CuratorFramework.class);
        Assert.assertNotNull("CuratorFramework ref not null", sref);
        CuratorFramework service = syscontext.getService(sref);
        Assert.assertNotNull("CuratorFramework not null", service);
    }

    @Test
    public void testGitService() {
        ServiceReference<GitService> sref = syscontext.getServiceReference(GitService.class);
        Assert.assertNotNull("GitService ref not null", sref);
        GitService service = syscontext.getService(sref);
        Assert.assertNotNull("GitService not null", service);
    }

    @Test
    public void testDataStore() throws Exception {
        ServiceReference<DataStore> sref = syscontext.getServiceReference(DataStore.class);
        Assert.assertNotNull("DataStore ref not null", sref);
        DataStore service = syscontext.getService(sref);
        Assert.assertNotNull("DataStore not null", service);
    }

    @Test
    public void testPortService() {
        ServiceReference<PortService> sref = syscontext.getServiceReference(PortService.class);
        Assert.assertNotNull("PortService ref not null", sref);
        PortService service = syscontext.getService(sref);
        Assert.assertNotNull("PortService not null", service);
    }

    @Test
    public void testFabricService() {
        ServiceReference<FabricService> sref = syscontext.getServiceReference(FabricService.class);
        Assert.assertNotNull("FabricService ref not null", sref);
        FabricService service = syscontext.getService(sref);
        Assert.assertNotNull("FabricService not null", service);
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testContainerProvider() {
        ServiceReference<ContainerProvider> sref = syscontext.getServiceReference(ContainerProvider.class);
        Assert.assertNotNull("ContainerProvider ref not null", sref);
        ContainerProvider service = syscontext.getService(sref);
        Assert.assertNotNull("ContainerProvider not null", service);
    }
}

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
package io.fabric8.test.fabric.runtime.embedded;

import io.fabric8.api.Constants;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.CreateEnsembleOptions.Builder;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.PortService;
import io.fabric8.api.ZooKeeperClusterBootstrap;
import io.fabric8.git.GitService;
import io.fabric8.runtime.itests.support.ServiceLocator;
import io.fabric8.utils.PasswordEncoder;

import java.util.Dictionary;

import org.apache.curator.framework.CuratorFramework;
import io.fabric8.test.fabric.runtime.embedded.support.AbstractEmbeddedTest;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test fabric-core servies
 */
@RunWith(Arquillian.class)
public class FabricBootstrapTest extends AbstractEmbeddedTest {

    private static final String SYSTEM_PASSWORD = "systempassword";

    @Test
    public void testFabricCreate() throws Exception {

        Builder<?> builder = CreateEnsembleOptions.builder().agentEnabled(false).clean(true).zookeeperPassword(SYSTEM_PASSWORD).waitForProvision(false);
        CreateEnsembleOptions options = builder.build();

        ZooKeeperClusterBootstrap bootstrap = ServiceLocator.getRequiredService(ZooKeeperClusterBootstrap.class);
        bootstrap.create(options);

        FabricService fabricService = ServiceLocator.getRequiredService(FabricService.class);
        Container[] containers = fabricService.getContainers();
        Assert.assertNotNull("Containers not null", containers);

        // Verify other required services
        ServiceLocator.getRequiredService(CuratorFramework.class);
        ServiceLocator.getRequiredService(GitService.class);
        ServiceLocator.getRequiredService(DataStore.class);
        ServiceLocator.getRequiredService(PortService.class);
        ServiceLocator.getRequiredService(ContainerProvider.class);

        // Test that a provided by command line password exists
        ConfigurationAdmin configAdmin = ServiceLocator.getRequiredService(ConfigurationAdmin.class);
        org.osgi.service.cm.Configuration configuration = configAdmin.getConfiguration(io.fabric8.api.Constants.ZOOKEEPER_CLIENT_PID);
        Dictionary<String, Object> dictionary = configuration.getProperties();
        Assert.assertEquals("Expected provided zookeeper password", PasswordEncoder.encode(SYSTEM_PASSWORD), dictionary.get("zookeeper.password"));

        assertConfigurations(configAdmin);
    }

    private void assertConfigurations(ConfigurationAdmin configAdmin) throws Exception {
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
}

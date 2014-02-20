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
import io.fabric8.api.Container;
import io.fabric8.api.ContainerProvider;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.CreateEnsembleOptions.Builder;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.PortService;
import io.fabric8.api.ZooKeeperClusterBootstrap;
import io.fabric8.git.GitService;
import io.fabric8.runtime.itests.support.FabricTestSupport;

import java.util.Dictionary;

import org.apache.curator.framework.CuratorFramework;
import org.fusesource.test.fabric.runtime.embedded.support.AbstractEmbeddedTest;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
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
public class FabricBootstrapTest extends AbstractEmbeddedTest {

    @Test
    public void testFabricCreate() throws Exception {
        String zkpassword = System.getProperty(CreateEnsembleOptions.ZOOKEEPER_PASSWORD);
        Assert.assertNotNull(CreateEnsembleOptions.ZOOKEEPER_PASSWORD + " not null", zkpassword);
        Builder<?> builder = CreateEnsembleOptions.builder().agentEnabled(false).clean(true).zookeeperPassword(zkpassword).waitForProvision(false);
        CreateEnsembleOptions options = builder.build();

        ZooKeeperClusterBootstrap bootstrap = FabricTestSupport.getRequiredService(ZooKeeperClusterBootstrap.class);
        bootstrap.create(options);

        FabricService fabricService = FabricTestSupport.getRequiredService(FabricService.class);
        Container[] containers = fabricService.getContainers();
        Assert.assertNotNull("Containers not null", containers);

        // Verify other required services
        FabricTestSupport.getRequiredService(CuratorFramework.class);
        FabricTestSupport.getRequiredService(GitService.class);
        FabricTestSupport.getRequiredService(DataStore.class);
        FabricTestSupport.getRequiredService(PortService.class);
        FabricTestSupport.getRequiredService(ContainerProvider.class);

        // Test that a provided by command line password exists
        ConfigurationAdmin configAdmin = FabricTestSupport.getRequiredService(ConfigurationAdmin.class);
        org.osgi.service.cm.Configuration configuration = configAdmin.getConfiguration(io.fabric8.api.Constants.ZOOKEEPER_CLIENT_PID);
        Dictionary<String, Object> dictionary = configuration.getProperties();
        Assert.assertEquals("Expected provided zookeeper password", "systempassword", dictionary.get("zookeeper.password"));

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

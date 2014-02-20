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
package io.fabric8.runtime.itests;


import io.fabric8.api.Container;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.CreateEnsembleOptions.Builder;
import io.fabric8.api.FabricService;
import io.fabric8.api.ZooKeeperClusterBootstrap;
import io.fabric8.runtime.itests.support.FabricTestSupport;

import java.io.InputStream;
import java.util.Dictionary;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.gravia.Constants;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.test.gravia.itests.support.AnnotatedContextListener;
import org.jboss.test.gravia.itests.support.ArchiveBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Test basic {@link FabricService} functionality
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Jan-2014
 */
@RunWith(Arquillian.class)
public class ContainerStartupTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final ArchiveBuilder archive = new ArchiveBuilder("container-startup-test");
        archive.addClasses(RuntimeType.TOMCAT, AnnotatedContextListener.class);
        archive.addClasses(FabricTestSupport.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                if (ArchiveBuilder.getTargetContainer() == RuntimeType.KARAF) {
                    OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                    builder.addBundleManifestVersion(2);
                    builder.addBundleSymbolicName(archive.getName());
                    builder.addBundleVersion("1.0.0");
                    builder.addManifestHeader(Constants.GRAVIA_ENABLED, Boolean.TRUE.toString());
                    builder.addImportPackages(RuntimeLocator.class, FabricService.class);
                    builder.addImportPackages(ConfigurationAdmin.class);
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability(archive.getName(), "1.0.0");
                    builder.addManifestHeader("Dependencies", "org.jboss.gravia,io.fabric8.api");
                    return builder.openStream();
                }
            }
        });
        return archive.getArchive();
    }

    @Test
    public void testLocalFabricCluster() throws Exception {

        String zkpassword = System.getProperty(CreateEnsembleOptions.ZOOKEEPER_PASSWORD);
        Assert.assertNotNull(CreateEnsembleOptions.ZOOKEEPER_PASSWORD + " not null", zkpassword);
        Builder<?> builder = CreateEnsembleOptions.builder().agentEnabled(false).clean(true).zookeeperPassword(zkpassword).waitForProvision(false);
        CreateEnsembleOptions options = builder.build();

        ModuleContext syscontext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ZooKeeperClusterBootstrap bootstrap = syscontext.getService(syscontext.getServiceReference(ZooKeeperClusterBootstrap.class));
        bootstrap.create(options);

        FabricService fabricService = FabricTestSupport.awaitService(FabricService.class);
        Container[] containers = fabricService.getContainers();
        Assert.assertNotNull("Containers not null", containers);

        //Test that a provided by command line password exists
        ConfigurationAdmin configurationAdmin = FabricTestSupport.getRequiredService(ConfigurationAdmin.class);
        org.osgi.service.cm.Configuration configuration = configurationAdmin.getConfiguration(io.fabric8.api.Constants.ZOOKEEPER_CLIENT_PID);
        Dictionary<String, Object> dictionary = configuration.getProperties();
        Assert.assertEquals("Expected provided zookeeper password", "systempassword", dictionary.get("zookeeper.password"));
    }
}
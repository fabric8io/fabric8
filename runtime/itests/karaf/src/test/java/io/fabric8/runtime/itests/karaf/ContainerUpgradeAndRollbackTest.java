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
package io.fabric8.runtime.itests.karaf;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.runtime.itests.support.CommandSupport;
import io.fabric8.runtime.itests.support.ContainerBuilder;
import io.fabric8.runtime.itests.support.FabricEnsembleSupport;
import io.fabric8.runtime.itests.support.Provision;
import io.fabric8.runtime.itests.support.ServiceProxy;
import io.fabric8.runtime.itests.support.WaitForConfigurationChange;

import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
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
import org.jboss.gravia.itests.support.AnnotatedContextListener;
import org.jboss.gravia.itests.support.ArchiveBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(Arquillian.class)
public class ContainerUpgradeAndRollbackTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final ArchiveBuilder archive = new ArchiveBuilder("container-upgrade-rollback-test");
        archive.addClasses(RuntimeType.TOMCAT, AnnotatedContextListener.class);
        archive.addPackage(CommandSupport.class.getPackage());
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
                    builder.addImportPackages(AbstractCommand.class, Action.class);
                    builder.addImportPackage("org.apache.felix.service.command;status=provisional");
                    builder.addImportPackages(ConfigurationAdmin.class);
                    return builder.openStream();
                } else {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addIdentityCapability(archive.getName(), "1.0.0");
                    builder.addManifestHeader("Dependencies", "io.fabric8.api,org.apache.karaf,org.jboss.gravia");
                    return builder.openStream();
                }
            }
        });
        return archive.getArchive();
    }

    /**
     * This tests the simple scenario of
     * 1. create a child container
     * 2. create a new version
     * 3. modify the profile of the new version
     * 4. upgrade all containers
     * 5. verify that child is provisioned according to the new version
     * 6. rollback containers.
     * 7. verify that the child is provisioned according to the old version.
     */
    @Test
    public void testContainerUpgradeAndRollback() throws Exception {
        CommandSupport.executeCommand("fabric:create --force --clean -n");
        Set<Container> containers = ContainerBuilder.create().withName("camel").withProfiles("feature-camel").assertProvisioningResult().build();
        try {
            CommandSupport.executeCommand("fabric:version-create --parent 1.0 1.1");

            ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
            ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
            try {
                //Make sure that the profile change has been applied before changing the version
                CountDownLatch latch = WaitForConfigurationChange.on(fabricProxy.getService());
                CommandSupport.executeCommand("fabric:profile-edit --features camel-script --features camel-hazelcast feature-camel 1.1");
                Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
            } finally {
                fabricProxy.close();
            }

            CommandSupport.executeCommand("fabric:profile-display --version 1.1 feature-camel");
            CommandSupport.executeCommand("fabric:container-upgrade --all 1.1");
            Provision.provisioningSuccess(containers, FabricEnsembleSupport.PROVISION_TIMEOUT);
            CommandSupport.executeCommand("fabric:container-list");
            for (Container container : containers) {
                Assert.assertEquals("Container should have version 1.1", "1.1", container.getVersion().getId());
                String bundles = CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
                Assert.assertNotNull(bundles);
                System.out.println(bundles);
                Assert.assertFalse("Expected camel-hazelcast installed on container: " + container.getId(), bundles.isEmpty());
            }

            CommandSupport.executeCommand("fabric:container-rollback --all 1.0");
            Provision.provisioningSuccess(containers, FabricEnsembleSupport.PROVISION_TIMEOUT);
            CommandSupport.executeCommand("fabric:container-list");

            for (Container container : containers) {
                Assert.assertEquals("Container should have version 1.0", "1.0", container.getVersion().getId());
                String bundles = CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
                Assert.assertNotNull(bundles);
                System.out.println(bundles);
                Assert.assertTrue("Expected no camel-hazelcast installed on container: " + container.getId(), bundles.isEmpty());
            }
        } finally {
            ContainerBuilder.destroy(containers);
        }
    }
}

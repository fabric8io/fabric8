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
package io.fabric8.itests.basic.karaf;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.gravia.ServiceLocator;
import io.fabric8.itests.support.CommandSupport;
import io.fabric8.itests.support.ContainerBuilder;
import io.fabric8.itests.support.ProvisionSupport;
import io.fabric8.itests.support.ServiceProxy;
import io.fabric8.itests.support.WaitForConfigurationChange;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;

import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;

@RunWith(Arquillian.class)
public class ExtendedUpgradeAndRollbackTest  {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "extended-upgrade-rollback-test");
        archive.addPackage(CommandSupport.class.getPackage());
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleVersion("1.0.0");
                builder.addImportPackages(ServiceLocator.class, FabricService.class);
                builder.addImportPackages(AbstractCommand.class, Action.class);
                builder.addImportPackage("org.apache.felix.service.command;status=provisional");
                builder.addImportPackages(ConfigurationAdmin.class, ServiceTracker.class, Logger.class);
                builder.addImportPackages(CuratorFramework.class, ZooKeeperUtils.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    /**
     * The purpose of this test is that everything works ok, even if the container is created after the version.
     * This is a test for the issue: http://fusesource.com/issues/browse/FABRIC-363
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testContainerAfterVersionUpgradeAndDowngrade() throws Exception {
        System.out.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        BundleContext moduleContext = ServiceLocator.getSystemContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            System.out.println(CommandSupport.executeCommand("fabric:version-create --parent 1.0 1.1"));
            Set<Container> containers = ContainerBuilder.create().withName("basic_camel_a").withProfiles("feature-camel").assertProvisioningResult().build(fabricService);
            try {
                //Make sure that the profile change has been applied before changing the version
                CountDownLatch latch = WaitForConfigurationChange.on(fabricService);
                System.out.println(CommandSupport.executeCommand("fabric:profile-edit --features camel-script --features camel-hazelcast feature-camel 1.1"));
                Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));

                System.out.println(CommandSupport.executeCommand("fabric:container-upgrade --all 1.1"));
                ProvisionSupport.provisioningSuccess(containers, ProvisionSupport.PROVISION_TIMEOUT);
                System.out.println(CommandSupport.executeCommand("fabric:container-list"));

                for (Container container : containers) {
                    Assert.assertEquals("Container should have version 1.1", "1.1", container.getVersion().getId());
                    String bundles = CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
                    Assert.assertNotNull(bundles);
                    System.out.println(bundles);
                    Assert.assertFalse("Expected camel-hazelcast installed on container:"+container.getId()+".", bundles.isEmpty());
                }
                System.out.println(CommandSupport.executeCommand("fabric:container-rollback --all 1.0"));
                ProvisionSupport.provisioningSuccess(containers, ProvisionSupport.PROVISION_TIMEOUT);
                System.out.println(CommandSupport.executeCommand("fabric:container-list"));
                for (Container container : containers) {
                    Assert.assertEquals("Container should have version 1.0", "1.0", container.getVersion().getId());
                    String bundles = CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + container.getId() + " osgi:list -s | grep camel-hazelcast");
                    Assert.assertNotNull(bundles);
                    System.out.println(bundles);
                    Assert.assertTrue("Expected no camel-hazelcast installed on container:" + container.getId() + ".", bundles.isEmpty());
                }
            } finally {
                ContainerBuilder.stop(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }


    /**
     * This is a test for http://fusesource.com/issues/browse/FABRIC-367.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testContainerAfterVersionDowngrade() throws Exception {
        System.out.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        BundleContext moduleContext = ServiceLocator.getSystemContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            System.out.println(CommandSupport.executeCommand("fabric:version-create --parent 1.0 1.1"));
            System.out.println(CommandSupport.executeCommand("fabric:container-upgrade --all 1.1"));
            Set<Container> containers = ContainerBuilder.create().withName("basic_camel_b").withProfiles("feature-camel").assertProvisioningResult().build(fabricService);
            try {
                System.out.println(CommandSupport.executeCommand("fabric:container-rollback --all 1.0"));
                ProvisionSupport.provisioningSuccess(containers, ProvisionSupport.PROVISION_TIMEOUT);
                for (Container container : containers) {
                    Assert.assertEquals("Container should have version 1.0", "1.0", container.getVersion().getId());
                    Assert.assertNotNull(ZooKeeperUtils.exists(ServiceLocator.awaitService(CuratorFramework.class), "/fabric/configs/versions/1.0/containers/" + container.getId()));
                }
            } finally {
                ContainerBuilder.stop(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }
}

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
package io.fabric8.itests.smoke.karaf;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.gravia.ServiceLocator;
import io.fabric8.itests.support.CommandSupport;
import io.fabric8.itests.support.ContainerBuilder;
import io.fabric8.itests.support.ProvisionSupport;
import io.fabric8.itests.support.ServiceProxy;

import java.io.InputStream;
import java.util.Set;

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

/**
 * Test the fabric:create command
 *
 * @since 04-Feb-2014
 */
@RunWith(Arquillian.class)
public class CreateChildContainerTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "create-child-test");
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
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testCreateChildContainer() throws Exception {
        System.err.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        BundleContext moduleContext = ServiceLocator.getSystemContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Set<Container> containers = ContainerBuilder.child(1).withName("smoke_child_a").build(fabricService);
            try {
                Assert.assertEquals("One container", 1, containers.size());
                Container child = containers.iterator().next();
                Assert.assertEquals("smoke_child_a", child.getId());
                Assert.assertEquals("root", child.getParent().getId());
            } finally {
                ContainerBuilder.stop(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Test
    public void testCreateChildContainerWithCustomZKServerPort() throws Exception {
        System.err.println(CommandSupport.executeCommand("fabric:create --force --clean -n --zookeeper-server-port 2345"));
        System.err.println(CommandSupport.executeCommand("fabric:profile-create --parents default p1"));
        System.err.println(CommandSupport.executeCommand("fabric:profile-edit --features fabric-zookeeper-commands p1"));
        BundleContext moduleContext = ServiceLocator.getSystemContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Set<Container> containers = ContainerBuilder.child(1).withName("smoke_child_b").withProfiles("p1").build(fabricService);
            ProvisionSupport.provisioningSuccess(containers, ProvisionSupport.PROVISION_TIMEOUT);
            try {
                Container child = containers.iterator().next();
                String ensembleUrl = CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + child.getId() + " zk:get /fabric/configs/ensemble/url");
                Assert.assertTrue("Child should use custom ZK server port, but was: " + ensembleUrl, ensembleUrl.contains("${zk:root/ip}:2345"));
            } finally {
                ContainerBuilder.stop(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Test
    public void testCreateChildWithMergedConfiguration() throws Exception {
        CommandSupport.executeCommand("fabric:create --force --clean -n");
        CommandSupport.executeCommand("fabric:profile-create --parents karaf test");
        // will wipe out other properties
        CommandSupport.executeCommand("fabric:profile-edit --pid org.apache.karaf.log/size=102 test");
        // will *not* wipe out other properties
        CommandSupport.executeCommand("fabric:profile-edit --pid org.apache.karaf.shell/fabric.config.merge=true test");
        CommandSupport.executeCommand("fabric:profile-edit --pid org.apache.karaf.shell/sshIdleTimeout=1800002 test");

        BundleContext moduleContext = ServiceLocator.getSystemContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Set<Container> containers = ContainerBuilder.child(1).withName("smoke_child_c").withProfiles("test").assertProvisioningResult().build(fabricService);

            try {
                Assert.assertEquals("One container", 1, containers.size());
                Container child = containers.iterator().next();
                Assert.assertEquals("smoke_child_c", child.getId());
                Assert.assertEquals("root", child.getParent().getId());
                String logPid = CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + child.getId()
                        + " config:proplist --pid org.apache.karaf.log");
                String shellPid = CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + child.getId()
                        + " config:proplist --pid org.apache.karaf.shell");

                Assert.assertFalse(logPid.contains("pattern"));
                Assert.assertTrue(logPid.contains("size = 102"));
                Assert.assertTrue(shellPid.contains("sshHost"));
                Assert.assertTrue(shellPid.contains("sshIdleTimeout = 1800002"));
            } finally {
                ContainerBuilder.stop(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

}

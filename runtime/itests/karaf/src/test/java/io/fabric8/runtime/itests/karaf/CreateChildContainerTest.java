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

import java.io.InputStream;
import java.util.Set;

import io.fabric8.runtime.itests.support.FabricEnsembleSupport;
import io.fabric8.runtime.itests.support.Provision;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.gravia.Constants;
import org.jboss.gravia.resource.ManifestBuilder;
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
import org.osgi.util.tracker.ServiceTracker;

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
        final ArchiveBuilder archive = new ArchiveBuilder("create-child-test");
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
                    builder.addImportPackages(ConfigurationAdmin.class, ServiceTracker.class);
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

    @Test
    public void testCreateChildContainer() throws Exception {
        System.err.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        Set<Container> containers = ContainerBuilder.child(1).withName("child").build();
        try {
            Assert.assertEquals("One container", 1, containers.size());
            Container child = containers.iterator().next();
            Assert.assertEquals("child1", child.getId());
            Assert.assertEquals("root", child.getParent().getId());
        } finally {
            ContainerBuilder.destroy(containers);
        }
    }

    @Test
    public void testCreateChildContainerWithCustomZKServerPort() throws Exception {
        System.err.println(CommandSupport.executeCommand("fabric:create --force --clean -n --zookeeper-server-port 2345"));
        System.err.println(CommandSupport.executeCommand("fabric:profile-create --parents default p1"));
        System.err.println(CommandSupport.executeCommand("fabric:profile-edit --features fabric-zookeeper-commands p1"));
        Set<Container> containers = ContainerBuilder.child(1).withName("child").withProfiles("p1").build();
        Provision.provisioningSuccess(containers, FabricEnsembleSupport.PROVISION_TIMEOUT);
        try {
            Container child = containers.iterator().next();
            String ensembleUrl = CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + child.getId() + " zk:get /fabric/configs/ensemble/url");
            Assert.assertTrue("Child should use custom ZK server port", ensembleUrl.contains("${zk:root/ip}:2345"));
        } finally {
            ContainerBuilder.destroy(containers);
        }
    }

    @Test
    public void testCreateChildWithMergedConfiguration() throws Exception {
        CommandSupport.executeCommand("fabric:create --force --clean -n");
        CommandSupport.executeCommand("fabric:profile-create --parents karaf test");
        // will wipe out other properties
        CommandSupport.executeCommand("fabric:profile-edit --pid org.apache.karaf.log/size=102 test");
        // will *not* wipe out other properties
        CommandSupport.executeCommand("fabric:profile-edit --pid org.apache.karaf.shell/sshIdleTimeout=1800002 test");
        CommandSupport.executeCommand("fabric:profile-edit --pid org.apache.karaf.shell/fabric.config.merge=true test");

        Set<Container> containers = ContainerBuilder.child(1).withName("child")
            .withProfiles("test")
//            .withJvmOpts("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006")
            .assertProvisioningResult().build();

        try {
            Assert.assertEquals("One container", 1, containers.size());
            Container child = containers.iterator().next();
            Assert.assertEquals("child1", child.getId());
            Assert.assertEquals("root", child.getParent().getId());
            String logPid = CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + child.getId() + " config:proplist --pid org.apache.karaf.log");
            String shellPid = CommandSupport.executeCommand("fabric:container-connect -u admin -p admin " + child.getId() + " config:proplist --pid org.apache.karaf.shell");

            Assert.assertFalse(logPid.contains("pattern"));
            Assert.assertTrue(logPid.contains("size = 102"));
            Assert.assertTrue(shellPid.contains("sshHost"));
            Assert.assertTrue(shellPid.contains("sshIdleTimeout = 1800002"));
        } finally {
            ContainerBuilder.destroy(containers);
        }
    }

}

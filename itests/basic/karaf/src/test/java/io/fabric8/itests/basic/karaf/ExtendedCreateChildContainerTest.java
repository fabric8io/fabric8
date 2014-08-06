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
import io.fabric8.itests.support.CommandSupport;
import io.fabric8.itests.support.ContainerBuilder;
import io.fabric8.itests.support.ProvisionSupport;
import io.fabric8.itests.support.ServiceProxy;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.osgi.StartLevelAware;
import org.jboss.gravia.Constants;
import org.jboss.gravia.itests.support.AnnotatedContextListener;
import org.jboss.gravia.itests.support.ArchiveBuilder;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.RuntimeLocator;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;

@RunWith(Arquillian.class)
public class ExtendedCreateChildContainerTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final ArchiveBuilder archive = new ArchiveBuilder("extended-child-container-test");
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
                    builder.addImportPackages(ConfigurationAdmin.class, Logger.class);
                    builder.addImportPackages(CuratorFramework.class, ZooKeeperUtils.class, ZkPath.class);
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
    // [FABRIC-370] Incomplete cleanup of registry entries when deleting containers.
    public void testContainerDelete() throws Exception {
        System.out.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            System.out.println(CommandSupport.executeCommand("fabric:version-create"));

            Set<Container> containers = ContainerBuilder.child(1).withName("basic.cntB").assertProvisioningResult().build(fabricService);
            try {
                CuratorFramework curator = ServiceLocator.awaitService(CuratorFramework.class);
                for (Container cnt : new HashSet<>(containers)) {
                    try {
                        cnt.destroy();
                        containers.remove(cnt);
                        Assert.assertNull(ZooKeeperUtils.exists(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath("1.1", cnt.getId())));
                        Assert.assertNull(ZooKeeperUtils.exists(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath("1.0", cnt.getId())));
                        Assert.assertNull(ZooKeeperUtils.exists(curator, ZkPath.CONTAINER.getPath(cnt.getId())));
                        Assert.assertNull(ZooKeeperUtils.exists(curator, ZkPath.CONTAINER_DOMAINS.getPath(cnt.getId())));
                        Assert.assertNull(ZooKeeperUtils.exists(curator, ZkPath.CONTAINER_PROVISION.getPath(cnt.getId())));
                    } catch (Exception ex) {
                        //ignore
                    }
                }
            } finally {
                ContainerBuilder.destroy(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Test
    // [FABRIC-482] Fabric doesn't allow remote host user/password to be changed once the container is created.
    public void testContainerWithPasswordChange() throws Exception {
        System.out.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        ModuleContext moduleContext = RuntimeLocator.getRequiredRuntime().getModuleContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Set<Container> containers = ContainerBuilder.child(1).withName("basic.cntB").assertProvisioningResult().build(fabricService);
            try {
                Container cntB = containers.iterator().next();
                System.out.println(
                        CommandSupport.executeCommands(
                                "jaas:manage --realm karaf --module io.fabric8.jaas.ZookeeperLoginModule",
                                "jaas:userdel admin",
                                "jaas:useradd admin newpassword",
                                "jaas:roleadd admin admin",
                                "jaas:update"
                        )
                );
                System.out.println(CommandSupport.executeCommand("fabric:container-stop --user admin --password newpassword " + cntB.getId()));
                ProvisionSupport.containersAlive(containers, false, ProvisionSupport.PROVISION_TIMEOUT);
                containers.remove(cntB);
            } finally {
                ContainerBuilder.stop(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }
}

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

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.gravia.ServiceLocator;
import io.fabric8.itests.support.CommandSupport;
import io.fabric8.itests.support.ContainerBuilder;
import io.fabric8.itests.support.ServiceProxy;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;

import java.io.InputStream;
import java.util.Set;

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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;

@RunWith(Arquillian.class)
@Ignore("[FABRIC-1133] ResolverTest can only run one method at a time")
public class ResolverTest {

    @Deployment
    @StartLevelAware(autostart = true)
    public static Archive<?> deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "resolver-test");
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
                builder.addImportPackages(CuratorFramework.class, ZooKeeperUtils.class, ZkPath.class);
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testRootContainerResolver() throws Exception {
        System.out.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        BundleContext moduleContext = ServiceLocator.getSystemContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Container current = fabricService.getCurrentContainer();
            Assert.assertEquals("localhostname", current.getResolver());
            String sshUrlWithLocalhostResolver = current.getSshUrl();

            System.out.println(CommandSupport.executeCommand("fabric:container-resolver-set --container root localip"));
            Assert.assertEquals("localip", current.getResolver());
            String sshUrlWithLocalIpResolver = current.getSshUrl();
            //Check that the SSH URL has been updated.
            System.out.println("SSH URL with " + sshUrlWithLocalhostResolver + " resolver: localhostname");
            System.out.println("SSH URL with " + sshUrlWithLocalIpResolver + " resolver: localip");
            Assert.assertNotSame(sshUrlWithLocalhostResolver, sshUrlWithLocalIpResolver);
        } finally {
            fabricProxy.close();
        }
    }

    @Test
    public void testCreateWithGlobalResolver() throws Exception {
        System.out.println(CommandSupport.executeCommand("fabric:create --force --clean -n -g manualip --manual-ip localhost -b localhost"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        BundleContext moduleContext = ServiceLocator.getSystemContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Container current = fabricService.getCurrentContainer();
            Assert.assertEquals("manualip", current.getResolver());
        } finally {
            fabricProxy.close();
        }
    }

    @Test
    public void testCreateWithGlobalAndLocalResolver() throws Exception {
        System.out.println(CommandSupport.executeCommand("fabric:create --force --clean -n -g manualip -r localhostname --manual-ip localhost"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        BundleContext moduleContext = ServiceLocator.getSystemContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Container current = fabricService.getCurrentContainer();
            Assert.assertEquals("localhostname", current.getResolver());
        } finally {
            fabricProxy.close();
        }
    }

    @Test
    public void testChildContainerResolver() throws Exception {
        System.out.println(CommandSupport.executeCommand("fabric:create --force --clean -n"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        BundleContext moduleContext = ServiceLocator.getSystemContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CuratorFramework curator = fabricService.adapt(CuratorFramework.class);

            Set<Container> containers = ContainerBuilder.create(1, 1).withName("basic.cntF").withProfiles("default").assertProvisioningResult().build(fabricService);
            try {
                Container cntF = containers.iterator().next();

                Assert.assertEquals("localhostname", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(cntF.getId())));
                String sshUrlWithLocalhostResolver = cntF.getSshUrl();

                CommandSupport.executeCommand("fabric:container-resolver-set --container " + cntF.getId() + " localip");
                Assert.assertEquals("localip", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(cntF.getId())));
                String sshUrlWithLocalIpResolver = cntF.getSshUrl();
                //Check that the SSH URL has been updated.
                System.out.println("SSH URL with " + sshUrlWithLocalhostResolver + " resolver: localhostname");
                System.out.println("SSH URL with " + sshUrlWithLocalIpResolver + " resolver: localip");
                Assert.assertNotSame(sshUrlWithLocalhostResolver, sshUrlWithLocalIpResolver);

                setData(curator, ZkPath.CONTAINER_PUBLIC_IP.getPath(cntF.getId()), "my.public.ip.address");
                CommandSupport.executeCommand("fabric:container-resolver-set --container " + cntF.getId() + " publicip");
                Assert.assertEquals("publicip", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(cntF.getId())));
                String sshUrlWithPublicIpResolver = cntF.getSshUrl();
                System.out.println("SSH URL with " + sshUrlWithPublicIpResolver + " resolver: publicip");
                Assert.assertNotNull(sshUrlWithPublicIpResolver);
                Assert.assertTrue(sshUrlWithPublicIpResolver.startsWith("my.public.ip.address"));

                setData(curator, ZkPath.CONTAINER_MANUAL_IP.getPath(cntF.getId()), "my.manual.ip.address");
                CommandSupport.executeCommand("fabric:container-resolver-set --container " + cntF.getId() + " manualip");
                Assert.assertEquals("manualip", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(cntF.getId())));
                String sshUrlWithManualIpResolver = cntF.getSshUrl();

                System.out.println("SSH URL with " + sshUrlWithManualIpResolver + " resolver: manualip");
                Assert.assertNotNull(sshUrlWithManualIpResolver);
                Assert.assertTrue(sshUrlWithManualIpResolver.startsWith("my.manual.ip.address"));
            } finally {
                ContainerBuilder.destroy(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Test
    public void testResolverInheritanceOnChild() throws Exception {
        System.out.println(CommandSupport.executeCommand("fabric:create --force --clean -n -g localip -r manualip --manual-ip localhost -b localhost"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        BundleContext moduleContext = ServiceLocator.getSystemContext();
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(moduleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CuratorFramework curator = fabricService.adapt(CuratorFramework.class);

            Set<Container> containers = ContainerBuilder.create(1, 1).withName("basic.cntG").withProfiles("default").assertProvisioningResult().build(fabricService);
            try {
                Container cntG = containers.iterator().next();

                Assert.assertEquals("manualip", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(cntG.getId())));
                Assert.assertEquals("manualip", fabricService.getCurrentContainer().getResolver());

                //We stop the config admin bridge, since the next step is going to hung the container if we do propagate the change to config admin.
                //new BundleUtils(bundleContext).findAndStopBundle("io.fabric8.fabric-configadmin");
                //We want to make sure that the child points to the parent, so we change the parent resolvers and assert.
                System.out.println(CommandSupport.executeCommand("fabric:container-resolver-set --container root localip"));
                Assert.assertEquals("localip", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(cntG.getId())));
            } finally {
                ContainerBuilder.destroy(fabricService, containers);
            }
        } finally {
            fabricProxy.close();
        }
    }
}

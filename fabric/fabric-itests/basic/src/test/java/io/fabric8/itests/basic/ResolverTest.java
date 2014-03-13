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

package io.fabric8.itests.basic;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;
import io.fabric8.api.Container;
import io.fabric8.api.ContainerRegistration;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.utils.BundleUtils;
import io.fabric8.zookeeper.ZkPath;

import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ResolverTest extends FabricTestSupport {

    @Test
    public void testRootContainerResolver() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            Container current = fabricService.getCurrentContainer();
            ServiceLocator.awaitService(bundleContext, ContainerRegistration.class);
            Assert.assertEquals("localhostname", current.getResolver());
            String sshUrlWithLocalhostResolver = current.getSshUrl();

            waitForFabricCommands();
            System.err.println(executeCommand("fabric:container-resolver-set --container root localip"));
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
        System.err.println(executeCommand("fabric:create -n -g manualip --manual-ip localhost -b localhost --clean"));
        ServiceLocator.awaitService(bundleContext, ContainerRegistration.class);
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
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
        System.err.println(executeCommand("fabric:create -n -g manualip -r localhostname --manual-ip localhost --clean"));
        ServiceLocator.awaitService(bundleContext, ContainerRegistration.class);
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
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
        System.err.println(executeCommand("fabric:create -n"));
        ServiceLocator.awaitService(bundleContext, ContainerRegistration.class);
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CuratorFramework curator = fabricService.adapt(CuratorFramework.class);

            Set<Container> containers = ContainerBuilder.create(fabricProxy, 1, 1).withName("child").withProfiles("default").assertProvisioningResult().build();
            try {
                Container child = containers.iterator().next();

                Assert.assertEquals("localhostname", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
                String sshUrlWithLocalhostResolver = child.getSshUrl();

                executeCommand("fabric:container-resolver-set --container " + child.getId() + " localip");
                Assert.assertEquals("localip", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
                String sshUrlWithLocalIpResolver = child.getSshUrl();
                //Check that the SSH URL has been updated.
                System.out.println("SSH URL with " + sshUrlWithLocalhostResolver + " resolver: localhostname");
                System.out.println("SSH URL with " + sshUrlWithLocalIpResolver + " resolver: localip");
                Assert.assertNotSame(sshUrlWithLocalhostResolver, sshUrlWithLocalIpResolver);

                setData(curator, ZkPath.CONTAINER_PUBLIC_IP.getPath(child.getId()), "my.public.ip.address");
                executeCommand("fabric:container-resolver-set --container " + child.getId() + " publicip");
                Assert.assertEquals("publicip", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
                String sshUrlWithPublicIpResolver = child.getSshUrl();
                System.out.println("SSH URL with " + sshUrlWithPublicIpResolver + " resolver: publicip");
                Assert.assertNotNull(sshUrlWithPublicIpResolver);
                Assert.assertTrue(sshUrlWithPublicIpResolver.startsWith("my.public.ip.address"));

                setData(curator, ZkPath.CONTAINER_MANUAL_IP.getPath(child.getId()), "my.manual.ip.address");
                executeCommand("fabric:container-resolver-set --container " + child.getId() + " manualip");
                Assert.assertEquals("manualip", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
                String sshUrlWithManualIpResolver = child.getSshUrl();

                System.out.println("SSH URL with " + sshUrlWithManualIpResolver + " resolver: manualip");
                Assert.assertNotNull(sshUrlWithManualIpResolver);
                Assert.assertTrue(sshUrlWithManualIpResolver.startsWith("my.manual.ip.address"));
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Test
    public void testResolverInheritanceOnChild() throws Exception {
        System.err.println(executeCommand("fabric:create -n -g localip -r manualip --manual-ip localhost -b localhost"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CuratorFramework curator = fabricService.adapt(CuratorFramework.class);

            Set<Container> containers = ContainerBuilder.create(fabricProxy, 1, 1).withName("child").withProfiles("default").assertProvisioningResult().build();
            try {
                Container child = containers.iterator().next();

                Assert.assertEquals("manualip", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
                Assert.assertEquals("manualip", fabricService.getCurrentContainer().getResolver());
                waitForFabricCommands();

                //We stop the config admin bridge, since the next step is going to hung the container if we do propagate the change to config admin.
                new BundleUtils(bundleContext).findAndStopBundle("io.fabric8.fabric-configadmin");
                //We want to make sure that the child points to the parent, so we change the parent resolvers and assert.
                System.err.println(executeCommand("fabric:container-resolver-set --container root localip"));
                Assert.assertEquals("localip", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                CoreOptions.wrappedBundle(mavenBundle("io.fabric8", "fabric-utils"))
                //debugConfiguration("5005",true)
        };
    }
}

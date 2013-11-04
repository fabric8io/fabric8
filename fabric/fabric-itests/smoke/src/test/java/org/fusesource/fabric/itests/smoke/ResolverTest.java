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

package org.fusesource.fabric.itests.smoke;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.getSubstitutedPath;
import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.setData;

import java.util.Set;

import junit.framework.Assert;

import org.apache.curator.framework.CuratorFramework;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.ContainerRegistration;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.itests.paxexam.support.FabricTestSupport;
import org.fusesource.fabric.utils.BundleUtils;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ResolverTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testRootContainerResolver() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        Container current = getFabricService().getCurrentContainer();
        ServiceLocator.getOsgiService(ContainerRegistration.class);
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
    }

    @Test
    public void testCreateWithGlobalResolver() throws Exception {
        System.err.println(executeCommand("fabric:create -n -g manualip --manual-ip localhost -b localhost --clean"));
        ServiceLocator.getOsgiService(ContainerRegistration.class);
        Container current = getFabricService().getCurrentContainer();
        Assert.assertEquals("manualip", current.getResolver());
    }

    @Test
    public void testCreateWithGlobalAndLocalResolver() throws Exception {
        System.err.println(executeCommand("fabric:create -n -g manualip -r localhostname --manual-ip localhost --clean"));
        ServiceLocator.getOsgiService(ContainerRegistration.class);
        Container current = getFabricService().getCurrentContainer();
        Assert.assertEquals("localhostname", current.getResolver());
    }

    @Test
    @Ignore("[FABRIC-648] Fix fabric smoke ResolverTest")
    public void testChildContainerResolver() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        ServiceLocator.getOsgiService(ContainerRegistration.class);
        CuratorFramework curator = getCurator();

        Set<Container> containers = ContainerBuilder.create(1, 1).withName("child").withProfiles("default").assertProvisioningResult().build();
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
    }

    @Test
    @Ignore("[FABRIC-648] Fix fabric smoke ResolverTest")
    public void testResolverInheritanceOnChild() throws Exception {
        System.err.println(executeCommand("fabric:create -n -g localip -r manualip --manual-ip localhost -b localhost"));
        CuratorFramework curator = getCurator();
        Set<Container> containers = ContainerBuilder.create(1, 1).withName("child").withProfiles("default").assertProvisioningResult().build();
        Container child = containers.iterator().next();

        Assert.assertEquals("manualip", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
        Assert.assertEquals("manualip", getFabricService().getCurrentContainer().getResolver());
        waitForFabricCommands();

        //We stop the config admin bridge, since the next step is going to hung the container if we do propagate the change to config admin.
        new BundleUtils(bundleContext).findAndStopBundle("org.fusesource.fabric.fabric-configadmin");
        //We want to make sure that the child points to the parent, so we change the parent resolvers and assert.
        System.err.println(executeCommand("fabric:container-resolver-set --container root localip"));
        Assert.assertEquals("localip", getSubstitutedPath(curator, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //debugConfiguration("5005",true)
        };
    }
}

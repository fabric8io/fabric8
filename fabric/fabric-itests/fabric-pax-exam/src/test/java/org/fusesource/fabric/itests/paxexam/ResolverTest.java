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

package org.fusesource.fabric.itests.paxexam;

import junit.framework.Assert;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricService;
import org.fusesource.fabric.itests.paxexam.support.ContainerBuilder;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.osgi.service.blueprint.container.BlueprintContainer;

import javax.inject.Inject;

import java.util.Set;

import static org.fusesource.tooling.testing.pax.exam.karaf.ServiceLocator.getOsgiService;

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

        Assert.assertEquals("localhostname", current.getResolver());
        String sshUrlWithLocalhostResolver = current.getSshUrl();

        //We need to wait till fabric commands are available.
        getOsgiService(BlueprintContainer.class, "(osgi.blueprint.container.symbolicname=org.fusesource.fabric.fabric-commands)", DEFAULT_TIMEOUT);
        System.err.println(executeCommand("fabric:container-resolver-set --container root localip"));
        Assert.assertEquals("localip", current.getResolver());
        String sshUrlWithLocalIpResolver = current.getSshUrl();
        //Check that the SSH URL has been updated.
        System.out.println("SSH URL with " + sshUrlWithLocalhostResolver + " resolver: localhostname");
        System.out.println("SSH URL with " + sshUrlWithLocalIpResolver + " resolver:");
        Assert.assertNotSame(sshUrlWithLocalhostResolver, sshUrlWithLocalIpResolver);
    }


    @Test
    public void testResolverPriorities() throws Exception {
        Container current = getFabricService().getCurrentContainer();
        System.err.println(executeCommand("fabric:create -n -g manualip --manual-ip localhost --clean"));
        Assert.assertEquals("manualip", current.getResolver());

        System.getProperties().remove(ZkDefs.LOCAL_RESOLVER_PROPERTY);
        System.getProperties().remove(ZkDefs.GLOBAL_RESOLVER_PROPERTY);

        System.err.println(executeCommand("fabric:create -n -g manualip -r localhostname --manual-ip localhost --clean"));
        Assert.assertEquals("localhostname", current.getResolver());

        System.getProperties().remove(ZkDefs.LOCAL_RESOLVER_PROPERTY);
        System.getProperties().remove(ZkDefs.GLOBAL_RESOLVER_PROPERTY);

        System.err.println(executeCommand("fabric:create -n -g manualip"));
        Assert.assertEquals("localhostname", current.getResolver());
    }

    @Test
    public void testChildContainerResolver() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        FabricService fabricService = getFabricService();
        IZKClient zookeeper = getZookeeper();

        Set<Container> containers = ContainerBuilder.create(1,1).withName("child").withProfiles("default").assertProvisioningResult().build();
        Container child = containers.iterator().next();

        Assert.assertEquals("localhostname", ZooKeeperUtils.getSubstitutedPath(zookeeper, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
        String sshUrlWithLocalhostResolver = child.getSshUrl();

        executeCommand("fabric:container-resolver-set --container "+child.getId()+" localip");
        Assert.assertEquals("localip", ZooKeeperUtils.getSubstitutedPath(zookeeper, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
        String sshUrlWithLocalIpResolver = child.getSshUrl();
        //Check that the SSH URL has been updated.
        System.out.println("SSH URL with " + sshUrlWithLocalhostResolver + " resolver: localhostname");
        System.out.println("SSH URL with " + sshUrlWithLocalIpResolver + " resolver: localip");
        Assert.assertNotSame(sshUrlWithLocalhostResolver, sshUrlWithLocalIpResolver);

        ZooKeeperUtils.set(zookeeper, ZkPath.CONTAINER_PUBLIC_IP.getPath(child.getId()), "my.public.ip.address");
        executeCommand("fabric:container-resolver-set --container "+child.getId()+" publicip");
        Assert.assertEquals("publicip", ZooKeeperUtils.getSubstitutedPath(zookeeper, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
        String sshUrlWithPublicIpResolver = child.getSshUrl();
        System.out.println("SSH URL with " + sshUrlWithPublicIpResolver + " resolver: publicip");
        Assert.assertNotNull(sshUrlWithPublicIpResolver);
        Assert.assertTrue(sshUrlWithPublicIpResolver.startsWith("my.public.ip.address"));

        ZooKeeperUtils.set(zookeeper, ZkPath.CONTAINER_MANUAL_IP.getPath(child.getId()), "my.manual.ip.address");
        executeCommand("fabric:container-resolver-set --container "+child.getId()+" manualip");
        Assert.assertEquals("manualip", ZooKeeperUtils.getSubstitutedPath(zookeeper, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
        String sshUrlWithManualIpResolver = child.getSshUrl();

        System.out.println("SSH URL with " + sshUrlWithManualIpResolver + " resolver: manualip");
        Assert.assertNotNull(sshUrlWithManualIpResolver);
        Assert.assertTrue(sshUrlWithManualIpResolver.startsWith("my.manual.ip.address"));
    }

    @Test
    public void testResolverInheritanceOnChild() throws Exception {
        System.err.println(executeCommand("fabric:create -n -g localip -r manualip --manual-ip localhost"));
        IZKClient zookeeper = getZookeeper();
        Set<Container> containers = ContainerBuilder.create(1, 1).withName("child").withProfiles("default").assertProvisioningResult().build();
        Container child = containers.iterator().next();

        Assert.assertEquals("manualip", ZooKeeperUtils.getSubstitutedPath(zookeeper, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
        Assert.assertEquals("manualip", getFabricService().getCurrentContainer().getResolver());

        //We want to make sure that the child points to the parent, so we change the parent resolvers and assert.
        waitForFabricCommands();
        System.err.println(executeCommand("fabric:container-resolver-set --container root localip"));
        Assert.assertEquals("localip", ZooKeeperUtils.getSubstitutedPath(zookeeper, ZkPath.CONTAINER_RESOLVER.getPath(child.getId())));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //debugConfiguration("5005",true)
        };
    }
}

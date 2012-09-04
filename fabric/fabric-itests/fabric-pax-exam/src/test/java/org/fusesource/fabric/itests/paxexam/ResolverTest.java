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
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class ResolverTest extends FabricTestSupport {

    @Test
    public void testRootContainerResolver() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        Container current = getFabricService().getCurrentContainer();

        Assert.assertEquals("localhostname", current.getResolver());
        String sshUrlWithLocalhostResolver = current.getSshUrl();

        executeCommand("fabric:container-resolver-set localip");
        Assert.assertEquals("localip", current.getResolver());
        String sshUrlWithLocalIpResolver = current.getSshUrl();
        //Check that the SSH URL has been updated.
        System.out.println("SSH URL with " + sshUrlWithLocalhostResolver + " resolver: localhostname");
        System.out.println("SSH URL with " + sshUrlWithLocalIpResolver + " resolver:");
        Assert.assertNotSame(sshUrlWithLocalhostResolver, sshUrlWithLocalIpResolver);
    }


    //Ignore this test case, as it's prone to errors due to knwon issues.
    @Ignore
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
        try {
            createAndAssertChildContainer("child1", "root", "default");
            Container child1 = getFabricService().getContainer("child1");

            Assert.assertEquals("localhostname", child1.getResolver());
            String sshUrlWithLocalhostResolver = child1.getSshUrl();

            executeCommand("fabric:container-resolver-set --container child1 localip");
            Assert.assertEquals("localip", child1.getResolver());
            String sshUrlWithLocalIpResolver = child1.getSshUrl();
            //Check that the SSH URL has been updated.
            System.out.println("SSH URL with "+sshUrlWithLocalhostResolver+" resolver: localhostname");
            System.out.println("SSH URL with "+ sshUrlWithLocalIpResolver+" resolver: localip" );
            Assert.assertNotSame(sshUrlWithLocalhostResolver, sshUrlWithLocalIpResolver);

            IZKClient zookeeper  = getOsgiService(IZKClient.class);
            ZooKeeperUtils.set(zookeeper, ZkPath.CONTAINER_PUBLIC_IP.getPath("child1"), "my.public.ip.address");
            executeCommand("fabric:container-resolver-set --container child1 publicip");
            Assert.assertEquals("publicip", child1.getResolver());
            String sshUrlWithPublicIpResolver = child1.getSshUrl();
            System.out.println("SSH URL with "+ sshUrlWithPublicIpResolver+" resolver: publicip" );
            Assert.assertNotNull(sshUrlWithPublicIpResolver);
            Assert.assertTrue(sshUrlWithPublicIpResolver.startsWith("my.public.ip.address"));

            ZooKeeperUtils.set(zookeeper, ZkPath.CONTAINER_MANUAL_IP.getPath("child1"), "my.manual.ip.address");
            executeCommand("fabric:container-resolver-set --container child1 manualip");
            Assert.assertEquals("manualip", child1.getResolver());
            String sshUrlWithManualIpResolver = child1.getSshUrl();

            System.out.println("SSH URL with "+sshUrlWithManualIpResolver+" resolver: manualip" );
            Assert.assertNotNull(sshUrlWithManualIpResolver);
            Assert.assertTrue(sshUrlWithManualIpResolver.startsWith("my.manual.ip.address"));


        } finally {
            destroyChildContainer("child1");
        }
    }

    @Test
    public void testGlobalResolverInheritanceOnChild() throws Exception {
        System.err.println(executeCommand("fabric:create -n -g localip -r manualip --manual-ip localhost"));
        try {
            createAndAssertChildContainer("child1", "root", "default");
            Container child1 = getFabricService().getContainer("child1");

            Assert.assertEquals("localip", child1.getResolver());
            Assert.assertEquals("manualip", getFabricService().getCurrentContainer().getResolver());

        } finally {
            destroyChildContainer("child1");
        }
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}

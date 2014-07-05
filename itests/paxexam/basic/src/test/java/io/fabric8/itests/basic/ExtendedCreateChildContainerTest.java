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
package io.fabric8.itests.basic;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceLocator;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import io.fabric8.zookeeper.ZkPath;
import io.fabric8.zookeeper.utils.ZooKeeperUtils;

import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ExtendedCreateChildContainerTest extends FabricTestSupport {

    @Test
    // [FABRIC-370] Incomplete cleanup of registry entries when deleting containers.
    public void testContainerDelete() throws Exception {
        System.out.println(executeCommand("fabric:create -n --wait-for-provisioning"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            System.out.println(executeCommand("fabric:version-create"));

            Set<ContainerProxy> containers = ContainerBuilder.child(fabricProxy, 1).withName("child").assertProvisioningResult().build();
            try {
                CuratorFramework curator = ServiceLocator.awaitService(bundleContext, CuratorFramework.class);
                for (Container c : containers) {
                    try {
                        c.destroy();
                        Assert.assertNull(ZooKeeperUtils.exists(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath("1.1", c.getId())));
                        Assert.assertNull(ZooKeeperUtils.exists(curator, ZkPath.CONFIG_VERSIONS_CONTAINER.getPath("1.0", c.getId())));
                        Assert.assertNull(ZooKeeperUtils.exists(curator, ZkPath.CONTAINER.getPath(c.getId())));
                        Assert.assertNull(ZooKeeperUtils.exists(curator, ZkPath.CONTAINER_DOMAINS.getPath(c.getId())));
                        Assert.assertNull(ZooKeeperUtils.exists(curator, ZkPath.CONTAINER_PROVISION.getPath(c.getId())));
                    } catch (Exception ex) {
                        //ignore
                    }
                }
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Test
    // [FABRIC-482] Fabric doesn't allow remote host user/password to be changed once the container is created.
    public void testContainerWithPasswordChange() throws Exception {
        System.out.println(executeCommand("fabric:create -n --wait-for-provisioning"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            Set<ContainerProxy> containers = ContainerBuilder.child(fabricProxy, 1).withName("child").assertProvisioningResult().build();
            try {
                Thread.sleep(5000);
                Container container = containers.iterator().next();
                System.out.println(
                        executeCommands(
                                "jaas:manage --realm karaf --module io.fabric8.jaas.ZookeeperLoginModule",
                                "jaas:userdel admin",
                                "jaas:useradd admin newpassword",
                                "jaas:roleadd admin admin",
                                "jaas:update"
                        )
                );
                System.out.println(executeCommand("fabric:container-stop --user admin --password newpassword "+container.getId()));
                Provision.containersAlive(containers, false, 6 * DEFAULT_TIMEOUT);
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
                //KarafDistributionOption.debugConfiguration("5005", false)
        };
    }
}

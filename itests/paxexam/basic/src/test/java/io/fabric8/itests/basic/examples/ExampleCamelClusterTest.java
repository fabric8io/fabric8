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
package io.fabric8.itests.basic.examples;


import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildren;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerCondition;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.fusesource.jansi.AnsiString;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
@Ignore("[FABRIC-981] Fix basic ExampleCamelClusterTest")
public class ExampleCamelClusterTest extends FabricTestSupport {

    @Test
    public void testRegistryEntries() throws Exception {
        System.out.println(executeCommand("fabric:create -n --wait-for-provisioning root"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CuratorFramework curator = fabricService.adapt(CuratorFramework.class);

            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy, 3).withName("fabric-camel").withProfiles("feature-camel").assertProvisioningResult().build();
            try {
                //We will use the first container as a client and the rest as servers.
                LinkedList<Container> containerList = new LinkedList<Container>(containers);
                Container client = containerList.removeLast();

                LinkedList<Container> servers = new LinkedList<Container>(containerList);

                for (Container c : servers) {
                    Profile p = c.getVersion().getProfile("example-camel-cluster.server");
                    c.setProfiles(new Profile[]{p});
                }

                Provision.provisioningSuccess(servers, PROVISION_TIMEOUT);

                Profile p = client.getVersion().getProfile("example-camel-cluster.client");
                client.setProfiles(new Profile[]{p});

                Provision.provisioningSuccess(Arrays.asList(new Container[]{client}), PROVISION_TIMEOUT);

                System.out.println(executeCommand("fabric:container-list"));
                System.out.println(executeCommand("fabric:profile-display --overlay fabric-camel-server"));

                //Check that the entries have been properly propagated.
                Assert.assertNotNull(exists(curator, "/fabric/registry/camel/endpoints"));
                Assert.assertEquals(1, getChildren(curator, "/fabric/registry/camel/endpoints").size());

                Assert.assertTrue(Provision.waitForCondition(Arrays.asList(client), new ContainerCondition() {
                    @Override
                    public Boolean checkConditionOnContainer(final Container c) {
                        return getCompletedExchangesCount(c) > 0;
                    }
                }, 60000L));

                //We want to kill all but one server, so we take out the first and keep it to the end.
                Container lastActiveServerContainer = servers.removeLast();

                for (Container c : servers) {
                    try {
                        c.destroy(true);
                    } catch (Exception ex) {
                        //ignore.
                    }
                    //Get the number of exchanges completed before we kill the server.
                    final int completed = getCompletedExchangesCount(client);

                    //Ensure that we still have messages flowing
                    Assert.assertTrue(Provision.waitForCondition(Arrays.asList(client), new ContainerCondition() {
                        @Override
                        public Boolean checkConditionOnContainer(final Container c) {
                            return getCompletedExchangesCount(c) > completed + 3;
                        }
                    }, 60000L));
                }
                System.out.println(new AnsiString(executeCommand("fabric:container-connect -u admin -p admin " + client.getId() + " camel:route-info fabric-client")).getPlain().toString());
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    private int getCompletedExchangesCount(final Container c) {
        String response = new AnsiString(executeCommand("fabric:container-connect -u admin -p admin " + c.getId() + " camel:route-info fabric-client | grep \"Exchanges Completed\"")).getPlain().toString();
        System.out.println(response);
        return Integer.parseInt(response.replaceAll("[^0-9]", ""));
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //KarafDistributionOption.debugConfiguration("5005", true),
                KarafDistributionOption.editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.asInProject().getVersion(GROUP_ID, ARTIFACT_ID))
        };
    }
}

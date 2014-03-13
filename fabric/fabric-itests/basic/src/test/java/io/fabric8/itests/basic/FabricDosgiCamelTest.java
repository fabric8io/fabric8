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

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerCondition;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import io.fabric8.zookeeper.ZkPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class FabricDosgiCamelTest extends FabricTestSupport {

    @Test
    public void testFeatureProvisioning() throws Exception {
        System.err.println(executeCommand("fabric:create -n root"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();
            CuratorFramework curator = fabricService.adapt(CuratorFramework.class);

            waitForFabricCommands();

            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy, 2).withName("dosgi").withProfiles("example-dosgi-camel").assertProvisioningResult().build();
            try {
                List<Container> containerList = new ArrayList<Container>(containers);
                List<Container> dosgiProviderContainers = containerList.subList(0, containerList.size() / 2);
                List<Container> dosgiCamelContainers = containerList.subList(containerList.size() / 2, containerList.size());

                for (Container c : dosgiProviderContainers) {
                    setData(curator, ZkPath.CONTAINER_PROVISION_RESULT.getPath(c.getId()), "changing profile");
                    Profile p = c.getVersion().getProfile("example-dosgi-camel.provider");
                    c.setProfiles(new Profile[]{p});
                }

                for (Container c : dosgiCamelContainers) {
                    setData(curator, ZkPath.CONTAINER_PROVISION_RESULT.getPath(c.getId()), "changing profile");
                    Profile p = c.getVersion().getProfile("example-dosgi-camel.consumer");
                    c.setProfiles(new Profile[]{p});
                }

                Provision.provisioningSuccess(dosgiProviderContainers, PROVISION_TIMEOUT);
                Provision.provisioningSuccess(dosgiCamelContainers, PROVISION_TIMEOUT);

                Assert.assertTrue(Provision.waitForCondition(dosgiCamelContainers, new ContainerCondition() {
                    @Override
                    public Boolean checkConditionOnContainer(final Container c) {
                        String response = executeCommand("fabric:container-connect -u admin -p admin " + c.getId() + " log:display | grep \"Message from distributed service to\"");
                        System.err.println(executeCommand("fabric:container-connect -u admin -p admin " + c.getId() + " camel:route-info fabric-client"));
                        Assert.assertNotNull(response);
                        System.err.println(response);
                        String[] lines = response.split("\n");
                        //TODO: This assertion is very relaxed and guarantees nothing.
                        return lines.length >= 1;
                    }
                }, 20000L));
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
                editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.asInProject().getVersion(GROUP_ID, ARTIFACT_ID))
        };
    }
}

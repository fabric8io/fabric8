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
package io.fabric8.itests.basic.camel;

import java.util.Set;

import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricFeaturesTest;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class CamelProfileTest extends FabricFeaturesTest {

    @Test
    public void testFeatures() throws Exception {
        System.out.println(executeCommand("fabric:create -n --wait-for-provisioning"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            FabricService fabricService = fabricProxy.getService();

            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy).withName("feature-camel").withProfiles("feature-camel").assertProvisioningResult().build();
            try {
                CuratorFramework curator = fabricService.adapt(CuratorFramework.class);
                assertProvisionedFeature(fabricService, curator, containers, "camel-http", "feature-camel", "camel-http");
                assertProvisionedFeature(fabricService, curator, containers, "camel-jetty", "feature-camel", "camel-jetty");
                assertProvisionedFeature(fabricService, curator, containers, "camel-jms", "feature-camel", "camel-jms");
                assertProvisionedFeature(fabricService, curator, containers, "camel-ftp", "feature-camel", "camel-ftp");
                assertProvisionedFeature(fabricService, curator, containers, "camel-quartz", "feature-camel", "camel-quartz");
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}

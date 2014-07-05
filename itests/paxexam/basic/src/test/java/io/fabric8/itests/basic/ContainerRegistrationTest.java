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
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;

import java.util.Set;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.BundleContext;

/**
 * A test for making sure that the container registration info such as jmx url and ssh url are updated, if new values
 * are assigned to them via the profile.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class ContainerRegistrationTest extends FabricTestSupport {

    @Inject
    BundleContext bundleContext;

    @Test
    public void testContainerRegistration() throws Exception {
        System.out.println(executeCommand("fabric:create -n --wait-for-provisioning"));
        //System.out.println(executeCommand("shell:info"));
        //System.out.println(executeCommand("fabric:info"));
        //System.out.println(executeCommand("fabric:profile-list"));

        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            waitForFabricCommands();

            System.out.println(executeCommand("fabric:profile-create --parents default child-profile"));
            Assert.assertTrue(Provision.profileAvailable(bundleContext, "child-profile", "1.0", DEFAULT_TIMEOUT));

            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy, 1,1).withName("cnt").withProfiles("child-profile").assertProvisioningResult().build();
            try {
                Container child1 = containers.iterator().next();
                System.out.println(executeCommand("fabric:profile-edit --import-pid --pid org.apache.karaf.shell child-profile"));
                System.out.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.shell/sshPort=8105 child-profile"));

                System.out.println(executeCommand("fabric:profile-edit --import-pid --pid org.apache.karaf.management child-profile"));
                System.out.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.management/rmiServerPort=55555 child-profile"));
                System.out.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.management/rmiRegistryPort=1100 child-profile"));
                System.out.println(executeCommand("fabric:profile-edit --pid org.apache.karaf.management/serviceUrl=service:jmx:rmi://localhost:55555/jndi/rmi://localhost:1099/karaf-"+child1.getId()+" child-profile"));

                Thread.sleep(DEFAULT_TIMEOUT);

                String sshUrl = child1.getSshUrl();
                String jmxUrl = child1.getJmxUrl();
                Assert.assertTrue(sshUrl.endsWith("8105"));
                Assert.assertTrue(jmxUrl.contains("55555"));
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
                //KarafDistributionOption.debugConfiguration("5005", true)
       };
    }
}

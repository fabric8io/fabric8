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
package org.fusesource.mq.itests;

import io.fabric8.api.FabricService;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;

import java.util.Set;

import io.fabric8.itests.paxexam.support.ContainerProxy;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;


@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class KillSessionFailOverTest extends GracefullFailOverTest {

    Set<ContainerProxy> setupCluster(ServiceProxy<FabricService> fabricProxy, String groupName, String brokerName) throws Exception {
        System.out.println(executeCommand("fabric:mq-create --group " + groupName + " " + brokerName));
        String profileName = "mq-broker-"+groupName+"."+brokerName;
        System.out.println(executeCommand("fabric:profile-edit --features fabric-zookeeper-commands " + profileName));
        return ContainerBuilder.child(fabricProxy, 2).withName("child").withProfiles(profileName).assertProvisioningResult().build();
    }


    void failOver(FabricService fabricService, String container) throws Exception {
        System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + container + " zk:kill"));
    }
}

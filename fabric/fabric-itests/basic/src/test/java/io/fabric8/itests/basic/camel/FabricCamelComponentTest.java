package io.fabric8.itests.basic.camel;


import org.apache.curator.framework.CuratorFramework;

import io.fabric8.api.Container;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import io.fabric8.zookeeper.ZkPath;

import org.fusesource.jansi.AnsiString;
import org.junit.After;
import org.junit.Ignore;
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

import scala.actors.threadpool.Arrays;

import java.util.LinkedHashSet;
import java.util.Set;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildren;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-668] Fix fabric basic FabricCamelComponentTest")
public class FabricCamelComponentTest extends FabricTestSupport {


    @After
    public void tearDown() throws InterruptedException {
        //ContainerBuilder.destroy();
    }

    @Test
    public void testRegistryEntries() throws Exception {
        int startingPort = 9090;
        System.err.println(executeCommand("fabric:create -n root"));
        CuratorFramework curator = getCurator();
        //Wait for zookeeper service to become available.
        System.err.println(executeCommand("fabric:profile-create --parents feature-camel fabric-camel"));
        System.err.println(executeCommand("fabric:profile-create --parents fabric-camel fabric-camel-server"));
        System.err.println(executeCommand("fabric:profile-create --parents fabric-camel fabric-camel-client"));
        System.err.println(executeCommand("fabric:profile-edit --repositories mvn:org.fusesource.examples.fabric-camel-cluster/features/" + System.getProperty("fabric.version") + "/xml/features fabric-camel"));

        System.err.println(executeCommand("fabric:profile-edit --features camel-server fabric-camel-server"));
        executeCommand("fabric:profile-edit --features camel-client fabric-camel-client");

        Set<Container> containers = ContainerBuilder.create(3).withName("fabric-camel").withProfiles("fabric-camel").assertProvisioningResult().build();

        //We will use the first container as a client and the rest as servers.
        Container camelClientContainer = containers.iterator().next();
        containers.remove(camelClientContainer);
        Set<Container> camelServerContainers = new LinkedHashSet<Container>(containers);


        int index = 1;
        for (Container c : camelServerContainers) {
            setData(curator, ZkPath.CONTAINER_PROVISION_RESULT.getPath(c.getId()), "changing");
            executeCommand("fabric:container-connect -u admin -p admin " + camelClientContainer.getId() + " log:set DEBUG");
            System.err.println(executeCommand("fabric:profile-create --parents fabric-camel-server fabric-camel-server-" + index));
            System.err.println(executeCommand("fabric:profile-edit --pid io.fabric8.examples.camel.loadbalancing.server/portNumber=" + (startingPort++) + " fabric-camel-server-" + index));
            System.err.println(executeCommand("fabric:profile-display fabric-camel-server-" + index));
            System.err.println(executeCommand("fabric:container-change-profile " + c.getId() + " fabric-camel-server-" + (index++)));
        }

        Provision.provisioningSuccess(camelServerContainers, PROVISION_TIMEOUT);
        setData(curator, ZkPath.CONTAINER_PROVISION_RESULT.getPath(camelClientContainer.getId()), "changing");
        System.err.println(executeCommand("fabric:container-change-profile " + camelClientContainer.getId() + " fabric-camel-client"));
        Provision.provisioningSuccess(Arrays.asList(new Container[]{camelClientContainer}), PROVISION_TIMEOUT);

        System.err.println(executeCommand("fabric:container-list"));
        System.err.println(executeCommand("fabric:profile-display --overlay fabric-camel-server"));

        //Check that the entries have been properly propagated.
        Assert.assertNotNull(exists(curator, "/fabric/registry/camel/endpoints/"));
        Assert.assertEquals(1, getChildren(curator, "/fabric/registry/camel/endpoints/").size());
        Thread.sleep(5000);
        System.err.println(executeCommand("fabric:container-connect -u admin -p admin " + camelClientContainer.getId() + " camel:route-list"));
        String response = new AnsiString(executeCommand("fabric:container-connect -u admin -p admin " + camelClientContainer.getId() + " camel:route-info fabric-client | grep Failed")).getPlain().toString();
        System.err.println(response);
        int failed = Integer.parseInt(response.replaceAll("[^0-9]", ""));
        Assert.assertEquals("Failed exchanges found on client",0, failed);

        //We want to kill all but one server, so we take out the first and keep it to the end.
        Container lastActiveServerContainer = camelServerContainers.iterator().next();
        camelServerContainers.remove(lastActiveServerContainer);
        for (Container c : camelServerContainers) {
            c.destroy();
            Thread.sleep(25000);
            response = new AnsiString(executeCommand("fabric:container-connect -u admin -p admin " + camelClientContainer.getId() + " camel:route-info fabric-client | grep Failed")).getPlain().toString();
            System.err.println(response);
            failed = Integer.parseInt(response.replaceAll("[^0-9]", ""));
            Assert.assertEquals("Failed exchanges found after container:"+c.getId()+ " shut down",0, failed);
        }
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(fabricDistributionConfiguration()),
                //debugConfiguration("5005",true),
                editConfigurationFilePut("etc/system.properties", "fabric.version", MavenUtils.asInProject().getVersion(GROUP_ID, ARTIFACT_ID))
        };
    }
}

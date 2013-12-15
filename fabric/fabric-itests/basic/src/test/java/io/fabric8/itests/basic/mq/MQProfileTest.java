package io.fabric8.itests.basic.mq;

import org.apache.activemq.broker.jmx.BrokerViewMBean;
import io.fabric8.api.Container;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import javax.management.ObjectName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
@Ignore("[FABRIC-674] Fix fabric basic MQProfileTest")
public class MQProfileTest extends FabricTestSupport {

    ArrayList<Container> containers = new ArrayList<Container>();

    @After
    public void tearDown() throws InterruptedException {
        for (Container container : containers) {
            System.out.println("destroying " + container.getId());
            destroyChildContainer(container.getId());
        }
        ContainerBuilder.destroy();
    }

    @Test
    public void testLocalChildCreation() throws Exception {

        System.err.println(executeCommand("fabric:create -n"));

        Set<Container> containers = ContainerBuilder.create().withName("mq").withProfiles("mq-default").assertProvisioningResult().build();

        installAndCheckFeature("activemq");


        Container container = getFabricService().getContainer("mq1");

        // give it a bit time
        Thread.sleep(5000);

        // check jmx stats
        BrokerViewMBean bean = (BrokerViewMBean)getMBean(container, new ObjectName("org.apache.activemq:type=Broker,brokerName=mq1"), BrokerViewMBean.class);
        assertEquals("Producer not present", 0, bean.getTotalProducerCount());
        assertEquals("Consumer not present", 0, bean.getTotalConsumerCount());


        containers.addAll(ContainerBuilder.create().withName("example").withProfiles("example-mq").assertProvisioningResult().build());
        // give it a bit time
        Thread.sleep(10000);
        assertEquals("Producer not present", 1, bean.getTotalProducerCount());
        assertEquals("Consumer not present", 1, bean.getTotalConsumerCount());
    }

    @Test
    public void testMQCreateBasic() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        //addStagingRepoToDefaultProfile();

        Thread.sleep(10000);

        System.out.println(executeCommand("mq-create --create-container mq --jmx-user admin --jmx-password admin --minimumInstances 1 mq"));
        // give it a bit time
        Thread.sleep(5000);

        installAndCheckFeature("activemq");

        Container container = getFabricService().getContainer("mq1");
        containers.add(container);
        Provision.containersStatus(Arrays.asList(container), "success", PROVISION_TIMEOUT);

        BrokerViewMBean bean = (BrokerViewMBean)getMBean(container, new ObjectName("org.apache.activemq:type=Broker,brokerName=mq"), BrokerViewMBean.class);

        System.out.println(executeCommand("container-list"));


        containers.addAll(ContainerBuilder.create().withName("example").withProfiles("example-mq").assertProvisioningResult().build());
        // give it a bit time
        Thread.sleep(10000);

        installAndCheckFeature("activemq");

        // check jmx stats
        assertEquals("Producer not present", 1, bean.getTotalProducerCount());
        assertEquals("Consumer not present", 1, bean.getTotalConsumerCount());
    }

    @Test
    public void testMQCreateMS() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));

        Thread.sleep(10000);

        executeCommand("mq-create --create-container broker --jmx-user admin --jmx-password admin ms-broker");
        Container container1 = getFabricService().getContainer("broker1");
        Container container2 = getFabricService().getContainer("broker2");
        containers.add(container1);
        containers.add(container2);

        Provision.containersStatus(containers, "success", PROVISION_TIMEOUT);
        installAndCheckFeature("activemq");
        Thread.sleep(10000);

        BrokerViewMBean broker1 = (BrokerViewMBean)getMBean(container1, new ObjectName("org.apache.activemq:type=Broker,brokerName=ms-broker"), BrokerViewMBean.class);

        assertEquals("ms-broker", broker1.getBrokerName());
        destroyChildContainer("broker1");
        containers.remove(container1);
        Thread.sleep(10000); // TODO implement wait for condition with timeout

        BrokerViewMBean broker2 = (BrokerViewMBean)getMBean(container2, new ObjectName("org.apache.activemq:type=Broker,brokerName=ms-broker"), BrokerViewMBean.class);
        assertEquals("ms-broker", broker2.getBrokerName());

        //TODO add example to verify failover

    }

    @Test
    public void testMQCreateNetwork() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        addStagingRepoToDefaultProfile();

        Thread.sleep(10000);

        executeCommand("mq-create --group us-east --networks us-west --create-container us-east --jmx-user admin --jmx-password admin --networks-username admin --networks-password admin --minimumInstances 1 us-east");
        executeCommand("mq-create --group us-west --networks us-east --create-container us-west --jmx-user admin --jmx-password admin --networks-username admin --networks-password admin --minimumInstances 1 us-west");
        Container container1 = getFabricService().getContainer("us-east1");
        containers.add(container1);
        Container container2 = getFabricService().getContainer("us-west1");
        containers.add(container2);

        Provision.containersStatus(containers, "success", PROVISION_TIMEOUT);

        containers.addAll(ContainerBuilder.create().withName("example-producer").withProfiles("example-mq-producer").withProfiles("mq-client-us-east").assertProvisioningResult().build());
        containers.addAll(ContainerBuilder.create().withName("example-consumer").withProfiles("example-mq-consumer").withProfiles("mq-client-us-west").assertProvisioningResult().build());
        // give it a bit time
        Thread.sleep(10000);

        installAndCheckFeature("activemq");

        BrokerViewMBean brokerEast = (BrokerViewMBean)getMBean(container1, new ObjectName("org.apache.activemq:type=Broker,brokerName=us-east"), BrokerViewMBean.class);
        BrokerViewMBean brokerWest = (BrokerViewMBean)getMBean(container2, new ObjectName("org.apache.activemq:type=Broker,brokerName=us-west"), BrokerViewMBean.class);

        assertFalse("Messages not sent", brokerEast.getTotalEnqueueCount() == 0);

        assertFalse("Messages not received", brokerWest.getTotalDequeueCount() == 0);

    }

    @Configuration
    public Option[] config() {
        return fabricDistributionConfiguration();
    }
}

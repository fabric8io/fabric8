package io.fabric8.itests.basic.mq;

import io.fabric8.api.Container;
import io.fabric8.groups.internal.ZooKeeperMultiGroup;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.curator.framework.CuratorFramework;
import org.fusesource.mq.fabric.FabricDiscoveryAgent.ActiveMQNode;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import javax.management.ObjectName;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class MQProfileTest extends FabricTestSupport {

    Map<String, Container> containers = new HashMap<String,Container>();

    @After
    public void tearDown() throws InterruptedException {
        for (Container container : containers.values()) {
            System.out.println("destroying " + container.getId());
            destroyChildContainer(container.getId());
        }
    }

    @Test
    public void testLocalChildCreation() throws Exception {

        System.err.println(executeCommand("fabric:create -n"));

        addAll(containers, ContainerBuilder.create().withName("mq").withProfiles("mq-default").assertProvisioningResult().build());


        Container container = getFabricService().getContainer("mq1");

        // check jmx stats
        final BrokerViewMBean bean = (BrokerViewMBean)Provision.getMBean(container, new ObjectName("org.apache.activemq:type=Broker,brokerName=mq1"), BrokerViewMBean.class, 30000);
        assertEquals("Producer not present", 0, bean.getTotalProducerCount());
        assertEquals("Consumer not present", 0, bean.getTotalConsumerCount());


        addAll(containers, ContainerBuilder.create().withName("example").withProfiles("example-mq").assertProvisioningResult().build());

        Provision.waitForCondition(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                while(bean.getTotalProducerCount() == 0 && bean.getTotalConsumerCount() == 0) {
                    Thread.sleep(1000);
                }
                return true;
            }
        }, 30000L);
        assertEquals("Producer not present", 1, bean.getTotalProducerCount());
        assertEquals("Consumer not present", 1, bean.getTotalConsumerCount());
    }

    @Test
    public void testMQCreateBasic() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));

        System.err.println(executeCommand("mq-create --create-container mq --jmx-user admin --jmx-password admin --minimumInstances 1 mq"));

        Provision.waitForCondition(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                while(getFabricService().getContainer("mq1") == null) {
                    Thread.sleep(1000);
                }
                return true;
            }
        }, 30000L);

        Container container = getFabricService().getContainer("mq1");
        containers.put("mq1", container);
        Provision.containersStatus(Arrays.asList(container), "success", PROVISION_TIMEOUT);

        final BrokerViewMBean bean = (BrokerViewMBean)Provision.getMBean(container, new ObjectName("org.apache.activemq:type=Broker,brokerName=mq"), BrokerViewMBean.class, 30000);

        System.err.println(executeCommand("container-list"));


        addAll(containers, ContainerBuilder.create().withName("example").withProfiles("example-mq").assertProvisioningResult().build());

        Provision.waitForCondition(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                while(bean.getTotalProducerCount() == 0 && bean.getTotalConsumerCount() == 0) {
                    Thread.sleep(1000);
                }
                return true;
            }
        }, 30000L);

        // check jmx stats
        assertEquals("Producer not present", 1, bean.getTotalProducerCount());
        assertEquals("Consumer not present", 1, bean.getTotalConsumerCount());
    }

    @Test
    @Ignore("[FABRIC-674] Fix fabric basic MQProfileTest")
    public void testMQCreateMS() throws Exception {

        System.err.println(executeCommand("fabric:create -n"));

        CuratorFramework curator = getCurator();

        executeCommand("mq-create --create-container broker --jmx-user admin --jmx-password admin ms-broker");
        Container container1 = getFabricService().getContainer("broker1");
        Container container2 = getFabricService().getContainer("broker2");
        containers.put("broker1", container1);
        containers.put("broker2", container2);

        Provision.containersStatus(containers.values(), "success", PROVISION_TIMEOUT);

        final ZooKeeperMultiGroup group = new ZooKeeperMultiGroup<ActiveMQNode>(curator, "/fabric/registry/clusters/fusemq/default", ActiveMQNode.class);
        group.start();
        ActiveMQNode master = null;
        Provision.waitForCondition(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                while ((ActiveMQNode)group.master() == null) {
                    Thread.sleep(1000);
                }
                return true;
            }
        }, 30000L);
        master = (ActiveMQNode)group.master();
        final String masterContainer = master.getContainer();
        System.err.println("master=" + masterContainer);

        BrokerViewMBean broker1 = (BrokerViewMBean)Provision.getMBean(getFabricService().getContainer(masterContainer), new ObjectName("org.apache.activemq:type=Broker,brokerName=ms-broker"), BrokerViewMBean.class, 30000);

        assertEquals("ms-broker", broker1.getBrokerName());

        destroyChildContainer(masterContainer);
        containers.remove(containers.get(masterContainer));

        Provision.waitForCondition(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                while (group.master() == null || group.master().getContainer() == masterContainer) {
                    Thread.sleep(1000);
                }
                return true;
            }
        }, 30000L);
        master = (ActiveMQNode)group.master();
        System.err.println("new master=" + master.getContainer());

        BrokerViewMBean broker2 = (BrokerViewMBean)Provision.getMBean(getFabricService().getContainer(master.getContainer()), new ObjectName("org.apache.activemq:type=Broker,brokerName=ms-broker"), BrokerViewMBean.class, 30000);
        assertEquals("ms-broker", broker2.getBrokerName());

        //TODO add example to verify failover

    }

    @Test
    @Ignore("[FABRIC-674] Fix fabric basic MQProfileTest")
    public void testMQCreateNetwork() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));

        executeCommand("mq-create --group us-east --networks us-west --create-container us-east --jmx-user admin --jmx-password admin --networks-username admin --networks-password admin --minimumInstances 1 us-east");
        executeCommand("mq-create --group us-west --networks us-east --create-container us-west --jmx-user admin --jmx-password admin --networks-username admin --networks-password admin --minimumInstances 1 us-west");
        Container container1 = getFabricService().getContainer("us-east1");
        containers.put("us-east1", container1);
        Container container2 = getFabricService().getContainer("us-west1");
        containers.put("us-west1", container2);

        Provision.containersStatus(containers.values(), "success", PROVISION_TIMEOUT);

        final BrokerViewMBean brokerEast = (BrokerViewMBean)Provision.getMBean(container1, new ObjectName("org.apache.activemq:type=Broker,brokerName=us-east"), BrokerViewMBean.class, 30000);
        final BrokerViewMBean brokerWest = (BrokerViewMBean)Provision.getMBean(container2, new ObjectName("org.apache.activemq:type=Broker,brokerName=us-west"), BrokerViewMBean.class, 30000);

        addAll(containers, ContainerBuilder.create().withName("example-producer").withProfiles("example-mq-producer").withProfiles("mq-client-us-east").assertProvisioningResult().build());
        addAll(containers, ContainerBuilder.create().withName("example-consumer").withProfiles("example-mq-consumer").withProfiles("mq-client-us-west").assertProvisioningResult().build());

        Provision.waitForCondition(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                while(brokerEast.getTotalEnqueueCount() == 0 && brokerWest.getTotalDequeueCount() == 0) {
                    Thread.sleep(1000);
                }
                return true;
            }
        }, 30000L);

        assertFalse("Messages not sent", brokerEast.getTotalEnqueueCount() == 0);

        assertFalse("Messages not received", brokerWest.getTotalDequeueCount() == 0);

    }

    public void addAll(Map<String, Container> containers, Set<Container> built) {
        for (Container container : built) {
            containers.put(container.getId(), container);
        }
    }

    @Configuration
   	public Option[] config() {
   		return new Option[]{
   				new DefaultCompositeOption(fabricDistributionConfiguration()),
                scanFeatures("default", "mq-fabric").start()
   		};
   	}
}

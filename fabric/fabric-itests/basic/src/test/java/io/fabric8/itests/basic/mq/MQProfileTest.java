package io.fabric8.itests.basic.mq;

import io.fabric8.api.Container;
import io.fabric8.api.Profile;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
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
import java.util.Set;
import java.util.concurrent.Callable;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class MQProfileTest extends FabricTestSupport {


    @After
    public void tearDown() throws InterruptedException {
        ContainerBuilder.destroy();
    }

    @Test
    public void testLocalChildCreation() throws Exception {

        System.err.println(executeCommand("fabric:create -n"));
        Set<Container> containers = ContainerBuilder.create(2).withName("child").withProfiles("default").assertProvisioningResult().build();
        Container broker = containers.iterator().next();
        containers.remove(broker);

        Profile brokerProfile = broker.getVersion().getProfile("mq-default");
        broker.setProfiles(new Profile[]{brokerProfile});

        Provision.provisioningSuccess(Arrays.asList(broker), PROVISION_TIMEOUT);

        // check jmx stats
        final BrokerViewMBean bean = (BrokerViewMBean)Provision.getMBean(broker, new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getId()), BrokerViewMBean.class, 30000);
        assertEquals("Producer not present", 0, bean.getTotalProducerCount());
        assertEquals("Consumer not present", 0, bean.getTotalConsumerCount());


        for (Container c : containers) {
            Profile exampleProfile = broker.getVersion().getProfile("example-mq");
            c.setProfiles(new Profile[]{exampleProfile});
        }

        Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);

        Provision.waitForCondition(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                while(bean.getTotalProducerCount() == 0 || bean.getTotalConsumerCount() == 0) {
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
        System.err.println(executeCommand("mq-create --jmx-user admin --jmx-password admin --minimumInstances 1 mq"));

        Set<Container> containers = ContainerBuilder.create(2).withName("child").withProfiles("default").assertProvisioningResult().build();
        Container broker = containers.iterator().next();
        containers.remove(broker);

        Profile brokerProfile = broker.getVersion().getProfile("mq-broker-default.mq");
        broker.setProfiles(new Profile[]{brokerProfile});

        Provision.provisioningSuccess(Arrays.asList(broker), PROVISION_TIMEOUT);

        final BrokerViewMBean bean = (BrokerViewMBean)Provision.getMBean(broker, new ObjectName("org.apache.activemq:type=Broker,brokerName=mq"), BrokerViewMBean.class, 30000);

        System.err.println(executeCommand("container-list"));

        for (Container c : containers) {
            Profile exampleProfile = broker.getVersion().getProfile("example-mq");
            c.setProfiles(new Profile[]{exampleProfile});
        }

        Provision.provisioningSuccess(containers, PROVISION_TIMEOUT);

        Provision.waitForCondition(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                while(bean.getTotalProducerCount() == 0 || bean.getTotalConsumerCount() == 0) {
                    Thread.sleep(1000);
                }
                return true;
            }
        }, 30000L);
        assertEquals("Producer not present", 1, bean.getTotalProducerCount());
        assertEquals("Consumer not present", 1, bean.getTotalConsumerCount());
    }


    @Test
    public void testMQCreateNetwork() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));

        executeCommand("mq-create --group us-east --networks us-west --jmx-user admin --jmx-password admin --networks-username admin --networks-password admin --minimumInstances 1 us-east");
        executeCommand("mq-create --group us-west --networks us-east --jmx-user admin --jmx-password admin --networks-username admin --networks-password admin --minimumInstances 1 us-west");
        Set<Container> containers = ContainerBuilder.create(4).withName("child").withProfiles("default").assertProvisioningResult().build();

        Container eastBroker = containers.iterator().next();
        containers.remove(eastBroker);

        Container westBroker = containers.iterator().next();
        containers.remove(westBroker);

        Profile eastBrokerProfile = eastBroker.getVersion().getProfile("mq-broker-us-east.us-east");
        eastBroker.setProfiles(new Profile[]{eastBrokerProfile});

        Profile westBrokerProfile = eastBroker.getVersion().getProfile("mq-broker-us-west.us-west");
        westBroker.setProfiles(new Profile[]{westBrokerProfile});

        Provision.provisioningSuccess(Arrays.asList(westBroker, eastBroker), PROVISION_TIMEOUT);

        final BrokerViewMBean brokerEast = (BrokerViewMBean)Provision.getMBean(eastBroker, new ObjectName("org.apache.activemq:type=Broker,brokerName=us-east"), BrokerViewMBean.class, 30000);
        final BrokerViewMBean brokerWest = (BrokerViewMBean)Provision.getMBean(westBroker, new ObjectName("org.apache.activemq:type=Broker,brokerName=us-west"), BrokerViewMBean.class, 30000);


        Container eastProducer = containers.iterator().next();
        containers.remove(eastProducer);
        executeCommand("container-add-profile " + eastProducer.getId()+" example-mq-producer mq-client-us-east");
        Container westConsumer = containers.iterator().next();
        containers.remove(westConsumer);
        executeCommand("container-add-profile " + westConsumer.getId() + " example-mq-consumer mq-client-us-west");

        Provision.provisioningSuccess(Arrays.asList(eastProducer, westConsumer), PROVISION_TIMEOUT);

        System.out.println(executeCommand("fabric:container-list"));

        Provision.waitForCondition(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                while(brokerEast.getTotalEnqueueCount() == 0 || brokerWest.getTotalDequeueCount() == 0) {
                    Thread.sleep(1000);
                }
                return true;
            }
        }, 60000L);

        System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + eastBroker.getId() + " bstat"));
        System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + westBroker.getId() + " bstat"));

        assertFalse("Messages not sent", brokerEast.getTotalEnqueueCount() == 0);

        assertFalse("Messages not received", brokerWest.getTotalDequeueCount() == 0);

    }

    @Configuration
   	public Option[] config() {
   		return new Option[]{
   				new DefaultCompositeOption(fabricDistributionConfiguration()),
                scanFeatures("default", "mq-fabric").start()
   		};
   	}
}

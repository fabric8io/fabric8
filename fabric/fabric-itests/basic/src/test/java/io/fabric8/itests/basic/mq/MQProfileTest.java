package io.fabric8.itests.basic.mq;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ServiceProxy;
import io.fabric8.itests.paxexam.support.ContainerBuilder;
import io.fabric8.itests.paxexam.support.ContainerProxy;
import io.fabric8.itests.paxexam.support.FabricTestSupport;
import io.fabric8.itests.paxexam.support.Provision;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.ObjectName;

import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.command.DiscoveryEvent;
import org.apache.activemq.transport.discovery.DiscoveryListener;
import org.apache.curator.framework.CuratorFramework;
import org.fusesource.mq.fabric.FabricDiscoveryAgent;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class MQProfileTest extends FabricTestSupport {

    @Test
    public void testLocalChildCreation() throws Exception {

        System.err.println(executeCommand("fabric:create -n"));
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            Set<ContainerProxy> containers= ContainerBuilder.create(fabricProxy, 2).withName("child").withProfiles("default").assertProvisioningResult().build();
            try {
                LinkedList<Container> containerList = new LinkedList<Container>(containers);
                Container broker = containerList.removeLast();

                Profile brokerProfile = broker.getVersion().getProfile("mq-default");
                broker.setProfiles(new Profile[]{brokerProfile});

                Provision.provisioningSuccess(Arrays.asList(broker), PROVISION_TIMEOUT);

                waitForBroker("default");

                // check jmx stats
                final BrokerViewMBean bean = (BrokerViewMBean)Provision.getMBean(broker, new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getId()), BrokerViewMBean.class, 120000);
                Assert.assertEquals("Producer not present", 0, bean.getTotalProducerCount());
                Assert.assertEquals("Consumer not present", 0, bean.getTotalConsumerCount());


                for (Container c : containerList) {
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
                }, 120000L);
                Assert.assertEquals("Producer not present", 1, bean.getTotalProducerCount());
                Assert.assertEquals("Consumer not present", 1, bean.getTotalConsumerCount());
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Test
    public void testMQCreateBasic() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));
        System.err.println(executeCommand("mq-create --jmx-user admin --jmx-password admin --minimumInstances 1 mq"));

        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy, 2).withName("child").withProfiles("default").assertProvisioningResult().build();
            try {
                LinkedList<Container> containerList = new LinkedList<Container>(containers);
                Container broker = containerList.removeLast();

                Profile brokerProfile = broker.getVersion().getProfile("mq-broker-default.mq");
                broker.setProfiles(new Profile[]{brokerProfile});

                Provision.provisioningSuccess(Arrays.asList(broker), PROVISION_TIMEOUT);

                waitForBroker("default");

                final BrokerViewMBean bean = (BrokerViewMBean)Provision.getMBean(broker, new ObjectName("org.apache.activemq:type=Broker,brokerName=mq"), BrokerViewMBean.class, 120000);

                System.err.println(executeCommand("container-list"));

                for (Container c : containerList) {
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
                }, 120000L);
                Assert.assertEquals("Producer not present", 1, bean.getTotalProducerCount());
                Assert.assertEquals("Consumer not present", 1, bean.getTotalConsumerCount());
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    @Test
    public void testMQCreateNetwork() throws Exception {
        System.err.println(executeCommand("fabric:create -n"));

        executeCommand("mq-create --group us-east --networks us-west --jmx-user admin --jmx-password admin --networks-username admin --networks-password admin --minimumInstances 1 us-east");
        executeCommand("mq-create --group us-west --networks us-east --jmx-user admin --jmx-password admin --networks-username admin --networks-password admin --minimumInstances 1 us-west");
        ServiceProxy<FabricService> fabricProxy = ServiceProxy.createServiceProxy(bundleContext, FabricService.class);
        try {
            Set<ContainerProxy> containers = ContainerBuilder.create(fabricProxy, 4).withName("child").withProfiles("default").assertProvisioningResult().build();
            try {
                LinkedList<Container> containerList = new LinkedList<Container>(containers);
                Container eastBroker = containerList.removeLast();
                Container westBroker = containerList.removeLast();


                Profile eastBrokerProfile = eastBroker.getVersion().getProfile("mq-broker-us-east.us-east");
                eastBroker.setProfiles(new Profile[]{eastBrokerProfile});

                Profile westBrokerProfile = eastBroker.getVersion().getProfile("mq-broker-us-west.us-west");
                westBroker.setProfiles(new Profile[]{westBrokerProfile});

                Provision.provisioningSuccess(Arrays.asList(westBroker, eastBroker), PROVISION_TIMEOUT);

                waitForBroker("us-east");
                waitForBroker("us-west");

                final BrokerViewMBean brokerEast = (BrokerViewMBean)Provision.getMBean(eastBroker, new ObjectName("org.apache.activemq:type=Broker,brokerName=us-east"), BrokerViewMBean.class, 120000);
                final BrokerViewMBean brokerWest = (BrokerViewMBean)Provision.getMBean(westBroker, new ObjectName("org.apache.activemq:type=Broker,brokerName=us-west"), BrokerViewMBean.class, 120000);


                Container eastProducer = containerList.removeLast();
                executeCommand("container-add-profile " + eastProducer.getId()+" example-mq-producer mq-client-us-east");
                Container westConsumer = containerList.removeLast();
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
                }, 120000L);

                System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + eastBroker.getId() + " bstat"));
                System.out.println(executeCommand("fabric:container-connect -u admin -p admin " + westBroker.getId() + " bstat"));

                Assert.assertFalse("Messages not sent", brokerEast.getTotalEnqueueCount() == 0);

                Assert.assertFalse("Messages not received", brokerWest.getTotalDequeueCount() == 0);
            } finally {
                ContainerBuilder.destroy(containers);
            }
        } finally {
            fabricProxy.close();
        }
    }

    protected void waitForBroker(String groupName) throws Exception {
        ServiceProxy<CuratorFramework> curatorProxy = ServiceProxy.createServiceProxy(bundleContext, CuratorFramework.class);
        try {
            CuratorFramework curator = curatorProxy.getService();

            final CountDownLatch serviceLatch = new CountDownLatch(1);
            final FabricDiscoveryAgent discoveryAgent = new FabricDiscoveryAgent();

            discoveryAgent.setCurator(curator);
            discoveryAgent.setGroupName(groupName);
            discoveryAgent.setDiscoveryListener( new DiscoveryListener() {
                @Override
                public void onServiceAdd(DiscoveryEvent discoveryEvent) {
                    System.out.println("Service added:" + discoveryEvent.getServiceName());
                    serviceLatch.countDown();
                    try {
                        discoveryAgent.stop();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onServiceRemove(DiscoveryEvent discoveryEvent) {
                    System.out.println("Service removed:" + discoveryEvent.getServiceName());
                }
            });

            discoveryAgent.start();
            Assert.assertTrue(serviceLatch.await(15, TimeUnit.MINUTES));
        } finally {
            curatorProxy.close();
        }
    }

    @Configuration
   	public Option[] config() {
   		return new Option[]{
   				new DefaultCompositeOption(fabricDistributionConfiguration()),
                CoreOptions.scanFeatures("default", "mq-fabric").start()
   		};
   	}
}

package org.fusesource.fabric.itests.paxexam.mq;

import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.itests.paxexam.FabricTestSupport;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.HashMap;
import java.util.Map;

import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;
import static junit.framework.Assert.assertEquals;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class MQProfileTest extends FabricTestSupport {

    @After
    public void tearDown() throws InterruptedException {
       destroyChildContainer("mq1");
       destroyChildContainer("example");
    }

    @Test
    public void testLocalChildCreation() throws Exception {
        System.err.println(executeCommand("fabric:create"));
        addStagingRepoToDefaultProfile();
        createAndAssetChildContainer("mq1", "root", "mq");

        createAndAssetChildContainer("example", "root", "example-mq");


        // give it a bit time
        Thread.sleep(3000);

        // check jmx stats
        Container container = getFabricService().getContainer("mq1");
        System.out.println(container.getJmxUrl());

        JMXServiceURL url = new JMXServiceURL(container.getJmxUrl());
        Map env = new HashMap();
        String[] creds = {"admin", "admin"};
        env.put(JMXConnector.CREDENTIALS, creds);
        JMXConnector jmxc = JMXConnectorFactory.connect(url, env);
        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        //TODO investigate why the broker is named karaf.name in this case
        ObjectName mbeanName = new ObjectName("org.apache.activemq:Type=Broker,BrokerName=karaf.name");
        BrokerViewMBean bean = JMX.newMBeanProxy(mbsc, mbeanName,
                BrokerViewMBean.class, true);
        assertEquals("Producer not present", 1, bean.getTotalProducerCount());
        assertEquals("Consumer not present", 1, bean.getTotalConsumerCount());
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
                fabricDistributionConfiguration(), keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.ERROR)};
    }
}

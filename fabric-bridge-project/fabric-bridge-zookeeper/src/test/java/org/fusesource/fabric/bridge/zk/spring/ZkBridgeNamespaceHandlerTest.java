package org.fusesource.fabric.bridge.zk.spring;

import org.fusesource.fabric.bridge.internal.AbstractConnectorTestSupport;
import org.fusesource.fabric.bridge.model.BridgeDestinationsConfig;
import org.fusesource.fabric.bridge.zk.ZkBridgeConnector;
import org.fusesource.fabric.bridge.zk.ZkGatewayConnector;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.jms.JMSException;
import javax.jms.TextMessage;

public class ZkBridgeNamespaceHandlerTest extends AbstractZkConnectorTestSupport {

	private static final Logger LOG = LoggerFactory.getLogger(ZkBridgeNamespaceHandlerTest.class);
    private static AbstractApplicationContext applicationContextZkServer;

    @BeforeClass
    public static void setUpClass() {
        applicationContextZkServer = new ClassPathXmlApplicationContext("test-zkserver.xml");
        applicationContext = new ClassPathXmlApplicationContext("test-zk-context.xml");
    }

    @AfterClass
    public static void tearDownClass() {
        applicationContext.destroy();
        applicationContextZkServer.destroy();
    }

	@Test
	public void testZkBridgeConnector() {
		ZkBridgeConnector bean = applicationContext.getBean("myzkbridge", ZkBridgeConnector.class);
		LOG.info(bean.toString());
		assertTrue("ZkBridgeConnector not started", bean.isRunning());
	}

    @Test
    public void testZkGatewayConnector() {
        ZkGatewayConnector bean = applicationContext.getBean("myzkgateway", ZkGatewayConnector.class);
        LOG.info(bean.toString());
        assertTrue("ZkGatewayConnector not started", bean.isRunning());
    }

}

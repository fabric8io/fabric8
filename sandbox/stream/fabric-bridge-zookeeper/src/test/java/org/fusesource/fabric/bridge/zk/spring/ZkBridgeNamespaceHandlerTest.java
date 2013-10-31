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
package org.fusesource.fabric.bridge.zk.spring;

import org.fusesource.fabric.bridge.zk.ZkBridgeConnector;
import org.fusesource.fabric.bridge.zk.ZkGatewayConnector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@Ignore("[FABRIC-687] Fix fabric bridge zookeeper ZkBridgeNamespaceHandlerTest")
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

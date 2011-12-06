/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.bridge.zk.spring;

import org.fusesource.fabric.bridge.model.BridgeDestinationsConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ZkConfigTest extends AbstractZkConnectorTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ZkConfigTest.class);
    private static AbstractApplicationContext applicationContextZkServer;

    @BeforeClass
    public static void setUpClass() {
        applicationContextZkServer = new ClassPathXmlApplicationContext("test-zkserver.xml");
        AbstractZkConnectorTestSupport.applicationContext = new ClassPathXmlApplicationContext("test-zkconfig-context.xml");
    }

    @AfterClass
    public static void tearDownClass() {
        AbstractZkConnectorTestSupport.applicationContext.destroy();
        applicationContextZkServer.destroy();
    }

    @Test
    public void testZkBridgeDestinationsConfig() {
        BridgeDestinationsConfig bean = AbstractZkConnectorTestSupport.applicationContext.getBean("upstream", BridgeDestinationsConfig.class);
        LOG.info(bean.toString());
        assertTrue("BridgeDestinationsConfig not started", bean.getDestinations().get(0).getName().equals("source1"));

        bean = AbstractZkConnectorTestSupport.applicationContext.getBean("downstream", BridgeDestinationsConfig.class);
        LOG.info(bean.toString());
        assertTrue("BridgeDestinationsConfig not started", bean.getDestinations().get(0).getName().equals("source1.out"));
    }

}

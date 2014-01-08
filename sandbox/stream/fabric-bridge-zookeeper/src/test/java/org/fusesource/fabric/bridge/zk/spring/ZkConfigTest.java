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
package io.fabric8.bridge.zk.spring;

import io.fabric8.bridge.model.BridgeDestinationsConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
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

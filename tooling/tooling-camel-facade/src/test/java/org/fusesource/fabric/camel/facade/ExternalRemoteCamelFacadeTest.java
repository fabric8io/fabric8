/*
 * Copyright 2010 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package org.fusesource.fabric.camel.facade;

import java.util.List;
import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import junit.framework.TestCase;
import org.fusesource.fabric.camel.facade.mbean.CamelContextMBean;
import org.fusesource.fabric.camel.facade.mbean.CamelRouteMBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore("[FABRIC-678] Fix tooling camel ExternalRemoteCamelFacadeTest")
public class ExternalRemoteCamelFacadeTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(ExternalRemoteCamelFacadeTest.class);
    private RemoteJMXCamelFacade remote;
    private JMXConnector connector;
    private final String jmxUrl = "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi/camel";

    @Before
    public void setUp() throws Exception {
        LOG.info("Connecting to remote JVM over JMX using: {}", jmxUrl);
        JMXServiceURL url = new JMXServiceURL(jmxUrl);
        connector = JMXConnectorFactory.connect(url, null);
        MBeanServerConnection mbean = connector.getMBeanServerConnection();
        remote = new RemoteJMXCamelFacade(mbean);
    }

    @After
    public void tearDown() throws Exception {
        LOG.debug("Closing connection to remote JVM over JMX");
        connector.close();
    }

    @Test
    public void testGetId() throws Exception {
        String name = remote.getCamelContexts().get(0).getCamelId();
        assertEquals("camel", name);
    }

    @Test
    public void testGetCamelContext() throws Exception {
        CamelContextMBean context = remote.getCamelContexts().get(0);
        assertNotNull(context);
        assertEquals("camel", context.getCamelId());
        assertNotNull(context.getUptime());

        LOG.info("Update from remote Camel is {}", context.getUptime());
    }

    @Test
    public void testGetRoutes() throws Exception {
        String id = remote.getCamelContexts().get(0).getCamelId();
        List<CamelRouteMBean> routes = remote.getRoutes(id);
        assertNotNull(routes);
        assertTrue("There should be routes", routes.size() > 0);

        for (CamelRouteMBean route : routes) {
            LOG.info("Route {} consuming from {}", route.getRouteId(), route.getEndpointUri());
            String xml = route.dumpRouteAsXml();
            LOG.info(xml);
        }
    }

}

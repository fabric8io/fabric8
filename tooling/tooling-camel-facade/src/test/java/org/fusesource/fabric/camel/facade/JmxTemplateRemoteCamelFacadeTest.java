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

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.management.DefaultManagementAgent;
import org.apache.camel.management.DefaultManagementLifecycleStrategy;
import org.apache.camel.management.ManagedManagementStrategy;
import org.apache.camel.spi.ManagementStrategy;
import org.fusesource.fabric.service.JmxTemplate;
import org.fusesource.fabric.service.JmxTemplateSupport;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import static org.apache.camel.util.ServiceHelper.startServices;

@Ignore("[FABRIC-679] Fix tooling camel JmxTemplateRemoteCamelFacadeTest")
public class JmxTemplateRemoteCamelFacadeTest extends RemoteCamelFacadeTest {
    private static final Logger LOG = LoggerFactory.getLogger(JmxTemplateRemoteCamelFacadeTest.class);

    protected String serviceUrl = "/jmxrmi/myCamelTest";
    protected int rmiPort = 1099;
    //protected String jmxUrl = "service:jmx:rmi:///jndi/rmi://localhost:" + rmiPort + serviceUrl;
    protected String jmxUrl = "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi/camel";

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected CamelContext createCamelContext() throws Exception {

        // lets create a connector
        // none of these work due to the broken lifecycle...

        /*
        answer.getManagementStrategy().getManagementAgent().setCreateConnector(true);
        */

        /*
        ManagementStrategy managementStrategy = new DefaultManagementStrategy();
        DefaultManagementAgent agent = new DefaultManagementAgent(answer);
        agent.setCreateConnector(true);
        managementStrategy.setManagementAgent(agent);
        answer.setManagementStrategy(managementStrategy);
        startServices(answer);
        */


        // TODO shouldn't have to do this crap to enable a JMX connector!!!

        DefaultCamelContext answer = new DefaultCamelContext() {

            protected ManagementStrategy createManagementStrategy() {
                ManagementStrategy answer = null;
                try {
                    log.info("JMX enabled. Using ManagedManagementStrategy.");
                    DefaultManagementAgent agent = new DefaultManagementAgent(this);
                    agent.setCreateConnector(true);
                    answer = new ManagedManagementStrategy(agent);
                    // must start it to ensure JMX works and can load needed Spring JARs
                    startServices(answer);
                    // prefer to have it at first strategy
                    getLifecycleStrategies().add(0, new DefaultManagementLifecycleStrategy(this));
                } catch (NoClassDefFoundError e) {
                    answer = null;

                    // if we can't instantiate the JMX enabled strategy then fallback to default
                    // could be because of missing .jars on the classpath
                    log.warn("Cannot find needed classes for JMX lifecycle strategy."
                            + " Needed class is in spring-context.jar using Spring 2.5 or newer"
                            + " (spring-jmx.jar using Spring 2.0.x)."
                            + " NoClassDefFoundError: " + e.getMessage());
                } catch (Exception e) {
                    answer = null;
                    log.warn("Cannot create JMX lifecycle strategy. Fallback to using DefaultManagementStrategy (non JMX).", e);
                }

                // inject CamelContext
                if (answer instanceof CamelContextAware) {
                    CamelContextAware aware = (CamelContextAware) answer;
                    aware.setCamelContext(this);
                }

                return answer;
            }
        };
        answer.setName(name);
        return answer;
    }


    @Override
    protected CamelFacade createCamelFacade() throws Exception {
        LOG.info("Connecting to remote JVM over JMX using: {}", jmxUrl);
        JMXServiceURL url = new JMXServiceURL(jmxUrl);
        JMXConnector connector = JMXConnectorFactory.connect(url, null);
        JmxTemplateSupport jmxTemplate = new JmxTemplate(connector);
        return new JmxTemplateCamelFacade(jmxTemplate);
    }
}

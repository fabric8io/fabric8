/**
 *  Copyright 2005-2014 Red Hat, Inc.
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
package io.fabric8.itests.common;

import io.fabric8.api.mxbean.ManagementUtils;
import io.fabric8.api.mxbean.ProfileManagement;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.JMX;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;

/**
 * Test client side {@link ProfileManagement} test
 *
 * @since 15-Sep-2014
 */
@RunAsClient
@RunWith(Arquillian.class)
@Ignore("[FABRIC-1173] Cannot reliably delete profile version")
public class ProfileManagementProxyTest extends AbstractProfileManagementTest {

    static final String[] credentials = new String[] { "admin", "admin" };
    
    static ProfileManagement proxy;
    static JMXConnector connector;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        String jmxServiceURL = "service:jmx:rmi://127.0.0.1:44444/jndi/rmi://127.0.0.1:1099/karaf-root";
        Map<String, Object> env = ManagementUtils.getDefaultEnvironment(jmxServiceURL);
        env.put(JMXConnector.CREDENTIALS, new String[] { credentials[0], credentials[1] });
        connector = ManagementUtils.getJMXConnector(jmxServiceURL, env, 10, TimeUnit.SECONDS);
        proxy = JMX.newMXBeanProxy(connector.getMBeanServerConnection(), new ObjectName(ProfileManagement.OBJECT_NAME), ProfileManagement.class);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        connector.close();
    }

    @Override
    ProfileManagement getProxy() {
        return proxy;
    }
}

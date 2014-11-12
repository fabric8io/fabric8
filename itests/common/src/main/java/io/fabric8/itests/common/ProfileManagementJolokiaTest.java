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

import io.fabric8.api.mxbean.ProfileManagement;
import io.fabric8.jolokia.client.JolokiaMXBeanProxy;

import javax.management.ObjectName;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

/**
 * Test client side {@link ProfileManagement} test via jolokia
 *
 * @since 15-Sep-2014
 */
@RunAsClient
@RunWith(Arquillian.class)
public class ProfileManagementJolokiaTest extends AbstractProfileManagementTest {

    static final String[] credentials = new String[] { "admin", "admin" };
    
    static ProfileManagement proxy;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        String jmxServiceURL = "http://localhost:8181/jolokia";
        proxy = JolokiaMXBeanProxy.getMXBeanProxy(jmxServiceURL, new ObjectName(ProfileManagement.OBJECT_NAME), ProfileManagement.class, credentials[0], credentials[1]);
    }

    @Override
    ProfileManagement getProxy() {
        return proxy;
    }
}

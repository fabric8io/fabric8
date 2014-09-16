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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test client side {@link ProfileManagement} via jolokia
 *
 * @since 15-Sep-2014
 */
@RunAsClient
@RunWith(Arquillian.class)
public class ProfileManagementTest {

    static final String[] credentials = new String[] { "admin", "admin" };
    static final Map<String, String> endpointMapping = new HashMap<>();
    static {
        endpointMapping.put("karaf", "http://localhost:8181/jolokia");
        endpointMapping.put("tomcat", "http://localhost:8080/fabric/jolokia");
        endpointMapping.put("wildfly", "http://localhost:8080/fabric/jolokia");
    }
    
    static ProfileManagement proxy;
    
    @BeforeClass
    public static void beforeClass() {
        String serviceURL = endpointMapping.get(System.getProperty("target.container"));
        proxy = JolokiaMXBeanProxy.getMXBeanProxy(serviceURL, credentials[0], credentials[1], ProfileManagement.OBJECT_NAME, ProfileManagement.class);
    }
    
    @Test
    public void testGetVersions() throws Exception {
        List<String> versions = proxy.getVersions();
        versions.remove("master");
        Assert.assertEquals(1, versions.size());
        Assert.assertEquals("1.0", versions.get(0));
    }
}

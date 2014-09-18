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

import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.mxbean.ManagementUtils;
import io.fabric8.api.mxbean.ProfileManagement;
import io.fabric8.api.mxbean.ProfileState;
import io.fabric8.api.mxbean.VersionState;
import io.fabric8.container.wildfly.connector.WildFlyManagementUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.management.JMX;
import javax.management.remote.JMXConnector;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.runtime.RuntimeType;
import org.jboss.gravia.utils.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
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
    
    static JMXConnector connector;
    static ProfileManagement proxy;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        connector = getJMXConnector();
        proxy = JMX.newMXBeanProxy(connector.getMBeanServerConnection(), ProfileManagement.OBJECT_NAME, ProfileManagement.class);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        IOUtils.safeClose(connector);
    }

    private static JMXConnector getJMXConnector() {
        JMXConnector connector = null;
        RuntimeType runtimeType = RuntimeType.getRuntimeType(System.getProperty("target.container"));
        if (runtimeType == RuntimeType.KARAF) {
            String jmxServiceURL = "service:jmx:rmi://127.0.0.1:44444/jndi/rmi://127.0.0.1:1099/karaf-root";
            Map<String, Object> env = ManagementUtils.getDefaultEnvironment(jmxServiceURL);
            env.put(JMXConnector.CREDENTIALS, new String[] { credentials[0], credentials[1] });
            connector = ManagementUtils.getJMXConnector(jmxServiceURL, env, 10, TimeUnit.SECONDS);
        } else if (runtimeType == RuntimeType.TOMCAT) {
            String jmxServiceURL = "service:jmx:rmi:///jndi/rmi://127.0.0.1:8089/jmxrmi";
            Map<String, Object> env = ManagementUtils.getDefaultEnvironment(jmxServiceURL);
            env.put(JMXConnector.CREDENTIALS, new String[] { credentials[0], credentials[1] });
            connector = ManagementUtils.getJMXConnector(jmxServiceURL, env, 10, TimeUnit.SECONDS);
        } else if (runtimeType == RuntimeType.WILDFLY) {
            String jmxServiceURL = "service:jmx:http-remoting-jmx://127.0.0.1:9990";
            Map<String, Object> env = ManagementUtils.getDefaultEnvironment(jmxServiceURL);
            env.put(JMXConnector.CREDENTIALS, new String[] { credentials[0], credentials[1] });
            connector = WildFlyManagementUtils.getJMXConnector(jmxServiceURL, env, 10, TimeUnit.SECONDS);
        }
        return connector;
    }
    
    @Test
    public void testGetVersions() throws Exception {
        List<String> versions = new ArrayList<>(proxy.getVersionIds());
        versions.remove("master");
        Assert.assertEquals(1, versions.size());
        Assert.assertEquals("1.0", versions.get(0));
    }

    @Test
    public void testGetVersion() throws Exception {
        VersionState version = proxy.getVersion("1.0");
        Map<String, String> attributes = version.getAttributes();
        Assert.assertTrue(attributes.isEmpty());
        List<String> profiles = new ArrayList<>(version.getProfileIds());
        Assert.assertTrue(profiles.contains("default"));
        Assert.assertTrue(profiles.contains("fabric"));
    }

    @Test
    @Ignore
    public void testProfileCreateUpdateDelete() throws Exception {
        
        // fabric:profile-create prfA
        ProfileBuilder pbA10 = ProfileBuilder.Factory.create("1.0", "prfA")
                .addConfiguration("pidA", Collections.singletonMap("keyA", "valA"));
        ProfileState prfA10 = proxy.createProfile(new ProfileState(pbA10.getProfile()));
        Assert.assertEquals("1.0", prfA10.getVersion());
        Assert.assertEquals("prfA", prfA10.getId());
        Assert.assertEquals("valA", prfA10.getConfiguration("pidA").get("keyA"));
        
        // Verify access to original profile
        Assert.assertNotNull(proxy.getVersion("1.0").getProfileState("prfA"));
        
        // fabric:version-create --parent 1.0 1.1
        VersionState v11 = proxy.createVersion("1.0", "1.1", null);
        ProfileState prfA11a = v11.getProfileState("prfA");
        Assert.assertEquals("1.1", prfA11a.getVersion());
        Assert.assertEquals("prfA", prfA11a.getId());
        Assert.assertEquals("valA", prfA11a.getConfiguration("pidA").get("keyA"));
        
        // Verify access to original profile
        Assert.assertNotNull(proxy.getVersion("1.0").getProfileState("prfA"));
        Assert.assertNotNull(proxy.getVersion("1.1").getProfileState("prfA"));
        
        ProfileBuilder pbA11 = ProfileBuilder.Factory.createFrom(prfA11a.toProfile())
                .addConfiguration("pidA", Collections.singletonMap("keyB", "valB"));
        ProfileState prfA11b = proxy.createProfile(new ProfileState(pbA11.getProfile()));
        Assert.assertEquals("1.1", prfA11b.getVersion());
        Assert.assertEquals("prfA", prfA11b.getId());
        Assert.assertEquals("valB", prfA11b.getConfiguration("pidA").get("keyB"));
        
        Assert.assertNotEquals(prfA11a, prfA11b);
        // System.out.println(Profiles.getProfileDifference(prfA11a, prfA11b));
        
        // Verify access to original profile
        Assert.assertNotNull(proxy.getVersion("1.0").getProfileState("prfA"));
        Assert.assertNotNull(proxy.getVersion("1.1").getProfileState("prfA"));
        
        // Delete the profile/version that were added
        proxy.deleteProfile(prfA10.getVersion(), prfA10.getId(), true);
        proxy.deleteVersion(v11.getId());
    }
}

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

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.VersionBuilder;
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
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test client side {@link ProfileManagement} test
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
        Assert.assertEquals("Expected 1, but was" + versions, 1, versions.size());
        Assert.assertEquals("1.0", versions.get(0));
    }

    @Test
    public void testGetVersion() throws Exception {
        VersionState v10 = proxy.getVersion("1.0");
        Assert.assertEquals("1.0", v10.getId());
        Assert.assertTrue("Expected empty, but was" + v10.getAttributes(), v10.getAttributes().isEmpty());
        List<String> profiles = v10.getProfileIds();
        Assert.assertTrue(profiles.contains("default"));
        Assert.assertTrue(profiles.contains("fabric"));
    }

    @Test
    public void testCreateVersion() throws Exception {
        ProfileBuilder pbA11 = ProfileBuilder.Factory.create("1.1", "prfA");
        Profile prfA = pbA11.addConfiguration("pidA", Collections.singletonMap("keyA", "valA")).getProfile();
        VersionBuilder vb11 = VersionBuilder.Factory.create("1.1").addProfile(prfA);
        VersionState v11 = proxy.createVersion(new VersionState(vb11.getVersion()));
        try {
            Assert.assertEquals("1.1", v11.getId());
            Assert.assertTrue(v11.getAttributes().isEmpty());
            Assert.assertEquals("valA", v11.getProfileState("prfA").getConfiguration("pidA").get("keyA"));
        } finally {
            proxy.deleteVersion("1.1");
        }
    }

    @Test
    public void testCreateVersionFrom() throws Exception {
        // [FABRIC-1169] Profile version attributes leak to other versions
        // VersionState v12 = proxy.createVersion("1.0", "1.2", Collections.singletonMap("keyA", "valA"));
        VersionState v12 = proxy.createVersion("1.0", "1.2", null);
        try {
            Assert.assertEquals("1.2", v12.getId());
            //Assert.assertEquals("valA", v12.getAttributes().get("keyA"));
            List<String> profiles = v12.getProfileIds();
            Assert.assertTrue(profiles.contains("default"));
            Assert.assertTrue(profiles.contains("fabric"));
        } finally {
            proxy.deleteVersion("1.2");
        }
    }

    @Test
    public void testCreateUpdateDeleteProfile() throws Exception {
        ProfileBuilder pbA10 = ProfileBuilder.Factory.create("1.0", "prfA");
        pbA10.addConfiguration("pidA", Collections.singletonMap("keyA", "valA"));
        ProfileState prfA = proxy.createProfile(new ProfileState(pbA10.getProfile()));
        try {
            Assert.assertEquals("prfA", prfA.getId());
            Assert.assertEquals("1.0", prfA.getVersion());
            Assert.assertTrue(prfA.getAttributes().isEmpty());
            Assert.assertEquals("valA", prfA.getConfiguration("pidA").get("keyA"));
            
            // getProfile
            Assert.assertEquals(prfA, proxy.getProfile("1.0", "prfA"));

            // updateProfile
            prfA = proxy.getProfile("1.0", "prfA");
            pbA10 = ProfileBuilder.Factory.createFrom(prfA.toProfile());
            pbA10.addConfiguration("pidB", "keyB", "valB");
            prfA = proxy.updateProfile(new ProfileState(pbA10.getProfile()));
            Assert.assertEquals("prfA", prfA.getId());
            Assert.assertEquals("1.0", prfA.getVersion());
            Assert.assertTrue(prfA.getAttributes().isEmpty());
            Assert.assertEquals("valA", prfA.getConfiguration("pidA").get("keyA"));
            Assert.assertEquals("valB", prfA.getConfiguration("pidB").get("keyB"));
        } finally {
            proxy.deleteProfile("1.0", "prfA", false);
        }
    }
}

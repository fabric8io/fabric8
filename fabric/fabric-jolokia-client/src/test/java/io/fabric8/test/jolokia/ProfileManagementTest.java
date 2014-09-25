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
package io.fabric8.test.jolokia;

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;
import io.fabric8.api.mxbean.ProfileManagement;
import io.fabric8.api.mxbean.ProfileState;
import io.fabric8.api.mxbean.VersionState;
import io.fabric8.jolokia.client.OpenTypeGenerator;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.JMX;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenMBeanOperationInfo;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test {@link ProfileManagement} jolokia proxy
 *
 * @since 15-Sep-2014
 */
public class ProfileManagementTest {

    static MBeanServer server;
    static ProfileManagement proxy;
    static Version version10;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        
        Profile prfA = ProfileBuilder.Factory.create("1.0", "prfA").getProfile();
        Profile prfB = ProfileBuilder.Factory.create("1.0", "prfB").addParent("prfA").getProfile();
        version10 = VersionBuilder.Factory.create("1.0").addProfiles(Arrays.asList(prfA, prfB)).getVersion();

        ProfileManagement impl = Mockito.mock(ProfileManagement.class);
        Mockito.when(impl.getVersions()).thenReturn(Arrays.asList("1.0"));
        Mockito.when(impl.getVersion("1.0")).thenReturn(new VersionState(version10));
        Mockito.when(impl.getProfile("1.0", "prfA")).thenReturn(new ProfileState(prfA));
        Mockito.when(impl.getProfile("1.0", "prfB")).thenReturn(new ProfileState(prfB));
        
        server = ManagementFactory.getPlatformMBeanServer();
        StandardMBean mxbean = new StandardMBean(impl, ProfileManagement.class, true);
        server.registerMBean(mxbean, ProfileManagement.OBJECT_NAME);
        proxy = JMX.newMXBeanProxy(server, ProfileManagement.OBJECT_NAME, ProfileManagement.class);
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        server.unregisterMBean(ProfileManagement.OBJECT_NAME);
    }
    
    @Test
    public void testProxyGetVersions() throws Exception {
        List<String> versions = proxy.getVersions();
        Assert.assertEquals(1, versions.size());
        Assert.assertEquals("1.0", versions.get(0));
    }

    @Test
    public void testProxyGetVersion() throws Exception {
        VersionState ver10 = proxy.getVersion("1.0");
        List<String> profiles = ver10.getProfiles();
        Assert.assertEquals(2, profiles.size());
        Assert.assertEquals("prfA", ver10.getProfileState("prfA").getId());
        Assert.assertEquals("prfB", ver10.getProfileState("prfB").getId());
    }

    @Test
    public void testPlainGetProfile() throws Exception {
        Object[] params = new Object[] {"1.0", "prfB"};
        String[] signature = new String[]{ String.class.getName(), String.class.getName()};
        CompositeData cdata = (CompositeData) server.invoke(ProfileManagement.OBJECT_NAME, "getProfile", params, signature);
        ProfileState prfB = (ProfileState) OpenTypeGenerator.fromOpenData(cdata.getCompositeType(), ProfileState.class.getClassLoader(), cdata);
        Assert.assertEquals(version10.getProfile("prfB"), prfB.toProfile());
    }

    @Test
    public void testProfileStateRoundTrip() throws Exception {
        CompositeType ctype = getProfileStateType();
        ProfileState prfB = new ProfileState(version10.getProfile("prfB"));
        ClassLoader classLoader = prfB.getClass().getClassLoader();
        CompositeData cdata = (CompositeData) OpenTypeGenerator.toOpenData(ctype, prfB);
        ProfileState result = (ProfileState) OpenTypeGenerator.fromOpenData(ctype, classLoader, cdata);
        Assert.assertEquals(prfB, result);
    }

    @Test
    public void testVersionStateRoundTrip() throws Exception {
        CompositeType ctype = getVersionStateType();
        VersionState ver10 = new VersionState(version10);
        ClassLoader classLoader = ver10.getClass().getClassLoader();
        CompositeData cdata = (CompositeData) OpenTypeGenerator.toOpenData(ctype, ver10);
        VersionState result = (VersionState) OpenTypeGenerator.fromOpenData(ctype, classLoader, cdata);
        Assert.assertEquals(ver10, result);
    }

    private CompositeType getProfileStateType() throws Exception {
        MBeanInfo info = server.getMBeanInfo(ProfileManagement.OBJECT_NAME);
        Map<String, MBeanOperationInfo> opsinfo = new HashMap<>();
        for (MBeanOperationInfo op : info.getOperations()) {
            opsinfo.put(op.getName(), op);
        }
        OpenMBeanOperationInfo opinfo = (OpenMBeanOperationInfo) opsinfo.get("getProfile");
        return (CompositeType) opinfo.getReturnOpenType();
    }
    
    private CompositeType getVersionStateType() throws Exception {
        MBeanInfo info = server.getMBeanInfo(ProfileManagement.OBJECT_NAME);
        Map<String, MBeanOperationInfo> opsinfo = new HashMap<>();
        for (MBeanOperationInfo op : info.getOperations()) {
            opsinfo.put(op.getName(), op);
        }
        OpenMBeanOperationInfo opinfo = (OpenMBeanOperationInfo) opsinfo.get("getVersion");
        return (CompositeType) opinfo.getReturnOpenType();
    }
}

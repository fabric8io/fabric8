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
import io.fabric8.api.mxbean.ProfileManagement;
import io.fabric8.api.mxbean.ProfileState;
import io.fabric8.api.mxbean.VersionState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test client side {@link ProfileManagement} test
 *
 * @since 15-Sep-2014
 */
public abstract class AbstractProfileManagementTest {

    abstract ProfileManagement getProxy();
    
    @Test
    public void testGetVersions() throws Exception {
        List<String> versions = new ArrayList<>(getProxy().getVersions());
        Assert.assertTrue("Contains 1.0 - " + versions, versions.contains("1.0"));
    }

    @Test
    public void testGetVersion() throws Exception {
        VersionState v10 = getProxy().getVersion("1.0");
        Assert.assertEquals("1.0", v10.getId());
        Assert.assertTrue("Expected empty, but was" + v10.getAttributes(), v10.getAttributes().isEmpty());
        List<String> profiles = v10.getProfiles();
        Assert.assertTrue(profiles.contains("default"));
        Assert.assertTrue(profiles.contains("fabric"));
    }

    @Test
    public void testCreateVersion() throws Exception {
        ProfileBuilder pbA11 = ProfileBuilder.Factory.create("1.1", "prfA");
        Profile prfA = pbA11.addConfiguration("pidA", Collections.singletonMap("keyA", "valA")).getProfile();
        VersionBuilder vb11 = VersionBuilder.Factory.create("1.1").addProfile(prfA);
        VersionState v11 = getProxy().createVersion(new VersionState(vb11.getVersion()));
        try {
            Assert.assertEquals("1.1", v11.getId());
            Assert.assertTrue(v11.getAttributes().isEmpty());
            Assert.assertEquals("valA", v11.getProfileState("prfA").getConfiguration("pidA").get("keyA"));
        } finally {
            getProxy().deleteVersion("1.1");
        }
    }

    @Test
    public void testCreateVersionFrom() throws Exception {
        // [FABRIC-1169] Profile version attributes leak to other versions
        // VersionState v12 = getProxy().createVersion("1.0", "1.2", Collections.singletonMap("keyA", "valA"));
        VersionState v12 = getProxy().createVersionFrom("1.0", "1.2", null);
        try {
            Assert.assertEquals("1.2", v12.getId());
            //Assert.assertEquals("valA", v12.getAttributes().get("keyA"));
            List<String> profiles = v12.getProfiles();
            Assert.assertTrue(profiles.contains("default"));
            Assert.assertTrue(profiles.contains("fabric"));
        } finally {
            getProxy().deleteVersion("1.2");
        }
    }

    @Test
    public void testCreateUpdateDeleteProfile() throws Exception {
        ProfileBuilder pbA10 = ProfileBuilder.Factory.create("1.0", "prfA");
        pbA10.addConfiguration("pidA", Collections.singletonMap("keyA", "valA"));
        ProfileState prfA = getProxy().createProfile(new ProfileState(pbA10.getProfile()));
        try {
            Assert.assertEquals("prfA", prfA.getId());
            Assert.assertEquals("1.0", prfA.getVersion());
            Assert.assertTrue(prfA.getAttributes().isEmpty());
            Assert.assertEquals("valA", prfA.getConfiguration("pidA").get("keyA"));
            
            // getProfile
            Assert.assertEquals(prfA, getProxy().getProfile("1.0", "prfA"));

            // updateProfile
            prfA = getProxy().getProfile("1.0", "prfA");
            pbA10 = ProfileBuilder.Factory.createFrom(prfA.toProfile());
            pbA10.addConfiguration("pidB", "keyB", "valB");
            prfA = getProxy().updateProfile(new ProfileState(pbA10.getProfile()));
            Assert.assertEquals("prfA", prfA.getId());
            Assert.assertEquals("1.0", prfA.getVersion());
            Assert.assertTrue(prfA.getAttributes().isEmpty());
            Assert.assertEquals("valA", prfA.getConfiguration("pidA").get("keyA"));
            Assert.assertEquals("valB", prfA.getConfiguration("pidB").get("keyB"));
        } finally {
            getProxy().deleteProfile("1.0", "prfA", false);
        }
    }
}

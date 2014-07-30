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
package io.fabric8.itests.smoke.embedded;

import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.CreateEnsembleOptions.Builder;
import io.fabric8.api.LockHandle;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileRegistry;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;
import io.fabric8.api.ZooKeeperClusterBootstrap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the {@link ProfileService}
 */
@RunWith(Arquillian.class)
public class ProfileRegistryTest {

    private ProfileRegistry profileRegistry;
    
    @BeforeClass
    public static void beforeClass() {
        Builder<?> builder = CreateEnsembleOptions.builder().agentEnabled(false).clean(true).waitForProvision(false);
        ServiceLocator.getRequiredService(ZooKeeperClusterBootstrap.class).create(builder.build());
    }

    @Before
    public void setUp() {
        profileRegistry = ServiceLocator.getRequiredService(ProfileRegistry.class);
        Assert.assertNotNull("ProfileRegistry not null", profileRegistry);
    }
    
    @Test
    public void createVersionFrom() {
        
        Version defaultVersion = profileRegistry.getRequiredVersion("1.0");
        Assert.assertNotNull(defaultVersion);
        
        Assert.assertFalse(profileRegistry.hasVersion("1.1"));
        Assert.assertNull(profileRegistry.getVersion("1.1"));
        
        String versionId = profileRegistry.createVersion("1.0", "1.1", Collections.singletonMap("foo", "bar"));
        Assert.assertTrue(profileRegistry.hasVersion(versionId));
        Assert.assertEquals("1.1", versionId);
        
        // Version cannot get created twice
        try {
            profileRegistry.createVersion("1.0", "1.1", null);
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage().contains("Version already exists: 1.1"));
        }
        
        Version version = profileRegistry.getRequiredVersion(versionId);
        Assert.assertEquals("bar", version.getAttributes().get("foo"));
        Assert.assertEquals(defaultVersion.getProfileIds(), version.getProfileIds());
        
        // Delete the version again
        profileRegistry.deleteVersion(versionId);

        Assert.assertFalse(profileRegistry.hasVersion("1.1"));
        Assert.assertNull(profileRegistry.getVersion("1.1"));
    }

    @Test
    public void createVersion() {
        
        Version defaultVersion = profileRegistry.getRequiredVersion("1.0");
        Assert.assertNotNull(defaultVersion);
        
        // Version already exists
        VersionBuilder vbuilder = VersionBuilder.Factory.create("1.0");
        try {
            profileRegistry.createVersion(vbuilder.getVersion());
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("Version already exists: 1.0"));
        }
        
        Assert.assertFalse(profileRegistry.hasVersion("1.1"));
        Assert.assertNull(profileRegistry.getVersion("1.1"));
        
        vbuilder = VersionBuilder.Factory.create("1.1").addAttribute("foo", "bar");
        vbuilder.addProfile(ProfileBuilder.Factory.create("1.1", "prfA").getProfile());
        String versionId = profileRegistry.createVersion(vbuilder.getVersion());
        Assert.assertTrue(profileRegistry.hasVersion(versionId));
        Assert.assertEquals("1.1", versionId);
        
        Version version = profileRegistry.getRequiredVersion(versionId);
        Assert.assertEquals("bar", version.getAttributes().get("foo"));
        List<Profile> profiles = version.getProfiles();
        Assert.assertEquals(3, profiles.size());
        Assert.assertEquals("fabric-ensemble-0000", profiles.get(0).getId());
        Assert.assertEquals("fabric-ensemble-0000-1", profiles.get(1).getId());
        Assert.assertEquals("prfA", profiles.get(2).getId());
        
        // Delete the version again
        profileRegistry.deleteVersion(versionId);

        Assert.assertFalse(profileRegistry.hasVersion("1.1"));
        Assert.assertNull(profileRegistry.getVersion("1.1"));
    }

    @Test
    public void getVersions() {
        List<String> versionIds = profileRegistry.getVersionIds();
        Assert.assertEquals(1, versionIds.size());
        
        VersionBuilder vbuilder = VersionBuilder.Factory.create("1.1");
        String versionId = profileRegistry.createVersion(vbuilder.getVersion());
        
        versionIds = profileRegistry.getVersionIds();
        Assert.assertEquals(2, versionIds.size());
        Assert.assertEquals("1.0", versionIds.get(0));
        Assert.assertEquals("1.1", versionIds.get(1));
        
        // Delete the version again
        profileRegistry.deleteVersion(versionId);

        versionIds = profileRegistry.getVersionIds();
        Assert.assertEquals(1, versionIds.size());
    }

    @Test
    public void hasVersion() {
        
        Assert.assertTrue(profileRegistry.hasVersion("1.0"));
        Assert.assertFalse(profileRegistry.hasVersion("1.1"));
        
        VersionBuilder vbuilder = VersionBuilder.Factory.create("1.1");
        String versionId = profileRegistry.createVersion(vbuilder.getVersion());
        
        Assert.assertTrue(profileRegistry.hasVersion("1.0"));
        Assert.assertTrue(profileRegistry.hasVersion("1.1"));
        
        // Delete the version again
        profileRegistry.deleteVersion(versionId);

        Assert.assertTrue(profileRegistry.hasVersion("1.0"));
        Assert.assertFalse(profileRegistry.hasVersion("1.1"));
    }

    @Test
    public void getVersion() {
        
        Assert.assertNotNull(profileRegistry.getVersion("1.0"));
        Assert.assertNull(profileRegistry.getVersion("1.1"));
        
        VersionBuilder vbuilder = VersionBuilder.Factory.create("1.1");
        String versionId = profileRegistry.createVersion(vbuilder.getVersion());
        
        Assert.assertNotNull(profileRegistry.getVersion("1.0"));
        Assert.assertNotNull(profileRegistry.getVersion("1.1"));
        
        // Delete the version again
        profileRegistry.deleteVersion(versionId);

        Assert.assertNotNull(profileRegistry.getVersion("1.0"));
        Assert.assertNull(profileRegistry.getVersion("1.1"));
    }

    @Test
    public void getRequiredVersion() {
        
        Assert.assertNotNull(profileRegistry.getRequiredVersion("1.0"));
        try {
            Assert.assertNotNull(profileRegistry.getRequiredVersion("1.1"));
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("Version does not exist: 1.1"));
        }
    }

    @Test
    public void createProfile() {
        
        String versionId = "1.0";
        Assert.assertFalse(profileRegistry.hasProfile(versionId, "prfA"));
        
        ProfileBuilder pbuilder = ProfileBuilder.Factory.create(versionId, "prfA");
        String profileId = profileRegistry.createProfile(pbuilder.getProfile());
        
        // Profile already exists
        try {
            profileRegistry.createProfile(pbuilder.getProfile());
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("Profile already exists: prfA"));
        }
        
        Profile prfA = profileRegistry.getRequiredProfile(versionId, profileId);
        Assert.assertNotNull(prfA);
        
        // Delete the profile again
        profileRegistry.deleteProfile(versionId, profileId);

        Assert.assertFalse(profileRegistry.hasProfile(versionId, "prfA"));
    }

    @Test
    public void updateProfile() {
        
        String versionId = "1.0";
        Assert.assertFalse(profileRegistry.hasProfile(versionId, "prfA"));

        // Profile does not exist
        ProfileBuilder pbuilder = ProfileBuilder.Factory.create(versionId, "prfA");
        try {
            profileRegistry.updateProfile(pbuilder.getProfile());
            Assert.fail("IllegalStateException expected");
        } catch (IllegalStateException ex) {
            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("Profile does not exist: 1.0/prfA"));
        }
        
        String profileId = profileRegistry.createProfile(pbuilder.getProfile());
        Profile prfA = profileRegistry.getRequiredProfile(versionId, profileId);
        Assert.assertTrue(prfA.getAttributes().isEmpty());
        
        pbuilder = ProfileBuilder.Factory.createFrom(prfA).addAttribute("foo", "bar");
        profileId = profileRegistry.updateProfile(pbuilder.getProfile());
        prfA = profileRegistry.getRequiredProfile(versionId, profileId);
        Assert.assertEquals("bar", prfA.getAttributes().get("foo"));
        
        // Delete the profile again
        profileRegistry.deleteProfile(versionId, profileId);

        Assert.assertFalse(profileRegistry.hasProfile(versionId, "prfA"));
    }

    @Test
    public void importProfiles() {
        // [TODO] Test ProfileRegistry.importProfiles()
    }

    @Test
    public void importFromFileSystem() {
        // [TODO] Test ProfileRegistry.importFromFileSystem()
    }

    @Test
    public void exportProfiles() {
        // [TODO] Test ProfileRegistry.exportProfiles()
    }
}

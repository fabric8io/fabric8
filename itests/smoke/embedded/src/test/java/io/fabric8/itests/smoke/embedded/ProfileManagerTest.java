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

import io.fabric8.api.BootstrapComplete;
import io.fabric8.api.CreateEnsembleOptions;
import io.fabric8.api.CreateEnsembleOptions.Builder;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileManager;
import io.fabric8.api.ProfileManagerLocator;
import io.fabric8.api.Version;
import io.fabric8.api.ZooKeeperClusterBootstrap;

import java.util.Collections;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the {@link ProfileManager}
 */
@RunWith(Arquillian.class)
public class ProfileManagerTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServiceLocator.awaitService(BootstrapComplete.class);
        Builder<?> builder = CreateEnsembleOptions.builder().agentEnabled(false).clean(true).waitForProvision(false);
        ServiceLocator.getRequiredService(ZooKeeperClusterBootstrap.class).create(builder.build());
    }
    
    @Test
    public void testProfileManager() throws Exception {
        
        ProfileManager profileManager = ProfileManagerLocator.getProfileManager();
        
        // fabric:profile-create prfA
        ProfileBuilder pbA10 = ProfileBuilder.Factory.create("1.0", "prfA")
                .addConfiguration("pidA", Collections.singletonMap("keyA", "valA"));
        Profile prfA10 = profileManager.createProfile(pbA10.getProfile());
        Assert.assertEquals("1.0", prfA10.getVersion());
        Assert.assertEquals("prfA", prfA10.getId());
        Assert.assertEquals("valA", prfA10.getConfiguration("pidA").get("keyA"));
        
        // Verify access to original profile
        profileManager.getRequiredVersion("1.0").getRequiredProfile("prfA");
        
        // fabric:version-create --parent 1.0 1.1
        Version v11 = profileManager.createVersionFrom("1.0", "1.1", null);
        Profile prfA11a = v11.getRequiredProfile("prfA");
        Assert.assertEquals("1.1", prfA11a.getVersion());
        Assert.assertEquals("prfA", prfA11a.getId());
        Assert.assertEquals("valA", prfA11a.getConfiguration("pidA").get("keyA"));
        
        // Verify access to original profile
        profileManager.getRequiredVersion("1.0").getRequiredProfile("prfA");
        profileManager.getRequiredVersion("1.1").getRequiredProfile("prfA");
        
        ProfileBuilder pbA11 = ProfileBuilder.Factory.createFrom(prfA11a)
                .addConfiguration("pidA", Collections.singletonMap("keyB", "valB"));
        Profile prfA11b = profileManager.updateProfile(pbA11.getProfile());
        Assert.assertEquals("1.1", prfA11b.getVersion());
        Assert.assertEquals("prfA", prfA11b.getId());
        Assert.assertEquals("valB", prfA11b.getConfiguration("pidA").get("keyB"));
        
        Assert.assertNotEquals(prfA11a, prfA11b);
        // System.out.println(Profiles.getProfileDifference(prfA11a, prfA11b));
        
        // Verify access to original profile
        profileManager.getRequiredVersion("1.0").getRequiredProfile("prfA");
        profileManager.getRequiredVersion("1.1").getRequiredProfile("prfA");
        
        // Delete the profile/version that were added
        profileManager.deleteProfile(prfA10.getVersion(), prfA10.getId(), true);
        profileManager.deleteVersion(v11.getId());
    }
}

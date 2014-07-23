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
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;
import io.fabric8.api.ZooKeeperClusterBootstrap;

import java.util.Collections;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.runtime.ServiceLocator;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the {@link ProfileService}
 */
@RunWith(Arquillian.class)
public class ProfileServiceTest {

    private static final String SYSTEM_PASSWORD = "systempassword";

    @Test
    public void testFabricCreate() throws Exception {

        Builder<?> builder = CreateEnsembleOptions.builder().agentEnabled(false).clean(true).zookeeperPassword(SYSTEM_PASSWORD).waitForProvision(false);
        CreateEnsembleOptions options = builder.build();

        ZooKeeperClusterBootstrap bootstrap = ServiceLocator.getRequiredService(ZooKeeperClusterBootstrap.class);
        bootstrap.create(options);

        ProfileService profileService = ServiceLocator.getRequiredService(ProfileService.class);

        // fabric:profile-create prfA
        Profile prfA10 = ProfileBuilder.Factory.create("1.0", "prfA")
                .addConfiguration("pidA", Collections.singletonMap("keyA", "valA"))
                .getProfile();
        prfA10 = profileService.createProfile(prfA10);
        Assert.assertEquals("1.0", prfA10.getVersion());
        Assert.assertEquals("prfA", prfA10.getId());
        Assert.assertEquals("valA", prfA10.getConfiguration("pidA").get("keyA"));
        
        // Verify access to original profile
        profileService.getRequiredVersion("1.0").getRequiredProfile("prfA");
        
        // fabric:version-create --parent 1.0 1.1
        Version v11 = profileService.createVersion("1.0", "1.1", null);
        Profile prfA11 = v11.getRequiredProfile("prfA");
        Assert.assertEquals("1.1", prfA11.getVersion());
        Assert.assertEquals("prfA", prfA11.getId());
        Assert.assertEquals("valA", prfA11.getConfiguration("pidA").get("keyA"));
        
        // Verify access to original profile
        profileService.getRequiredVersion("1.0").getRequiredProfile("prfA");
        profileService.getRequiredVersion("1.1").getRequiredProfile("prfA");
        
        prfA11 = ProfileBuilder.Factory.createFrom(prfA11)
                .addConfiguration("pidA", Collections.singletonMap("keyB", "valB"))
                .getProfile();
        prfA11 = profileService.updateProfile(prfA11);
        Assert.assertEquals("1.1", prfA11.getVersion());
        Assert.assertEquals("prfA", prfA11.getId());
        Assert.assertEquals("valB", prfA11.getConfiguration("pidA").get("keyB"));
        
        // Verify access to original profile
        profileService.getRequiredVersion("1.0").getRequiredProfile("prfA");
        profileService.getRequiredVersion("1.1").getRequiredProfile("prfA");
        
        // Delete the profile/version that were added
        // [FIXME] Cannot delete profile/version 
        //profileService.deleteProfile(prfA10.getVersion(), prfA10.getId());
        //profileService.deleteVersion(v11.getId());
    }
}

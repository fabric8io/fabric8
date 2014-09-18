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
package io.fabric8.internal;

import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileBuilders;
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;
import io.fabric8.api.scr.AbstractComponent;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;

/**
 * A provider service for the {@link ProfileBuilders}
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
@Component(policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service(ProfileBuilders.class)
public final class ProfileBuildersImpl extends AbstractComponent implements ProfileBuilders {

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }
    
    @Override
    public VersionBuilder profileVersionBuilder() {
    	assertValid();
        return new DefaultVersionBuilder((String) null);
    }

    @Override
    public VersionBuilder profileVersionBuilder(String versionId) {
    	assertValid();
        return new DefaultVersionBuilder(versionId);
    }

    @Override
    public ProfileBuilder profileBuilder() {
    	assertValid();
        return new DefaultProfileBuilder(null, null);
    }

    @Override
    public ProfileBuilder profileBuilder(String profileId) {
        assertValid();
        return new DefaultProfileBuilder(null, profileId);
    }

    @Override
    public ProfileBuilder profileBuilder(String versionId, String profileId) {
        assertValid();
        return new DefaultProfileBuilder(versionId, profileId);
    }

    @Override
    public ProfileBuilder profileBuilderFrom(Profile profile) {
    	assertValid();
        return new DefaultProfileBuilder(profile);
    }
    
    @Override
    public VersionBuilder profileVersionBuilderFrom(Version version) {
    	assertValid();
        return new DefaultVersionBuilder(version);
    }
}

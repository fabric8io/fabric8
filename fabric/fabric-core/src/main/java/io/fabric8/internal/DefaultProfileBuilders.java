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

/**
 * A provider service for the {@link ProfileBuilders}
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
public final class DefaultProfileBuilders implements ProfileBuilders {

    @Override
    public VersionBuilder profileVersionBuilder() {
        return new DefaultVersionBuilder();
    }

    @Override
    public VersionBuilder profileVersionBuilder(String versionId) {
        return new DefaultVersionBuilder().identity(versionId);
    }

    @Override
    public VersionBuilder profileVersionBuilderFrom(Version version) {
        return new DefaultVersionBuilder().from(version);
    }
    
    @Override
    public ProfileBuilder profileBuilder() {
        return new DefaultProfileBuilder();
    }

    @Override
    public ProfileBuilder profileBuilder(String profileId) {
        return new DefaultProfileBuilder().identity(profileId);
    }

    @Override
    public ProfileBuilder profileBuilder(String versionId, String profileId) {
        return new DefaultProfileBuilder().version(versionId).identity(profileId);
    }

    @Override
    public ProfileBuilder profileBuilderFrom(Profile profile) {
        return new DefaultProfileBuilder().from(profile);
    }
}

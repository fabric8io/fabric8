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
package io.fabric8.api;

import org.osgi.framework.BundleReference;

import io.fabric8.api.gravia.ServiceLocator;

/**
 * A profile builder factory
 * 
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileBuilders {

	VersionBuilder profileVersionBuilder();

	VersionBuilder profileVersionBuilder(String versionId);

	ProfileBuilder profileBuilder();

	ProfileBuilder profileBuilder(String profileId);

    ProfileBuilder profileBuilder(String versionId, String profileId);
    
	ProfileBuilder profileBuilderFrom(Profile profile);

	VersionBuilder profileVersionBuilderFrom(Version version);

	// ConfigurationItemBuilder configurationItemBuilder();

	// ConfigurationItemBuilder configurationItemBuilder(String identity);

    final class Factory {

        public static ProfileBuilders getProfileBuilders() {
            ProfileBuilders builders;
            ClassLoader classLoader = ProfileBuilders.class.getClassLoader();
            if (classLoader instanceof BundleReference) {
                builders = ServiceLocator.getRequiredService(ProfileBuilders.class);
            } else {
                try {
                    builders = (ProfileBuilders) classLoader.loadClass("io.fabric8.internal.DefaultProfileBuilders").newInstance();
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            }
            return builders;
        }

        // Hide ctor
        private Factory() {
        }
    }
}

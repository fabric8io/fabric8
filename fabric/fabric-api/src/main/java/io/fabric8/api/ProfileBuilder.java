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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.gravia.runtime.ServiceLocator;


/**
 * A profile builder.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Mar-2014
 */
public interface ProfileBuilder extends AttributableBuilder<ProfileBuilder> {

    ProfileBuilder identity(String profileId);

    ProfileBuilder version(String versionId);

    List<String> getParents();
    
    Profile getParent(String profileId);
    
    ProfileBuilder addParent(Profile profile);

    ProfileBuilder addParents(List<Profile> profiles);

    ProfileBuilder setParents(List<Profile> profiles);
    
    ProfileBuilder removeParent(String profileId);

    Set<String> getConfigurationKeys();
    
    Map<String, String> getConfiguration(String pid);
    
    ProfileBuilder addConfiguration(String pid, Map<String, String> config);

    ProfileBuilder addConfiguration(String pid, String key, String value);

    ProfileBuilder setConfigurations(Map<String, Map<String, String>> configs);

    ProfileBuilder deleteConfiguration(String pid);

    Set<String> getFileConfigurationKeys();
    
    byte[] getFileConfiguration(String key);
    
    ProfileBuilder addFileConfiguration(String fileName, byte[] data);
    
    ProfileBuilder setFileConfigurations(Map<String, byte[]> configs);

    ProfileBuilder deleteFileConfiguration(String fileName);
    
    ProfileBuilder setBundles(List<String> values);

    ProfileBuilder setFabs(List<String> values);

    ProfileBuilder setFeatures(List<String> values);

    ProfileBuilder setRepositories(List<String> values);

    ProfileBuilder setOverrides(List<String> values);
    
    ProfileBuilder setOptionals(List<String> values);
    
    ProfileBuilder setTags(List<String> values);
    
    ProfileBuilder setOverlay(boolean overlay);
    
    ProfileBuilder setLocked(boolean flag);

    ProfileBuilder setLastModified(String lastModified);
    
    Profile getProfile();

    final class Factory {

        public static ProfileBuilder create() {
            ProfileBuilders factory = ServiceLocator.getRequiredService(ProfileBuilders.class);
            return factory.profileBuilder();
        }

        public static ProfileBuilder create(String profileId) {
            ProfileBuilders factory = ServiceLocator.getRequiredService(ProfileBuilders.class);
            return factory.profileBuilder(profileId);
        }

        public static ProfileBuilder create(String versionId, String profileId) {
            ProfileBuilders factory = ServiceLocator.getRequiredService(ProfileBuilders.class);
            return factory.profileBuilder(versionId, profileId);
        }

        public static ProfileBuilder createFrom(Profile profile) {
            ProfileBuilders factory = ServiceLocator.getRequiredService(ProfileBuilders.class);
            return factory.profileBuilderFrom(profile);
        }

        // Hide ctor
        private Factory() {
        }
    }
}
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

import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * The profile data store
 */
public interface ProfileRegistry {

    Map<String, String> getDataStoreProperties();
    
    //
    // Version management
    //
    
    void createVersion(String version);

    void createVersion(String parentVersionId, String toVersion);

    boolean hasVersion(String name);
    
    Map<String, String> getVersionAttributes(String version);
    
    void setVersionAttribute(String version, String key, String value);
    
    /**
     * Get the ordered list of available versions
     */
    List<String> getVersions();

    void deleteVersion(String version);
    
    //
    // Profile management
    //
    
    void createProfile(String version, String profile);

    boolean hasProfile(String version, String profile);

    String getLastModified(String version, String profile);

    List<String> getProfiles(String version);

    Map<String, String> getProfileAttributes(String version, String profile);

    void setProfileAttribute(String version, String profile, String key, String value);

    Map<String, byte[]> getFileConfigurations(String version, String profile);

    void setFileConfigurations(String version, String profile, Map<String, byte[]> configurations);

    Map<String, Map<String, String>> getConfigurations(String version, String profile);

    void setConfigurations(String version, String profile, Map<String, Map<String, String>> configurations);

    void deleteProfile(String version, String profile);

    //
    // [TODO] Below are methods that are accessed directly throughout the code base
    // These should go through {@link ProfileService}
    //
    
    /**
     * Imports one or more profile zips into the given version
     */
    void importProfiles(String version, List<String> profileZipUrls);

    /**
     * Exports profiles from the given version to the outputZipFileName which match the given wildcard
     */
    void exportProfiles(String version, String outputFileName, String wildcard);

    String getProfile(String version, String profile, boolean create);

    byte[] getFileConfiguration(String version, String profile, String name);

    void setFileConfiguration(String version, String profile, String name, byte[] configuration);

    Map<String, String> getConfiguration(String version, String profile, String pid);

    void setConfiguration(String version, String profile, String pid, Map<String, String> configuration);

    void importFromFileSystem(String from);

    Collection<String> listFiles(final String version, final Iterable<String> profiles, final String path);
}

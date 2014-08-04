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


/**
 * The profile data store
 */
public interface ProfileRegistry {

    /**
     * Get the data store properties
     */
    Map<String, String> getDataStoreProperties();
    
    /**
     * Aquire a write lock for the profile datastore
     */
    LockHandle aquireWriteLock();
    
    //
    // Version management
    //

    /**
     * Create a version as a copy from the given source version identity 
     */
    String createVersion(String sourceId, String targetId, Map<String, String> attributes);

    /**
     * Create the given version in the data store
     * @return The version id
     */
    String createVersion(Version version);

    /**
     * Get the ordered list of available versions
     */
    List<String> getVersionIds();

    /**
     * True if the data store contains the given version
     */
    boolean hasVersion(String versionId);

    /**
     * Get the version for the given id
     * @return The version or null
     */
    Version getVersion(String versionId);

    /**
     * Get the version for the given id
     * @throws IllegalStateException if the required version does not exist
     */
    Version getRequiredVersion(String versionId);

    /**
     * Delete the given version and all associated profiles
     */
    void deleteVersion(String versionId);
    
    //
    // Profile management
    //
    
    /**
     * Create the given profile in the data store
     * @return The profile id
     */
    String createProfile(Profile profile);
    
    /**
     * Create the given profile in the data store
     * @return The profile id
     */
    String updateProfile(Profile profile);

    /**
     * True if the given profile existes in the given version
     */
    boolean hasProfile(String versionId, String profileId);

    /**
     * Get the profile for the given version and id
     * @return The profile or null
     */
    Profile getProfile(String versionId, String profileId);

    /**
     * Get the profile for the given version and id
     * @throws IllegalStateException if the required profile does not exist
     */
    Profile getRequiredProfile(String versionId, String profileId);

    /** 
     * Get the list of profiles associated with the given version  
     */
    List<String> getProfiles(String versionId);

    /**
     * Delete the given profile from the data store
     */
    void deleteProfile(String versionId, String profileId);

    //
    // Import/Export 
    // [TODO] Consider utility methods for import/export that go through ProfileService 
    //
    
    void importProfiles(String versionId, List<String> profileZipUrls);

    void importFromFileSystem(String importPath);

    void exportProfiles(String versionId, String outputFileName, String wildcard);
}

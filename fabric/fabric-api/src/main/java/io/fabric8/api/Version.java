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
 * The immutable view of a profile version
 */
public interface Version extends Comparable<Version>, HasId {

    /**
     * The attribute key for the description of the version
     */
    String DESCRIPTION = "description";

    /**
     * The attribute key for the locked flag
     */
    String LOCKED = "locked";

    /**
     * @return Returns the revision of the Version.
     */
    public String getRevision();

    /**
     * Get the version attributes
     */
    Map<String, String> getAttributes();
    
    /**
     * Get the list of available profile identities
     */
    List<String> getProfileIds();
    
    /**
     * Get the list of available profiles
     */
    List<Profile> getProfiles();

    /**
     * Get a profile for the given identity.
     * @return null if there is no profile for the given identity
     */
    Profile getProfile(String profileId);
    
    /**
     * Get a profile for the given identity.
     * @throws IllegalStateException if there is no profile for the given identity
     */
    Profile getRequiredProfile(String profileId);

    /**
     * True if the version contains a profile for the given identity.
     */
    boolean hasProfile(String profileId);
}

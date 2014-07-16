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

/**
 * The immutable view of a profile
 */
public interface Profile extends Comparable<Profile>, HasId {

    /**
     * The attribute key for the list of parents
     */
    String PARENTS = "parents";

    /**
     * The attribute key for the description of the profile
     */
    String DESCRIPTION = "description";

    /**
     * The attribute key for the locked flag
     */
    String LOCKED = "locked";

    /**
     * The attribute key for the abstract flag
     */
    String ABSTRACT = "abstract";

    /**
     * The attribute key for the hidden flag
     */
    String HIDDEN = "hidden";

    String HASH = "hash";

    /**
     * Key indicating a deletion.
     * This value can appear as the value of a key in a configuration
     * or as a key itself.  If used as a key, the whole configuration
     * is flagged has been deleted from its parent when computing the
     * overlay.
     */
    String DELETED = "#deleted#";

    String getVersion();

    Map<String, String> getAttributes();

    List<Profile> getParents();
    
    List<String> getLibraries();
    List<String> getEndorsedLibraries();
    List<String> getExtensionLibraries();
    List<String> getBundles();
    List<String> getFabs();
    List<String> getFeatures();
    List<String> getRepositories();
    List<String> getOverrides();
    List<String> getOptionals();

    /**
     * Returns the URL of the profile's icon, relative to the Fabric REST API or null if no icon could be found
     */
    String getIconURL();

    /**
     * Returns the summary markdown text of the profile or null if none could be found. Typically returns the "Summary.md" file contents as a String
     */
    String getSummaryMarkdown();


    /**
     * Returns the list of tags that are applied to this profile. If none is configured then it defaults to the parent folders of the profile ID.
     */
    List<String> getTags();

    /**
     * Get the configuration file names that are available on this profile
     */
    Set<String> getConfigurationFileNames();

    Map<String, byte[]> getFileConfigurations();

    /**
     * Get the configuration file for the given name
     */
    byte[] getFileConfiguration(String fileName);

    /**
     * Get all configuration properties
     */
    Map<String, Map<String, String>> getConfigurations();

    /**
     * Get the configuration properties for the given PID
     * @return an empty map if the there is no configuration for the given pid
     */
    Map<String, String> getConfiguration(String pid);

    /**
     * Indicate if this profile is an overlay or not.
     */
    boolean isOverlay();

    /**
     * Returns true if this profile is Abstract. 
     * Abstract profiles should not be provisioned by default, they are intended to be inherited
     */
    boolean isAbstract();

    /**
     * Returns true if this profile is locked. 
     * Locked profiles can't be modified.
     */
    boolean isLocked();

    /**
     * Returns true if this profile is hidden.  
     * Hidden profiles are not listed by default.
     */
    boolean isHidden();

    String getProfileHash();
}

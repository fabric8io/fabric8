/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.api;

import java.util.List;
import java.util.Map;

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

    /**
     * Returns a read only map of all the attributes of this profile
     * @return
     */
    Map<String, String> getAttributes();

    /**
     * Change an attribute on this version.
     * @param key the name of the attribute
     * @param value the new value or <code>null</code> to delete the attribute
     */
    void setAttribute(String key, String value);

    Profile[] getParents();
    void setParents(Profile[] parents);

    Container[] getAssociatedContainers();

    List<String> getLibraries();
    List<String> getEndorsedLibraries();
    List<String> getExtensionLibraries();
    List<String> getBundles();
    List<String> getFabs();
    List<String> getFeatures();
    List<String> getRepositories();
    List<String> getOverrides();

    /**
     * Returns the configuration file names that are available on this profile
     */
    List<String> getConfigurationFileNames();

    Map<String, byte[]> getFileConfigurations();

    /**
     * Returns the configuration file for the given name
     */
    byte[] getFileConfiguration(String fileName);

    /**
     * Update configurations of this profile with the new values
     *
     * @param configurations
     */
    void setFileConfigurations(Map<String, byte[]> configurations);

    /**
     * Returns all of the configuration properties
     */
    Map<String, Map<String, String>> getConfigurations();

    /**
     * Returns the configuration properties for the given PID
     */
    Map<String, String> getConfiguration(String pid);

    /**
     * Returns the configuration properties for the container
     */
    Map<String, String> getContainerConfiguration();

    /**
     * Update configurations of this profile with the new values
     *
     * @param configurations
     */
    void setConfigurations(Map<String, Map<String, String>> configurations);

    /**
     * Update configurations of this profile with the new values for the given PID
     *
     * @param configuration is the new configuration value for the given PID
     */
    void setConfiguration(String pid, Map<String, String> configuration);

    /**
     * Gets profile with configuration slitted with parents.
     *
     * @return Calculated profile or null if instance is already a calculated overlay.
     */
    Profile getOverlay();

    /**
     * Same as getOverlay() but also perform variable substitutions
     * @param substitute
     * @return
     */
    Profile getOverlay(boolean substitute);

    /**
     * Indicate if this profile is an overlay or not.
     *
     * @return
     */
    boolean isOverlay();

    void delete();

    public void delete(boolean force);

    void setBundles(List<String> values);

    void setFabs(List<String> values);

    void setFeatures(List<String> values);

    void setRepositories(List<String> values);

    void setOverrides(List<String> values);

    boolean configurationEquals(Profile other);

    /**
     * Checks if the two Profiles share the same agent configuration.
     * @param other
     * @return
     */
    boolean agentConfigurationEquals(Profile other);

    /**
     * Checks if the profile exists.
     * @return
     */
    boolean exists();

    /**
     * Manually trigger provisioning of this profile
     */
    void refresh();

    /**
     * Returns true if this profile is Abstract. Abstract profiles should not be provisioned by default,
     * they are intended to be inherited
     */
    boolean isAbstract();

    /**
     * Returns true if this profile is locked.  Locked profiles can't be modified.
     * @return
     */
    boolean isLocked();

    /**
     * Returns true if this profile is hidden.  Hidden profiles are not listed by default.
     * @return
     */
    boolean isHidden();

    String getProfileHash();
}

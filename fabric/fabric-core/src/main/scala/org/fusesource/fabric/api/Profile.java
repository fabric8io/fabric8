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
package org.fusesource.fabric.api;

import java.util.List;
import java.util.Map;

public interface Profile extends Comparable<Profile> {

    /**
     * Key indicating a deletion.
     * This value can appear as the value of a key in a configuration
     * or as a key itself.  If used as a key, the whole configuration
     * is flagged has been deleted from its parent when computing the
     * overlay.
     */
    final String DELETED = "#deleted#";

    String getId();
    String getVersion();

    Profile[] getParents();
    void setParents(Profile[] parents);

    Container[] getAssociatedContainers();

    List<String> getBundles();
    List<String> getFabs();
    List<String> getFeatures();
    List<String> getRepositories();

    Map<String, byte[]> getFileConfigurations();

    /**
     * Update configurations of this profile with the new values
     *
     * @param configurations
     */
    void setFileConfigurations(Map<String, byte[]> configurations);

    /**
     * Returns all of the configuration properties
     * @return
     */
    Map<String, Map<String, String>> getConfigurations();

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
     * Gets profile with configuration slitted with parents.
     *
     * @return Calculated profile or null if instance is already a calculated overlay.
     */
    Profile getOverlay();

    /**
     * Indicate if this profile is an overlay or not.
     *
     * @return
     */
    boolean isOverlay();

    void delete();

    void setBundles(List<String> values);

    void setFabs(List<String> values);

    void setFeatures(List<String> values);

    void setRepositories(List<String> values);

    boolean configurationEquals(Profile other);

    /**
     * Returns true if this profile is Abstract. Abstract profiles should not be provisioned by default,
     * they are intended to be inherited
     */
    boolean isAbstract();
}

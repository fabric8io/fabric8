/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

import java.util.Map;

public interface Profile {

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

    Map<String, byte[]> getFileConfigurations();

    /**
     * Update configurations of this profile with the new values
     *
     * @param configurations
     */
    void setFileConfigurations(Map<String, byte[]> configurations);

    Map<String, Map<String, String>> getConfigurations();

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

}

/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

import java.net.URI;
import java.util.Map;

public interface Profile {

    String getId();
    String getVersion();

    Profile[] getParents();
    void setParents(Profile[] parents);

    Map<String, Map<String, String>> getConfigurations();
    void setConfigurations(Map<String, Map<String, String>> configurations);

    URI[] getBundles();
    void setBundles(URI[] bundles);

    String[] getFeatures();
    void setFeatures(String[] features);

    URI[] getFeatureRepositories();
    void setFeatureRepositories(URI[] uris);

    /**
     * Gets profile with configuration slitted with parents.
     *
     * @return Calculated profile or null if instance is already a calculated overlay.
     */
    Profile getOverlay();

}

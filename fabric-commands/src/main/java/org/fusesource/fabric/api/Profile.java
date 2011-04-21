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

    String getName();

    // version is read only
    String getVersion();

    Profile[] getParents();
    void setParents(Profile[] parents);

    URI[] getBundles();
    void setBundles(URI[] bundles);

    String[] getFeatures();
    void setFeatures(String[] features);

    URI[] getFeatureRepositories();
    void setFeatureRepositories(URI[] uris);

    Map<String, Map<String, String>> getConfigurations();
    void setConfigurations(Map<String, Map<String, String>> configurations);

    Profile[] getExtensions();
    void setExtensions(Profile[] profiles);

}

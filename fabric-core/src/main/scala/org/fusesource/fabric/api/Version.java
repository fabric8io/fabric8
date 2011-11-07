/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.api;

public interface Version {

    String getName();

    Version getDerivedFrom();

    Profile[] getProfiles();

    /**
     * Gets a profile with the given name.
     * @param name name of the profile to get.
     * @return {@link Profile} with the given name. Returns <code>null</code> if it doesn't exist.
     */
    Profile getProfile(String name);

    Profile createProfile(String name);

    void delete();

}

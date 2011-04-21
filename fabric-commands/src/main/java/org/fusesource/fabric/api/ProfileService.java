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

public interface ProfileService {

    Map<String, Profile> getProfiles(String version);

    void updateProfile(String name, Profile newProfile) throws Exception;

    void removeProfile(Profile profile) throws Exception;

    Profile createProfile(String version, String name) throws Exception;


}

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

import java.util.Properties;

public interface Version extends Comparable<Version> {

    /**
     * The attribute key for the description of the version
     */
    String DESCRIPTION = "description";

    /**
     * The attribute key for the locked flag
     */
    String LOCKED = "locked";

    /**
     * Returns the name of the version
     * @return
     */
    String getName();

    /**
     * Returns a read only map of all the attributes of this version
     * @return
     */
    Properties getAttributes();

    /**
     * Change an attribute on this version.
     * @param key the name of the attribute
     * @param value the new value or <code>null</code> to delete the attribute
     */
    void setAttribute(String key, String value);

    VersionSequence getSequence();

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

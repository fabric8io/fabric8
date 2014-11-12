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

import io.fabric8.api.gravia.ServiceLocator;


/**
 * The profile manager locator
 *
 * @author thomas.diesler@jboss.com
 * @since 23-Apr-2014
 */
public final class ProfileManagerLocator {

    /**
     * Locate the profile manager
     */
    public static ProfileManager getProfileManager() {
        return ServiceLocator.getRequiredService(ProfileManager.class);
    }

    // Hide ctor
    private ProfileManagerLocator() {
    }
}

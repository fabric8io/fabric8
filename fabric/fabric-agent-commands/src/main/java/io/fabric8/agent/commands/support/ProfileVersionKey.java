/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.commands.support;

import io.fabric8.api.Profile;

/**
 */
public class ProfileVersionKey {
    private final Profile profile;
    private final String profileId;
    private final String version;

    public ProfileVersionKey(Profile profile) {
        this.profile = profile;
        this.profileId = profile.getId();
        this.version = profile.getVersion();
    }

    @Override
    public String toString() {
        return "ProfileVersionKey{" +
                "profileId='" + profileId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProfileVersionKey that = (ProfileVersionKey) o;

        if (!profileId.equals(that.profileId)) return false;
        if (!version.equals(that.version)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = profileId.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    public Profile getProfile() {
        return profile;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getVersion() {
        return version;
    }
}

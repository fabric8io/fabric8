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
package io.fabric8.jolokia.facade.facades;

import io.fabric8.api.HasId;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.VersionSequence;
import io.fabric8.jolokia.facade.utils.Helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jolokia.client.J4pClient;
import org.json.simple.JSONArray;

/**
 */
public class VersionFacade implements Version, HasId {

    J4pClient j4p;
    String id;

    public VersionFacade(J4pClient j4p, String id) {
        this.j4p = j4p;
        this.id = id;
    }

    @Override
	public String getParentId() {
        throw new UnsupportedOperationException();
	}

	@Override
    public Map<String, String> getAttributes() {
        return getFieldValue("attributes");
    }

	@Override
	public boolean hasProfile(String profileId) {
        throw new UnsupportedOperationException();
	}

    @Override
    public List<String> getProfileIds() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public List<Profile> getProfiles() {
        List<Profile> rc = new ArrayList<Profile>();

        JSONArray array = getFieldValue("profiles");
        if (array == null || array.size() == 0) {
            return Collections.emptyList();
        }

        for (Object profile : array) {
            rc.add(new ProfileFacade(j4p, id, (String)profile));
        }
        return rc;
    }

    @Override
    public Profile getProfile(String s) {
        return new ProfileFacade(j4p, id, s);
    }

    @Override
    public Profile getRequiredProfile(String s) {
        return new ProfileFacade(j4p, id, s);
    }

    @Override
    public int compareTo(Version other) {
        return new VersionSequence(id).compareTo(new VersionSequence(other.getId()));
    }

    @Override
    public String getId() {
        return id;
    }

    private <T extends Object> T getFieldValue(String field) {
        return Helpers.getFieldValue(j4p, "getVersion(java.lang.String, java.util.List)", id, field);
    }
}

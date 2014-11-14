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
package io.fabric8.internal;

import io.fabric8.api.AbstractAttributableBuilder;
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.api.VersionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import io.fabric8.api.gravia.IllegalStateAssertion;


/**
 * The default {@link VersionBuilder}
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
public final class DefaultVersionBuilder extends AbstractAttributableBuilder<VersionBuilder> implements VersionBuilder {

	private String versionId;
    private String revision;
	private Map<String, Profile> profiles = new TreeMap<>();
	
	@Override
	public VersionBuilder from(Version version) {
		setAttributes(version.getAttributes());
		addProfiles(version.getProfiles());
        return this;
	}

	@Override
	public VersionBuilder addOptions(OptionsProvider<VersionBuilder> optionsProvider) {
		return optionsProvider.addOptions(this);
	}
	
	@Override
	public VersionBuilder identity(String versionId) {
		this.versionId = versionId;
		return this;
	}

    @Override
    public VersionBuilder setRevision(String revision) {
        this.revision = revision;
        return this;
    }

	@Override
    public Set<String> getProfiles() {
        return profiles.keySet();
    }

    @Override
    public Profile getProfile(String profileId) {
        return profiles.get(profileId);
    }

    @Override
	public VersionBuilder addProfile(Profile profile) {
		profiles.put(profile.getId(), profile);
		return this;
	}

	@Override
	public VersionBuilder addProfiles(List<Profile> prflist) {
		for (Profile profile : prflist) {
			profiles.put(profile.getId(), profile);
		}
		return this;
	}

	@Override
	public VersionBuilder removeProfile(String profileId) {
		profiles.remove(profileId);
		return this;
	}

	@Override
	protected void validate() {
		super.validate();
		IllegalStateAssertion.assertNotNull(versionId, "Version must have an identity");
        for (Profile profile : profiles.values()) {
            String prfversion = profile.getVersion();
            IllegalStateAssertion.assertEquals(versionId, prfversion, "Profile version not '" + versionId + "' for: " + profile);
        }
	}

	@Override
	public Version getVersion() {
		validate();
		List<Profile> prflist = new ArrayList<>(profiles.values());
		return new VersionImpl(versionId, revision, getAttributes(), prflist);
	}
}
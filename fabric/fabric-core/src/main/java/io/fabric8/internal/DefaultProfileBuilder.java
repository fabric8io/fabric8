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

import io.fabric8.api.AbstractBuilder;
import io.fabric8.api.AttributableBuilder;
import io.fabric8.api.Constants;
import io.fabric8.api.DataStore;
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.internal.ProfileImpl.ConfigListType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.gravia.utils.IllegalStateAssertion;


/**
 * The default {@link ProfileBuilder}
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
final class DefaultProfileBuilder extends AbstractBuilder<ProfileBuilder> implements AttributableBuilder<ProfileBuilder>, ProfileBuilder {

	private String versionId;
	private String profileId;
	private Map<String, Profile> parentProfiles = new LinkedHashMap<>();
	private Map<String, byte[]> fileConfigurations = new HashMap<>();
	private Map<String, Map<String, String>> configurations = new HashMap<>();
	private String lastModified;
	private boolean isOverlay;
	
	DefaultProfileBuilder(String versionId, String profileId) {
	    this.versionId = versionId;
		this.profileId = profileId;
	}

	DefaultProfileBuilder(Profile profile) {
		versionId = profile.getVersion();
		profileId = profile.getId();
		addParents(profile.getParents());
		setFileConfigurations(profile.getFileConfigurations());
		setConfigurations(profile.getConfigurations());
		lastModified = null;
	}

	@Override
	public ProfileBuilder addOptions(OptionsProvider<ProfileBuilder> optionsProvider) {
		return optionsProvider.addOptions(this);
	}

	@Override
	public ProfileBuilder identity(String profileId) {
		this.profileId = profileId;
		return this;
	}

	@Override
	public ProfileBuilder version(String versionId) {
		this.versionId = versionId;
		return this;
	}

	@Override
	public ProfileBuilder addParent(Profile profile) {
		parentProfiles.put(profile.getId(), profile);
		return this;
	}

	@Override
	public ProfileBuilder addParents(List<Profile> profiles) {
		return addParentsInternal(profiles, false);
	}

	@Override
    public ProfileBuilder setParents(List<Profile> profiles) {
        return addParentsInternal(profiles, true);
    }

    private ProfileBuilder addParentsInternal(List<Profile> profiles, boolean clear) {
        if (clear) {
            parentProfiles.clear();
        }
        for (Profile profile : profiles) {
            parentProfiles.put(profile.getId(), profile);
        }
        return this;
    }
    
    @Override
	public ProfileBuilder removeParent(String profileId) {
		parentProfiles.remove(profileId);
		return this;
	}

	@Override
	public ProfileBuilder setFileConfigurations(Map<String, byte[]> configurations) {
		fileConfigurations = new HashMap<>(configurations);
		return this;
	}

    @Override
    public ProfileBuilder addConfigurationFile(String fileName, byte[] data) {
        fileConfigurations.put(fileName, data);
        return this;
    }

    @Override
    public ProfileBuilder deleteConfigurationFile(String fileName) {
        fileConfigurations.remove(fileName);
        return this;
    }

	@Override
	public ProfileBuilder setConfigurations(Map<String, Map<String, String>> configs) {
		configurations = new HashMap<>();
		for (Entry<String, Map<String, String>> entry : configs.entrySet()) {
			String pid = entry.getKey();
			Map<String, String> config = entry.getValue();
			configurations.put(pid, new HashMap<String, String>(config));
		}
		return this;
	}

	@Override
	public ProfileBuilder addConfiguration(String pid, Map<String, String> config) {
		configurations.put(pid, new HashMap<String, String>(config));
		return this;
	}

    @Override
    public Map<String, String> getConfiguration(String pid) {
        Map<String, String> config = configurations.get(pid);
        return config != null ? Collections.unmodifiableMap(config) : null;
    }
    
    @Override
    public ProfileBuilder deleteConfiguration(String pid) {
        configurations.remove(pid);
        return this;
    }
    
	@Override
	public ProfileBuilder setBundles(List<String> values) {
		addAgentConfiguration(ConfigListType.BUNDLES, values);
		return this;
	}

	@Override
	public ProfileBuilder setFabs(List<String> values) {
		addAgentConfiguration(ConfigListType.FABS, values);
		return this;
	}

	@Override
	public ProfileBuilder setFeatures(List<String> values) {
		addAgentConfiguration(ConfigListType.FEATURES, values);
		return this;
	}

	@Override
	public ProfileBuilder setRepositories(List<String> values) {
		addAgentConfiguration(ConfigListType.BUNDLES, values);
		return this;
	}

	@Override
	public ProfileBuilder setOverrides(List<String> values) {
		addAgentConfiguration(ConfigListType.OVERRIDES, values);
		return this;
	}

    @Override
    public ProfileBuilder setOptionals(List<String> values) {
        addAgentConfiguration(ConfigListType.OPTIONALS, values);
        return this;
    }

    @Override
    public ProfileBuilder setTags(List<String> values) {
        addAgentConfiguration(ConfigListType.TAGS, values);
        return this;
    }

	public ProfileBuilder setOverlay(boolean overlay) {
		this.isOverlay = overlay;
		return this;
	}

	@Override
	public ProfileBuilder setLastModified(String lastModified) {
		this.lastModified = lastModified;
		return this;
	}

	@Override
    public ProfileBuilder addAttribute(String key, String value) {
        Map<String, String> agentConfig = getAgentConfiguration();
        agentConfig.put(DataStore.ATTRIBUTE_PREFIX + key, value);
        return this;
    }

    @Override
    public ProfileBuilder setAttributes(Map<String, String> attributes) {
        Map<String, String> agentConfig = getAgentConfiguration();
        for (String key : new ArrayList<>(agentConfig.keySet())) {
            if (key.startsWith(DataStore.ATTRIBUTE_PREFIX)) {
                agentConfig.remove(key);
            }
        }
        for (Entry<String, String> entry : attributes.entrySet()) {
            agentConfig.put(DataStore.ATTRIBUTE_PREFIX + entry.getKey(), entry.getValue());
        }
        return null;
    }

    private Map<String, String> getAgentConfiguration() {
        Map<String, String> agentConfig = configurations.get(Constants.AGENT_PID);
        if (agentConfig == null) {
            agentConfig = new HashMap<String, String>();
            configurations.put(Constants.AGENT_PID, agentConfig);
        }
        return agentConfig;
    }
    
    private void addAgentConfiguration(ConfigListType type, List<String> values) {
		String prefix = type + ".";
		Map<String, String> agentConfig = getAgentConfiguration();
        for (String key : new ArrayList<>(agentConfig.keySet())) {
            if (key.startsWith(prefix)) {
                agentConfig.remove(key);
            }
        }
		for (String value : values) {
			agentConfig.put(prefix + value, value);
		}
	}
	
	@Override
	protected void validate() {
		super.validate();
		IllegalStateAssertion.assertNotNull(profileId, "Profile must have an identity");
		IllegalStateAssertion.assertNotNull(versionId, "Version must be specified");
		for (Profile profile : parentProfiles.values()) {
			String prfversion = profile.getVersion();
			IllegalStateAssertion.assertEquals(versionId, prfversion, "Profile version not '" + versionId + "' for: " + profile);
		}
	}

	@Override
	public Profile getProfile() {
		validate();
		List<Profile> parents = new ArrayList<>(parentProfiles.values());
		return new ProfileImpl(versionId, profileId, parents, fileConfigurations, configurations, lastModified, isOverlay);
	}
}
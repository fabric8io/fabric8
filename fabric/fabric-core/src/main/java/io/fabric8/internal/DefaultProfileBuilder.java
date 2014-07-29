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
import io.fabric8.api.OptionsProvider;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.internal.ProfileImpl.ConfigListType;
import io.fabric8.utils.DataStoreUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jboss.gravia.utils.IllegalStateAssertion;


/**
 * The default {@link ProfileBuilder}
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
final class DefaultProfileBuilder extends AbstractBuilder<ProfileBuilder> implements AttributableBuilder<ProfileBuilder>, ProfileBuilder {

    private static final String PARENTS_ATTRIBUTE_KEY = Profile.ATTRIBUTE_PREFIX + Profile.PARENTS;
	
    private String versionId;
	private String profileId;
	private Map<String, Profile> parentMapping = new LinkedHashMap<>();
	private Map<String, byte[]> fileMapping = new HashMap<>();
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
	    parentMapping.put(profile.getId(), profile);
	    updateParentsAttribute();
	    return this;
	}

	@Override
	public ProfileBuilder addParents(List<Profile> profiles) {
		return addParentsInternal(profiles, false);
	}

	@Override
    public List<String> getParents() {
        return Collections.unmodifiableList(new ArrayList<>(parentMapping.keySet()));
    }

    @Override
    public Profile getParent(String profileId) {
        return parentMapping.get(profileId);
    }

    @Override
    public ProfileBuilder setParents(List<Profile> profiles) {
        return addParentsInternal(profiles, true);
    }

    private ProfileBuilder addParentsInternal(List<Profile> profiles, boolean clear) {
        if (clear) {
            parentMapping.clear();
        }
        for (Profile profile : profiles) {
            parentMapping.put(profile.getId(), profile);
        }
        updateParentsAttribute();
        return this;
    }
    
    @Override
	public ProfileBuilder removeParent(String profileId) {
		parentMapping.remove(profileId);
		updateParentsAttribute();
		return this;
	}

    private void updateParentsAttribute() {
        Map<String, String> agentConfig = getMutableAgentConfiguration();
        agentConfig.remove(PARENTS_ATTRIBUTE_KEY);
        if (parentMapping.size() > 0) {
            agentConfig.put(PARENTS_ATTRIBUTE_KEY, parentsAttributeValue());
        }
        addConfiguration(Constants.AGENT_PID, agentConfig);
    }

    private String parentsAttributeValue() {
        String pspec = "";
        if (parentMapping.size() > 0) {
            for (String parentId : parentMapping.keySet()) {
                pspec += " " + parentId;
            }
            pspec = pspec.substring(1);
        }
        return pspec;
    }
    
    @Override
    public Set<String> getFileConfigurationKeys() {
        return fileMapping.keySet();
    }

    @Override
    public byte[] getFileConfiguration(String key) {
        return fileMapping.get(key);
    }

	@Override
	public ProfileBuilder setFileConfigurations(Map<String, byte[]> configurations) {
		fileMapping = new HashMap<>(configurations);
		return this;
	}

    @Override
    public ProfileBuilder addFileConfiguration(String fileName, byte[] data) {
        fileMapping.put(fileName, data);
        return this;
    }

    @Override
    public ProfileBuilder deleteFileConfiguration(String fileName) {
        fileMapping.remove(fileName);
        return this;
    }

	@Override
	public ProfileBuilder setConfigurations(Map<String, Map<String, String>> configs) {
	    for (String pid : getConfigurationKeys()) {
	        deleteConfiguration(pid);
	    }
		for (Entry<String, Map<String, String>> entry : configs.entrySet()) {
		    addConfiguration(entry.getKey(), new HashMap<>(entry.getValue()));
		}
		return this;
	}

	@Override
	public ProfileBuilder addConfiguration(String pid, Map<String, String> config) {
        fileMapping.put(pid + Profile.PROPERTIES_SUFFIX, DataStoreUtils.toBytes(config));
		return this;
	}

    @Override
    public Set<String> getConfigurationKeys() {
        Set<String> result = new HashSet<>();
        for (String fileKey : fileMapping.keySet()) {
            if (fileKey.endsWith(Profile.PROPERTIES_SUFFIX)) {
                String configKey = fileKey.substring(0, fileKey.indexOf(Profile.PROPERTIES_SUFFIX));
                result.add(configKey);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    @Override
    public Map<String, String> getConfiguration(String pid) {
        byte[] bytes = fileMapping.get(pid + Profile.PROPERTIES_SUFFIX);
        if (bytes == null) {
            return Collections.emptyMap();
        }
        Map<String, String> config = DataStoreUtils.toMap(bytes);
        return Collections.unmodifiableMap(config);
    }
    
    @Override
    public ProfileBuilder deleteConfiguration(String pid) {
        fileMapping.remove(pid + Profile.PROPERTIES_SUFFIX);
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
        Map<String, String> agentConfig = getMutableAgentConfiguration();
        agentConfig.put(Profile.ATTRIBUTE_PREFIX + key, value);
        addConfiguration(Constants.AGENT_PID, agentConfig);
        return this;
    }

    @Override
    public ProfileBuilder setAttributes(Map<String, String> attributes) {
        Map<String, String> agentConfig = getMutableAgentConfiguration();
        for (String key : new ArrayList<>(agentConfig.keySet())) {
            if (key.startsWith(Profile.ATTRIBUTE_PREFIX)) {
                agentConfig.remove(key);
            }
        }
        for (Entry<String, String> entry : attributes.entrySet()) {
            agentConfig.put(Profile.ATTRIBUTE_PREFIX + entry.getKey(), entry.getValue());
        }
        addConfiguration(Constants.AGENT_PID, agentConfig);
        return null;
    }

    private void addAgentConfiguration(ConfigListType type, List<String> values) {
		String prefix = type + ".";
		Map<String, String> agentConfig = getMutableAgentConfiguration();
        for (String key : new ArrayList<>(agentConfig.keySet())) {
            if (key.startsWith(prefix)) {
                agentConfig.remove(key);
            }
        }
		for (String value : values) {
			agentConfig.put(prefix + value, value);
		}
        addConfiguration(Constants.AGENT_PID, agentConfig);
	}
	
    private Map<String, String> getMutableAgentConfiguration() {
        return new LinkedHashMap<>(getConfiguration(Constants.AGENT_PID));
    }

	@Override
	protected void validate() {
		super.validate();
		IllegalStateAssertion.assertNotNull(profileId, "Profile must have an identity");
		IllegalStateAssertion.assertNotNull(versionId, "Version must be specified");
        for (Profile parent : parentMapping.values()) {
            String parentVersion = parent.getVersion();
            IllegalStateAssertion.assertEquals(versionId, parentVersion, "Profile version not '" + versionId + "' for: " + parent);
        }
	}

	@Override
	public Profile getProfile() {
		validate();
		List<Profile> parents = new ArrayList<>(parentMapping.values());
		return new ProfileImpl(versionId, profileId, parents, fileMapping, lastModified, isOverlay);
	}
}
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
import io.fabric8.utils.FabricValidations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.fabric8.api.gravia.IllegalStateAssertion;


/**
 * The default {@link ProfileBuilder}
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Mar-2014
 */
public final class DefaultProfileBuilder extends AbstractBuilder<ProfileBuilder> implements AttributableBuilder<ProfileBuilder>, ProfileBuilder {

    private static final String PARENTS_ATTRIBUTE_KEY = Profile.ATTRIBUTE_PREFIX + Profile.PARENTS;
    private static final String LOCKED_ATTRIBUTE_KEY = Profile.ATTRIBUTE_PREFIX + Profile.LOCKED;

    private String versionId;
	private String profileId;
	private Map<String, byte[]> fileMapping = new HashMap<>();
	private String lastModified;
	private boolean isOverlay;
	
	@Override
	public ProfileBuilder from(Profile profile) {
		versionId = profile.getVersion();
		profileId = profile.getId();
		setFileConfigurations(profile.getFileConfigurations());
        return this;
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
    public List<String> getParents() {
        Map<String, String> config = getConfigurationInternal(Constants.AGENT_PID);
        String pspec = config.get(PARENTS_ATTRIBUTE_KEY);
        String[] parentIds = pspec != null ? pspec.split(" ") : new String[0];
        return Arrays.asList(parentIds);
    }

    @Override
	public ProfileBuilder addParent(String parentId) {
        return addParentsInternal(Collections.singletonList(parentId), false);
	}

	@Override
	public ProfileBuilder addParents(List<String> parentIds) {
		return addParentsInternal(parentIds, false);
	}

    @Override
    public ProfileBuilder setParents(List<String> parentIds) {
        return addParentsInternal(parentIds, true);
    }

    private ProfileBuilder addParentsInternal(List<String> parentIds, boolean clear) {
        Set<String> currentIds = new LinkedHashSet<String>(getParents());
        if (clear) {
            currentIds.clear();
        }
        currentIds.addAll(parentIds);
        updateParentsAttribute(currentIds);
        return this;
    }
    
    @Override
	public ProfileBuilder removeParent(String profileId) {
        Set<String> currentIds = new LinkedHashSet<String>(getParents());
        currentIds.remove(profileId);
        updateParentsAttribute(currentIds);
		return this;
	}

    private void updateParentsAttribute(Collection<String> parentIds) {
        Map<String, String> config = getConfigurationInternal(Constants.AGENT_PID);
        config.remove(PARENTS_ATTRIBUTE_KEY);
        if (parentIds.size() > 0) {
            config.put(PARENTS_ATTRIBUTE_KEY, parentsAttributeValue(parentIds));
        }
        addConfiguration(Constants.AGENT_PID, config);
    }

    private String parentsAttributeValue(Collection<String> parentIds) {
        String pspec = "";
        if (parentIds.size() > 0) {
            for (String parentId : parentIds) {
                pspec += " " + parentId;
            }
            pspec = pspec.substring(1);
        }
        return pspec;
    }
    
    @Override
    public ProfileBuilder setLocked(boolean flag) {
        Map<String, String> config = getConfigurationInternal(Constants.AGENT_PID);
        if (flag) {
            config.put(LOCKED_ATTRIBUTE_KEY, "true");
        } else {
            config.remove(LOCKED_ATTRIBUTE_KEY);
        }
        addConfiguration(Constants.AGENT_PID, config);
        return this;
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
    public ProfileBuilder addConfiguration(String pid, String key, String value) {
        Map<String, String> config = getConfigurationInternal(pid);
        config.put(key, value);
        return addConfiguration(pid, config);
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
        Map<String, String> config = getConfigurationInternal(pid);
        return Collections.unmodifiableMap(config);
    }

    private Map<String, String> getConfigurationInternal(String pid) {
        byte[] bytes = fileMapping.get(pid + Profile.PROPERTIES_SUFFIX);
        return new HashMap<>(DataStoreUtils.toMap(bytes));
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
		addAgentConfiguration(ConfigListType.REPOSITORIES, values);
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
        addConfiguration(Constants.AGENT_PID, Profile.ATTRIBUTE_PREFIX + key, value);
        return this;
    }

    @Override
    public ProfileBuilder setAttributes(Map<String, String> attributes) {
        Map<String, String> config = getConfigurationInternal(Constants.AGENT_PID);
        for (String key : new ArrayList<>(config.keySet())) {
            if (key.startsWith(Profile.ATTRIBUTE_PREFIX)) {
                config.remove(key);
            }
        }
        for (Entry<String, String> entry : attributes.entrySet()) {
            config.put(Profile.ATTRIBUTE_PREFIX + entry.getKey(), entry.getValue());
        }
        addConfiguration(Constants.AGENT_PID, config);
        return null;
    }

    private void addAgentConfiguration(ConfigListType type, List<String> values) {
		String prefix = type + ".";
		Map<String, String> config = getConfigurationInternal(Constants.AGENT_PID);
        for (String key : new ArrayList<>(config.keySet())) {
            if (key.startsWith(prefix)) {
                config.remove(key);
            }
        }
		for (String value : values) {
			config.put(prefix + value, value);
		}
        addConfiguration(Constants.AGENT_PID, config);
	}
	
	@Override
	protected void validate() {
		super.validate();
		IllegalStateAssertion.assertNotNull(profileId, "Profile must have an identity");
		IllegalStateAssertion.assertNotNull(versionId, "Version must be specified");
        FabricValidations.validateProfileName(versionId);
	}

	@Override
	public Profile getProfile() {
		validate();
		return new ProfileImpl(versionId, profileId, getParents(), fileMapping, lastModified, isOverlay);
	}
}

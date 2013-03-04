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
package org.fusesource.fabric.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.zookeeper.KeeperException;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.FabricException;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.service.FabricServiceImpl;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;

import static org.fusesource.fabric.zookeeper.ZkProfiles.getPath;

public class ProfileImpl implements Profile {

	public static final String AGENT_PID = "org.fusesource.fabric.agent";

	private final String id;
	private final String version;
	private final FabricServiceImpl service;

	public ProfileImpl(String id, String version, FabricServiceImpl service) {
		this.id = id;
		this.version = version;
		this.service = service;
	}

	public String getId() {
		return id;
	}

	public String getVersion() {
		return version;
	}

	@Override
	public Properties getAttributes() {
		try {
			String node = getPath(version, id);
			return ZooKeeperUtils.getProperties(service.getZooKeeper(), node);
		} catch (Exception e) {
			throw new FabricException(e);
		}
	}

	@Override
	public void setAttribute(String key, String value) {
		try {
			Properties props = getAttributes();
			if (value != null) {
				props.setProperty(key, value);
			} else {
				props.remove(key);
			}
			String node = getPath(version, id);
			ZooKeeperUtils.setProperties(service.getZooKeeper(), node, props);
		} catch (Exception e) {
			throw new FabricException(e);
		}
	}

	public FabricServiceImpl getService() {
		return service;
	}

	//In some cases we need to sort profiles by Id.
	@Override
	public int compareTo(Profile profile) {
		return id.compareTo(profile.getId());
	}

	public enum ConfigListType {

		BUNDLES("bundle"),
		FABS("fab"),
		FEATURES("feature"),
		REPOSITORIES("repository"),
		OVERRIDES("override");

		private String value;

		private ConfigListType(String value) {
			this.value = value;
		}

		public String toString() {
			return value;
		}
	}

	public List<String> getBundles() {
		return getContainerConfigList(this, ConfigListType.BUNDLES);
	}

	public List<String> getFabs() {
		return getContainerConfigList(this, ConfigListType.FABS);
	}

	public List<String> getFeatures() {
		return getContainerConfigList(this, ConfigListType.FEATURES);
	}

	public List<String> getRepositories() {
		return getContainerConfigList(this, ConfigListType.REPOSITORIES);
	}

	@Override
	public List<String> getOverrides() {
		return getContainerConfigList(this, ConfigListType.OVERRIDES);
	}

	@Override
	public void setBundles(List<String> values) {
		setContainerConfigList(this, values, ConfigListType.BUNDLES);
	}

	@Override
	public void setFabs(List<String> values) {
		setContainerConfigList(this, values, ConfigListType.FABS);
	}

	@Override
	public void setFeatures(List<String> values) {
		setContainerConfigList(this, values, ConfigListType.FEATURES);
	}

	@Override
	public void setRepositories(List<String> values) {
		setContainerConfigList(this, values, ConfigListType.REPOSITORIES);
	}

	@Override
	public void setOverrides(List<String> values) {
		setContainerConfigList(this, values, ConfigListType.OVERRIDES);
	}

	public static List<String> getContainerConfigList(Profile p, ConfigListType type) {
		try {
			Properties containerProps = getContainerProperties(p);
			ArrayList<String> rc = new ArrayList<String>();
			for (Map.Entry<Object, Object> e : containerProps.entrySet()) {
				if (((String) e.getKey()).startsWith(type + ".")) {
					rc.add((String) e.getValue());
				}
			}
			return rc;

		} catch (Exception e) {
			throw new FabricException(e);
		}
	}

	public static void setContainerConfigList(Profile p, List<String> values, ConfigListType type) {
		Map<String, Map<String, String>> config = p.getConfigurations();
		String prefix = type + ".";
		Map<String, String> map = config.get(AGENT_PID);
		if (map == null) {
			map = new HashMap<String, String>();
			config.put(AGENT_PID, map);
		} else {
			List<String> keys = new ArrayList<String>(map.keySet());
			for (String key : keys) {
				if (key.startsWith(prefix)) {
					map.remove(key);
				}
			}
		}
		for (String value : values) {
			map.put(prefix + value, value);
		}
		p.setConfigurations(config);
	}

	public static Properties getContainerProperties(Profile p) throws IOException {
		byte[] b = p.getFileConfigurations().get(AGENT_PID + ".properties");
		if (b != null) {
			return toProperties(b);
		} else {
			return new Properties();
		}
	}

	public Profile[] getParents() {
		try {
			String str = getAttributes().getProperty(PARENTS);
			if (str == null || str.isEmpty()) {
				return new Profile[0];
			}
			str = str.trim();
			List<Profile> profiles = new ArrayList<Profile>();
			for (String p : str.split(" ")) {
				profiles.add(new ProfileImpl(p, version, service));
			}
			return profiles.toArray(new Profile[profiles.size()]);
		} catch (Exception e) {
			throw new FabricException(e);
		}
	}

	public void setParents(Profile[] parents) {
		if (isLocked()) {
			throw new UnsupportedOperationException("The profile " + id + " is locked and can not be modified");
		}
		try {
			StringBuilder sb = new StringBuilder();
			for (Profile parent : parents) {
				if (!version.equals(parent.getVersion())) {
					throw new IllegalArgumentException("Version mismatch setting parent profile " + parent + " with version "
							+ parent.getVersion() + " expected version " + version);
				}
				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(parent.getId());
			}
			setAttribute(PARENTS, sb.toString());
		} catch (Exception e) {
			throw new FabricException(e);
		}
	}

	public Container[] getAssociatedContainers() {
		try {
			ArrayList<Container> rc = new ArrayList<Container>();
			Container[] containers = service.getContainers();
			for (Container container : containers) {
				if (!container.getVersion().getName().equals(getVersion())) {
					continue;
				}
				for (Profile p : container.getProfiles()) {
					if (this.equals(p)) {
						rc.add(container);
						break;
					}
				}
			}
			return rc.toArray(new Container[0]);
		} catch (Exception e) {
			throw new FabricException(e);
		}
	}

	public boolean isOverlay() {
		return false;
	}

	public Profile getOverlay() {
		return new ProfileOverlayImpl(this);
	}

	@Override
	public Map<String, byte[]> getFileConfigurations() {
		try {
			Map<String, byte[]> configurations = new HashMap<String, byte[]>();
			String path = getPath(version, id);
			List<String> pids = service.getZooKeeper().getChildren(path);
			for (String pid : pids) {
				configurations.put(pid, getFileConfiguration(pid));
			}
			return configurations;
		} catch (Exception e) {
			throw new FabricException(e);
		}
	}

	public byte[] getFileConfiguration(String pid) throws InterruptedException, KeeperException {
		IZKClient zooKeeper = service.getZooKeeper();
		String path = getPath(version, id) + "/" + pid;
		if (zooKeeper.exists(path) == null) {
			return null;
		}
		if (zooKeeper.getData(path) == null) {
			List<String> children = zooKeeper.getChildren(path);
			StringBuffer buf = new StringBuffer();
			for (String child : children) {
				String value = zooKeeper.getStringData(path + "/" + child);
				buf.append(String.format("%s = %s\n", child, value));
			}
			return buf.toString().getBytes();
		} else {
			return zooKeeper.getData(path);
		}
	}

	@Override
	public void setFileConfigurations(Map<String, byte[]> configurations) {
		if (isLocked()) {
			throw new UnsupportedOperationException("The profile " + id + " is locked and can not be modified");
		}
		try {
			IZKClient zooKeeper = service.getZooKeeper();
			Map<String, byte[]> oldCfgs = getFileConfigurations();
			// Store new configs
			String path = getPath(version, id);
			for (Map.Entry<String, byte[]> entry : configurations.entrySet()) {
				String pid = entry.getKey();
				oldCfgs.remove(pid);
				byte[] newCfg = entry.getValue();
				String configPath = path + "/" + pid;
				if (zooKeeper.exists(configPath) != null && zooKeeper.getChildren(configPath).size() > 0) {
					List<String> kids = zooKeeper.getChildren(configPath);
					ArrayList<String> saved = new ArrayList<String>();
					// old format, we assume that the byte stream is in
					// a .properties format
					for (String line : new String(newCfg).split("\n")) {
						if (line.startsWith("#") || line.length() == 0) {
							continue;
						}
						String nameValue[] = line.split("=", 2);
						if (nameValue.length < 2) {
							continue;
						}
						String newPath = configPath + "/" + nameValue[0].trim();
						ZooKeeperUtils.set(zooKeeper, newPath, nameValue[1].trim());
						saved.add(nameValue[0].trim());
					}
					for (String kid : kids) {
						if (!saved.contains(kid)) {
							zooKeeper.deleteWithChildren(configPath + "/" + kid);
						}
					}
				} else {
					ZooKeeperUtils.set(zooKeeper, configPath, newCfg);
				}
			}
			for (String pid : oldCfgs.keySet()) {
				zooKeeper.deleteWithChildren(path + "/" + pid);
			}
		} catch (Exception e) {
			throw new FabricException(e);
		}
	}

	public Map<String, Map<String, String>> getConfigurations() {
		try {
			Map<String, Map<String, String>> configurations = new HashMap<String, Map<String, String>>();
			Map<String, byte[]> configs = getFileConfigurations();
			for (Map.Entry<String, byte[]> entry : configs.entrySet()) {
				if (entry.getKey().endsWith(".properties")) {
					String pid = stripSuffix(entry.getKey(), ".properties");
					configurations.put(pid, getConfiguration(pid));
				}
			}
			return configurations;
		} catch (Exception e) {
			throw new FabricException(e);
		}
	}

	@Override
	public Map<String, String> getContainerConfiguration() {
		Map<String, String> map = getConfigurations().get(AGENT_PID);
		if (map == null) {
			map = new HashMap<String, String>();
		}
		return map;
	}

	private Map<String, String> getConfiguration(String pid) throws InterruptedException, KeeperException, IOException {
		IZKClient zooKeeper = service.getZooKeeper();
		String path = getPath(version, id) + "/" + pid + ".properties";
		if (zooKeeper.exists(path) == null) {
			return null;
		}
		byte[] data = zooKeeper.getData(path);
		return toMap(toProperties(data));
	}

	public static byte[] toBytes(Properties source) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		source.store(baos, null);
		return baos.toByteArray();
	}

	public static Properties toProperties(byte[] source) throws IOException {
		Properties rc = new Properties();
		rc.load(new ByteArrayInputStream(source));
		return rc;
	}

	public static Map<String, String> toMap(Properties source) {
		Map<String, String> rc = new HashMap<String, String>();
		for (Map.Entry<Object, Object> entry : source.entrySet()) {
			rc.put((String) entry.getKey(), (String) entry.getValue());
		}
		return rc;
	}

	public static Properties toProperties(Map<String, String> source) {
		Properties rc = new Properties();
		for (Map.Entry<String, String> entry : source.entrySet()) {
			rc.put(entry.getKey(), entry.getValue());
		}
		return rc;
	}

	public static String stripSuffix(String value, String suffix) throws IOException {
		if (value.endsWith(suffix)) {
			return value.substring(0, value.length() - suffix.length());
		} else {
			return value;
		}
	}


	public void setConfigurations(Map<String, Map<String, String>> configurations) {
		if (isLocked()) {
			throw new UnsupportedOperationException("The profile " + id + " is locked and can not be modified");
		}
		try {
			IZKClient zooKeeper = service.getZooKeeper();
			Map<String, Map<String, String>> oldCfgs = getConfigurations();
			// Store new configs
			String path = getPath(version, id);
			for (Map.Entry<String, Map<String, String>> entry : configurations.entrySet()) {
				String pid = entry.getKey();
				oldCfgs.remove(pid);
				byte[] data = toBytes(toProperties(entry.getValue()));
				String p = path + "/" + pid + ".properties";
				ZooKeeperUtils.set(zooKeeper, p, data);
			}
			for (String key : oldCfgs.keySet()) {
				zooKeeper.deleteWithChildren(path + "/" + key + ".properties");
			}
		} catch (Exception e) {
			throw new FabricException(e);
		}
	}

	public void delete() {
		service.deleteProfile(this);
	}

	public boolean configurationEquals(Profile other) {
		Profile[] parents = getParents();
		Profile[] otherParents = other.getParents();
		Arrays.sort(parents);
		Arrays.sort(otherParents);
		if (!getConfigurations().equals(other.getConfigurations())) {
			return false;
		}
		if (parents.length != otherParents.length) {
			return false;
		}

		for (int i = 0; i < parents.length; i++) {
			if (!parents[i].configurationEquals(otherParents[i])) {
				return false;
			}
		}
		return true;
	}


	@Override
	public String toString() {
		return "ProfileImpl[" +
				"id='" + id + '\'' +
				", version='" + version + '\'' +
				']';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ProfileImpl profile = (ProfileImpl) o;
		if (!id.equals(profile.id)) return false;
		if (!version.equals(profile.version)) return false;
		return true;
	}

	@Override
	public int hashCode() {
		int result = id.hashCode();
		result = 31 * result + version.hashCode();
		return result;
	}

	@Override
	public boolean isAbstract() {
		return Boolean.parseBoolean(getAttributes().getProperty(ABSTRACT));
	}

	@Override
	public boolean isLocked() {
		return Boolean.parseBoolean(getAttributes().getProperty(LOCKED));
	}

	@Override
	public boolean isHidden() {
		return Boolean.parseBoolean(getAttributes().getProperty(HIDDEN));
	}

}

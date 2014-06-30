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
package io.fabric8.boot.commands.support;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.internal.ProfileImpl;
import io.fabric8.zookeeper.ZkPath;
import org.apache.curator.framework.CuratorFramework;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.service.cm.ConfigurationAdmin;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;

public abstract class FabricCommand extends OsgiCommandSupport {

    private CuratorFramework curator;
    protected FabricService fabricService;
    protected ConfigurationAdmin configurationAdmin;

    public FabricService getFabricService() {
        return fabricService;
    }

    public void setFabricService(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    protected void checkFabricAvailable() throws Exception {
    }

    public static String toString(Profile[] profiles) {
        if (profiles == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < profiles.length; i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(profiles[i].getId());
        }
        return sb.toString();
    }

    public static String toString(Iterable<String> profiles) {
        if (profiles == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String profile : profiles) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(profile);
            first = false;
        }
        return sb.toString();
    }

    /**
     * Gets all the profiles for the given names.
     * <p/>
     * <b>Important:</b> If a profile does not already exists with the given name, then a new {@link Profile} is
     * created and returned in the list.
     *
     * @see #getExistingProfiles(io.fabric8.api.FabricService, String, java.util.List)
     */
    public static Profile[] getProfiles(FabricService fabricService, String version, List<String> names) {
        return getProfiles(fabricService, fabricService.getVersion(version), names);
    }

    /**
     * Gets (or creates) all the profiles for the given names.
     * <p/>
     * <b>Important:</b> If a profile does not already exists with the given name, then a new {@link Profile} is
     * created and returned in the list.
     *
     * @see #getExistingProfiles(io.fabric8.api.FabricService, io.fabric8.api.Version, java.util.List)
     */
    public static Profile[] getProfiles(FabricService fabricService, Version version, List<String> names) {
        Profile[] allProfiles = version.getProfiles();
        List<Profile> profiles = new ArrayList<Profile>();
        if (names == null) {
            return new Profile[0];
        }
        for (String name : names) {
            Profile profile = null;
            for (Profile p : allProfiles) {
                if (name.equals(p.getId())) {
                    profile = p;
                    break;
                }
            }
            if (profile == null) {
                profile = new ProfileImpl(name, version.getId(), fabricService);
            }
            profiles.add(profile);
        }
        return profiles.toArray(new Profile[profiles.size()]);
    }

    /**
     * Gets all the existing profiles for the given names.
     *
     * @throws IllegalArgumentException if a profile with the given name does not exists
     */
    public static Profile[] getExistingProfiles(FabricService fabricService, String version, List<String> names) {
        return getExistingProfiles(fabricService, fabricService.getVersion(version), names);
    }

    /**
     * Gets all the existing profiles for the given names.
     *
     * @throws IllegalArgumentException if a profile with the given name does not exists
     */
    public static Profile[] getExistingProfiles(FabricService fabricService, Version version, List<String> names) {
        Profile[] allProfiles = version.getProfiles();
        List<Profile> profiles = new ArrayList<Profile>();
        if (names == null) {
            return new Profile[0];
        }
        for (String name : names) {
            Profile profile = null;
            for (Profile p : allProfiles) {
                if (name.equals(p.getId())) {
                    profile = p;
                    break;
                }
            }
            if (profile == null) {
                throw new IllegalArgumentException("Profile " + name + " does not exist.");
            }
            profiles.add(profile);
        }
        return profiles.toArray(new Profile[profiles.size()]);
    }

    /**
     * Gets the profile for the given name
     *
     * @throws java.lang.IllegalArgumentException if the profile does not exists
     */
    public static Profile getProfile(Version ver, String name) {
        Profile p = ver.getProfile(name);
        if (p == null) {
            throw new IllegalArgumentException("Profile " + name + " does not exist.");
        }
        return p;
    }

    /**
     * Checks if container is part of the ensemble.
     */
    public static boolean isPartOfEnsemble(FabricService fabricService, String containerName) {
        boolean result = false;
        CuratorFramework curator = fabricService.adapt(CuratorFramework.class);
        try {
            List<String> containerList = new ArrayList<String>();
            String clusterId = getStringData(curator, ZkPath.CONFIG_ENSEMBLES.getPath());
            String containers = getStringData(curator, ZkPath.CONFIG_ENSEMBLE.getPath(clusterId));
            Collections.addAll(containerList, containers.split(","));
            result = containerList.contains(containerName);
        } catch (Throwable t) {
            //ignore
        }
        return result;
    }

    public static Container getContainer(FabricService fabricService, String name) {
        Container[] containers = fabricService.getContainers();
        for (Container container : containers) {
            if (container.getId().equals(name)) {
                return container;
            }
        }
        throw new IllegalArgumentException("Container " + name + " does not exist.");
    }

    public static boolean doesContainerExist(FabricService fabricService, String name) {
        Container[] containers = fabricService.getContainers();
        for (Container container : containers) {
            if (container.getId().equals(name)) {
                return true;
            }
        }
        return false;
    }

    protected String percentText(double value) {
        return NumberFormat.getPercentInstance().format(value);
    }
}

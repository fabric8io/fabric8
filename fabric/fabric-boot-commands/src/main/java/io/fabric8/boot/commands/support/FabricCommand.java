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
package io.fabric8.boot.commands.support;

import org.apache.curator.framework.CuratorFramework;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import io.fabric8.api.Container;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.Version;
import io.fabric8.internal.ProfileImpl;
import io.fabric8.zookeeper.ZkPath;
import org.osgi.service.cm.ConfigurationAdmin;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    protected String toString(Profile[] profiles) {
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

    protected String toString(Iterable<String> profiles) {
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

    protected Profile[] getProfiles(String version, List<String> names) {
        return getProfiles(fabricService.getVersion(version), names);
    }

    protected Profile[] getProfiles(Version version, List<String> names) {
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
                profiles.add(new ProfileImpl(name, version.getId(), fabricService));
            }
            profiles.add(profile);
        }
        return profiles.toArray(new Profile[profiles.size()]);
    }

    protected Profile getProfile(Version ver, String name) {
        Profile p = ver.getProfile(name);
        if (p == null) {
            throw new IllegalArgumentException("Profile " + name + " does not exist.");
        }
        return p;
    }

    /**
     * Checks if container is part of the ensemble.
     *
     * @param containerName
     * @return
     */
    protected boolean isPartOfEnsemble(String containerName) {
        boolean result = false;
        Container container = fabricService.getContainer(containerName);
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

    /**
     * Gets the container by the given name
     *
     * @param name the name of the container
     * @return the found container, or <tt>null</tt> if not found
     */
    protected Container getContainer(String name) {
        Container[] containers = fabricService.getContainers();
        for (Container container : containers) {
            if (container.getId().equals(name)) {
                return container;
            }
        }
        throw new IllegalArgumentException("Container " + name + " does not exist.");
    }

    protected boolean doesContainerExist(String name) {
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

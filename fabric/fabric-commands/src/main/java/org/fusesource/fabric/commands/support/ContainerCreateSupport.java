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
package org.fusesource.fabric.commands.support;

import java.util.Collections;
import java.util.List;

import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Container;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.zookeeper.ZkDefs;

public abstract class ContainerCreateSupport extends FabricCommand {
    @Option(name = "--version", description = "The version id in the registry")
    protected String version = ZkDefs.DEFAULT_VERSION;
    @Option(name = "--profile", multiValued = true, required = false, description = "The profile IDs to associate with the new container(s)")
    protected List<String> profiles;
    @Option(name = "--enable-debuging", multiValued = false, required = false, description = "Enable debugging")
    protected Boolean debugContainer = Boolean.FALSE;
    @Option(name = "--ensemble-server", multiValued = false, required = false, description = "Whether the container should be a new ZooKeeper ensemble server")
    protected Boolean isEnsembleServer = Boolean.FALSE;

    public List<String> getProfileNames() {
        List<String> names = this.profiles;
        if (names == null || names.isEmpty()) {
            names = Collections.singletonList("default");
        }
        return names;
    }

    protected void setProfiles(Container[] children) {
        List<String> names = getProfileNames();
        try {
            Profile[] profiles = getProfiles(version, names);
            for (Container child : children) {
                child.setProfiles(profiles);
            }
        } catch (Exception ex) {

        }
    }

    protected void doValidateProfiles() {
        // get the profiles for the given version
        Version ver = version != null ? fabricService.getVersion(version) : fabricService.getDefaultVersion();
        Profile[] profiles = ver.getProfiles();

        // validate profiles exists before creating a new container
        List<String> names = getProfileNames();
        for (String profile : names) {
            if (!hasProfile(profiles, profile, ver)) {
                throw new IllegalArgumentException("Profile " + profile + " with version " + ver.getName() + " does not exist.");
            }
        }
    }

    private static boolean hasProfile(Profile[] profiles, String name, Version version) {
        if (profiles == null || profiles.length == 0) {
            return false;
        }

        for (Profile profile : profiles) {
            if (profile.getId().equals(name) && profile.getVersion().equals(version.getName())) {
                return true;
            }
        }

        return false;
    }
}

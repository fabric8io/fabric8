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
package org.fusesource.fabric.commands;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.RepositoryImpl;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.FabricCommand;
import org.fusesource.fabric.zookeeper.ZkDefs;

/**
 *
 */
@Command(name = "profile-edit", scope = "fabric", description = "Edit a profile")
public class ProfileEdit extends FabricCommand {

    static final String FEATURE_PREFIX = "feature.";
    static final String REPOSITORY_PREFIX = "repository.";
    static final String BUNDLE_PREFIX = "bundle.";
    static final String DELIMETER = ",";

    @Option(name = "--pid", description = "Target PID to edit")
    private String pid = null;

    @Option(name = "-r", aliases = {"--repositories"}, description = "Edit repositories", required = false, multiValued = false)
    private String repositoryUriList;

    @Option(name = "-f",aliases = {"--features"} ,description = "Edit features", required = false, multiValued = false)
    private String featuresList;

    @Option(name = "-b", aliases = {"--bundles"}, description = "Edit bundles", required = false, multiValued = false)
    private String bundlesList;

    @Option(name = "--set", description = "Set or create value(s)")
    private boolean set = true;

    @Option(name = "--delete", description = "Delete value(s)")
    private boolean delete = false;

    @Argument(index = 0, name = "profile", description = "The target profile to edit", required = true, multiValued = false)
    private String profileName;

    @Argument(index = 1,name = "version",  description = "The version of the profile to edit", required = false, multiValued = false)
    private String versionName = ZkDefs.DEFAULT_VERSION;

    private FeaturesService featuresService;

    @Override
    protected Object doExecute() throws Exception {
        checkFabricAvailable();
        if (delete) {
            set = false;
        }
        Version version = versionName != null ? fabricService.getVersion(versionName) : fabricService.getDefaultVersion();

        for (Profile profile : version.getProfiles()) {
            if (profileName.equals(profile.getId())) {
                editProfile(profile);
            }
        }
        return null;
    }

    private void editProfile(Profile profile) throws Exception {
        String pid = getPid();

        Map<String, Map<String, String>> config = profile.getConfigurations();
        Map<String, String> pidConfig = config.get(pid);
        if (pidConfig == null) {
            pidConfig = new HashMap<String, String>();
        }

        if (featuresList != null && !featuresList.isEmpty()) {
            String[] features = featuresList.split(DELIMETER);
            for (String feature : features) {
                updateConfig(pidConfig, FEATURE_PREFIX + feature.replace('/', '_'), feature, set, delete);
            }
        }
        if (repositoryUriList != null && !repositoryUriList.isEmpty()) {
            String[] repositoryURIs = repositoryUriList.split(DELIMETER);
            for (String repopsitoryURI : repositoryURIs) {
                updateConfig(pidConfig, REPOSITORY_PREFIX + repopsitoryURI.replace('/', '_'), repopsitoryURI, set, delete);
            }
        }
        if (bundlesList != null && !bundlesList.isEmpty()) {
            String[] bundles = bundlesList.split(DELIMETER);
            for (String bundlesLocation : bundles) {
                updateConfig(pidConfig, BUNDLE_PREFIX + bundlesLocation.replace('/', '_'), bundlesLocation, set, delete);
            }
        }

        config.put(pid, pidConfig);
        profile.setConfigurations(config);
    }

    public void updateConfig(Map<String,String> map, String key, String value, boolean set, boolean delete) {
      if (set) {
          map.put(key,value);
      } else if (delete)  {
          map.remove(key);
      }
    }

    private String getPid() {
        if (pid != null) {
            return pid;
        } else {
            return AGENT_PID;
        }
    }
}

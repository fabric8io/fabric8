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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.features.FeaturesService;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.commands.support.FabricCommand;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@Command(name = "profile-edit", scope = "fabric", description = "Edit a profile")
public class ProfileEdit extends FabricCommand {

     private static final Logger LOGGER = LoggerFactory.getLogger(ProfileEdit.class);

    static final String FEATURE_PREFIX = "feature.";
    static final String REPOSITORY_PREFIX = "repository.";
    static final String BUNDLE_PREFIX = "bundle.";
    static final String CONFIG_PREFIX = "config.";
    static final String SYSTEM_PREFIX = "system.";
    static final String DELIMETER = ",";


    @Option(name = "-r", aliases = {"--repositories"}, description = "Edit repositories", required = false, multiValued = false)
    private String repositoryUriList;

    @Option(name = "-f",aliases = {"--features"} ,description = "Edit features", required = false, multiValued = false)
    private String featuresList;

    @Option(name = "-b", aliases = {"--bundles"}, description = "Edit bundles", required = false, multiValued = false)
    private String bundlesList;

    @Option(name = "-p", aliases = {"--pid"}, description = "Edit configuration pid", required = false, multiValued = false)
    private String configAdminConfigList;

    @Option(name = "-s", aliases = {"--system"}, description = "Edit system properties", required = false, multiValued = false)
    private String systemPropertyList;

    @Option(name = "-c", aliases = {"--config"}, description = "Edit system properties", required = false, multiValued = false)
    private String configPropertyList;

    @Option(name = "-i", aliases = {"--import-pid"}, description = "Imports the pids that are edited, from local config admin", required = false, multiValued = false)
    private boolean importPid = false;

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

        Map<String, Map<String, String>> config = profile.getConfigurations();
        Map<String, String> pidConfig = config.get(AGENT_PID);
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

        if (configAdminConfigList != null && !configAdminConfigList.isEmpty()) {
            Map<String, String> configMap = extractConfigs(configAdminConfigList);
            for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                String key = configEntries.getKey();
                if (key.contains(".")) {
                    String pid = key.substring(0, key.lastIndexOf("."));
                    key = key.substring(key.lastIndexOf(".") + 1);
                    String value = configEntries.getValue();
                    Map<String,String> cfg = config.get(pid);
                    if (cfg == null) {
                        cfg = new HashMap<String,String>();
                    }
                    if (importPid) {
                        importPidFromLocalConfigAdmin(pid, cfg);
                    }
                    updateConfig(cfg,key,value,set,delete);
                    config.put(pid,cfg);
                }
            }
        }

        if (systemPropertyList != null && !systemPropertyList.isEmpty()) {
            String[] keyValues = systemPropertyList.split("=");
            Map<String, String> configMap = extractConfigs(systemPropertyList);
            for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                String key = configEntries.getKey();
                String value = configEntries.getValue();
                updateConfig(pidConfig, SYSTEM_PREFIX + key, value, set, delete);
            }
        }

        if (configPropertyList != null && !configPropertyList.isEmpty()) {
            String[] keyValues = configPropertyList.split("=");
            Map<String, String> configMap = extractConfigs(configPropertyList);
            for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                String key = configEntries.getKey();
                String value = configEntries.getValue();
                updateConfig(pidConfig, CONFIG_PREFIX + key, value, set, delete);
            }
        }

        config.put(AGENT_PID, pidConfig);
        profile.setConfigurations(config);
    }

    public void updateConfig(Map<String,String> map, String key, String value, boolean set, boolean delete) {
      if (set) {
          map.put(key,value);
      } else if (delete)  {
          map.remove(key);
      }
    }

    /**
     * Imports the pid to the target Map.
     * @param pid
     * @param target
     */
    private void importPidFromLocalConfigAdmin(String pid, Map<String, String> target) {
        try {
            Configuration configuration = configurationAdmin.getConfiguration(pid);
            Dictionary dictionary = configuration.getProperties();
            Enumeration keyEnumeration = dictionary.keys();
            while (keyEnumeration.hasMoreElements()) {
                String key = String.valueOf(keyEnumeration.nextElement());
                String value = String.valueOf(dictionary.get(key));
                target.put(key,value);
            }
        } catch (Exception e) {
            LOGGER.warn("Error while importing configuration {} to profile.",pid);
        }
    }

    /**
     * Extracts Key value pairs from a delimited string of key value pairs.
     * Note: The value may contain commas.
     * @param configs
     * @return
     */
    private Map<String, String> extractConfigs(String configs) {
        Map<String, String> configMap = new HashMap<String, String>();
        //If contains key values.
        if (configs.contains("=")) {
            String[] keyValues = configs.split("=");
            int index = 0;
            String prefix = "";
            while (index + 1 < keyValues.length) {
                String key = !prefix.isEmpty() ? prefix : keyValues[index];
                String value = keyValues[index + 1];
                if (value.contains(DELIMETER) && index + 2 != keyValues.length) {
                    prefix = value.substring(value.lastIndexOf(DELIMETER) + 1);
                    value = value.substring(0, value.lastIndexOf(DELIMETER));
                } else {
                    prefix = "";
                }
                configMap.put(key, value);
                index += 1;
            }
        } else {
          String[] keys = configs.split(",");
          for (String key:keys) {
              configMap.put(key,"");
          }
        }
        return configMap;
    }


}

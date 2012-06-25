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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.actors.threadpool.Arrays;

/**
 *
 */
@Command(name = "profile-edit", scope = "fabric", description = "Edits the specified version of the specified profile (where the version defaults to the current default version)", detailedDescription = "classpath:profileEdit.txt")
public class ProfileEdit extends FabricCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileEdit.class);

    static final String FEATURE_PREFIX = "feature.";
    static final String REPOSITORY_PREFIX = "repository.";
    static final String BUNDLE_PREFIX = "bundle.";
    static final String FAB_PREFIX = "fab.";
    static final String CONFIG_PREFIX = "config.";
    static final String SYSTEM_PREFIX = "system.";
    static final String DELIMETER = ",";
    static final String PID_KEY_SEPARATOR = "/";


    @Option(name = "-r", aliases = {"--repositories"}, description = "Edit the features repositories", required = false, multiValued = false)
    private String repositoryUriList;

    @Option(name = "-f", aliases = {"--features"}, description = "Edit features, specifying a comma-separated list of features to add (or delete).", required = false, multiValued = false)
    private String featuresList;

    @Option(name = "-b", aliases = {"--bundles"}, description = "Edit bundles, specifying a comma-separated list of bundles to add (or delete).", required = false, multiValued = false)
    private String bundlesList;

    @Option(name = "-f", aliases = {"--fabs"}, description = "Edit fabs, specifying a comma-separated list of fabs to add (or delete).", required = false, multiValued = false)
    private String fabsList;

    @Option(name = "-p", aliases = {"--pid"}, description = "Edit an OSGi configuration property, specified in the format <PID>/<Property>.", required = false, multiValued = false)
    private String configAdminConfigList;

    @Option(name = "-s", aliases = {"--system"}, description = "Edit the Java system properties that affect installed bundles (analogous to editing etc/system.properties in a root container).", required = false, multiValued = false)
    private String systemPropertyList;

    @Option(name = "-c", aliases = {"--config"}, description = "Edit the Java system properties that affect the karaf container (analogous to editing etc/config.properties in a root container).", required = false, multiValued = false)
    private String configPropertyList;

    @Option(name = "-i", aliases = {"--import-pid"}, description = "Imports the pids that are edited, from local OSGi config admin", required = false, multiValued = false)
    private boolean importPid = false;

    @Option(name = "--set", description = "Set or create values (selected by default).")
    private boolean set = true;

    @Option(name = "--delete", description = "Delete values.")
    private boolean delete = false;

    @Option(name = "--append", description = "Append value. It is only usable with the system, config & pid options")
    private boolean append = false;

    @Option(name = "--remove", description = "Removes values. It is only usable with the system, config & pid options")
    private boolean remove = false;

    @Option(name = "--delimiter", description = "Specifies the delimeter to use for appends and removals.")
    private String delimiter = ",";

    @Argument(index = 0, name = "profile", description = "The target profile to edit", required = true, multiValued = false)
    private String profileName;

    @Argument(index = 1, name = "version", description = "The version of the profile to edit. Defaults to the current default version.", required = false, multiValued = false)
    private String versionName = ZkDefs.DEFAULT_VERSION;


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
        String pid = AGENT_PID;

        if (configAdminConfigList != null) {
            pid = configAdminConfigList.substring(0,configAdminConfigList.indexOf(PID_KEY_SEPARATOR));
        }

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
        if (fabsList != null && !fabsList.isEmpty()) {
            String[] fabs = fabsList.split(DELIMETER);
            for (String fabsLocation : fabs) {
                updateConfig(pidConfig, FAB_PREFIX + fabsLocation.replace('/', '_'), fabsLocation, set, delete);
            }
        }

        if (configAdminConfigList != null && !configAdminConfigList.isEmpty()) {
            Map<String, String> configMap = extractConfigs(configAdminConfigList);
            for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                String key = configEntries.getKey();
                if (key.contains(PID_KEY_SEPARATOR)) {
                    String currentPid = key.substring(0, key.lastIndexOf(PID_KEY_SEPARATOR));
                    key = key.substring(key.lastIndexOf(PID_KEY_SEPARATOR) + 1);
                    String value = configEntries.getValue();
                    Map<String, String> cfg = config.get(currentPid);
                    if (cfg == null) {
                        cfg = new HashMap<String, String>();
                    }
                    if (importPid) {
                        importPidFromLocalConfigAdmin(currentPid, cfg);
                    }
                    updatedDelimitedList(pidConfig, key, value, delimiter, set, delete, append, remove);
                    config.put(currentPid, cfg);
                }
            }
        }

        if (systemPropertyList != null && !systemPropertyList.isEmpty()) {
            String[] keyValues = systemPropertyList.split("=");
            Map<String, String> configMap = extractConfigs(systemPropertyList);
            for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                String key = configEntries.getKey();
                String value = configEntries.getValue();
                updatedDelimitedList(pidConfig, SYSTEM_PREFIX + key, value, delimiter, set, delete, append, remove);
            }
        }

        if (configPropertyList != null && !configPropertyList.isEmpty()) {
            String[] keyValues = configPropertyList.split("=");
            Map<String, String> configMap = extractConfigs(configPropertyList);
            for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                String key = configEntries.getKey();
                String value = configEntries.getValue();
                updatedDelimitedList(pidConfig, CONFIG_PREFIX + key, value, delimiter, set, delete, append, remove);
            }
        }

        config.put(pid, pidConfig);
        profile.setConfigurations(config);
    }


    public void updatedDelimitedList(Map<String, String> map, String key, String value, String delimeter, boolean set, boolean delete, boolean append, boolean remove) {
        if (append || remove) {
            String oldValue = map.containsKey(key) ? map.get(key) : "";
            List<String> parts = new LinkedList(Arrays.asList(oldValue.split(delimeter)));
            //We need to remove any possible blanks.
            parts.remove("");
            if (append) {
                parts.add(value);
            }
            if (remove) {
                parts.remove(value);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                if (i != 0) {
                    sb.append(delimeter);
                }
                sb.append(parts.get(i));
            }
            map.put(key, sb.toString());
        } else if (set) {
            map.put(key, value);
        } else if (delete) {
            map.remove(key);
        }
    }

    public void updateConfig(Map<String, String> map, String key, String value, boolean set, boolean delete) {
        if (set) {
            map.put(key, value);
        } else if (delete) {
            map.remove(key);
        }
    }

    /**
     * Imports the pid to the target Map.
     *
     * @param pid
     * @param target
     */
    private void importPidFromLocalConfigAdmin(String pid, Map<String, String> target) {
        try {
            Configuration[] configuration = configurationAdmin.listConfigurations("service.pid=" + pid + ")");
            if (configuration != null && configuration.length > 0) {
                Dictionary dictionary = configuration[0].getProperties();
                Enumeration keyEnumeration = dictionary.keys();
                while (keyEnumeration.hasMoreElements()) {
                    String key = String.valueOf(keyEnumeration.nextElement());
                    String value = String.valueOf(dictionary.get(key));
                    target.put(key, value);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error while importing configuration {} to profile.", pid);
        }
    }

    /**
     * Extracts Key value pairs from a delimited string of key value pairs.
     * Note: The value may contain commas.
     *
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
            for (String key : keys) {
                configMap.put(key, "");
            }
        }
        return configMap;
    }


}

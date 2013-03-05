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


import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jline.Terminal;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.fabric.api.Profile;
import org.fusesource.fabric.api.Version;
import org.fusesource.fabric.boot.commands.support.FabricCommand;
import org.fusesource.fabric.commands.support.ZookeeperContentManager;
import org.fusesource.fabric.zookeeper.ZkDefs;
import org.fusesource.fabric.zookeeper.ZkProfiles;
import org.jledit.ConsoleEditor;
import org.jledit.EditorFactory;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@Command(name = "profile-edit", scope = "fabric", description = "Edits the specified version of the specified profile (where the version defaults to the current default version)", detailedDescription = "classpath:profileEdit.txt")
public class ProfileEdit extends FabricCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileEdit.class);
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    static final String FEATURE_PREFIX = "feature.";
    static final String REPOSITORY_PREFIX = "repository.";
    static final String BUNDLE_PREFIX = "bundle.";
    static final String FAB_PREFIX = "fab.";
    static final String OVERRIDE_PREFIX = "override.";
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

    @Option(name = "-o", aliases = {"--overrides"}, description = "Edit overrides, specifying a comma-separated list of overrides to add (or delete).", required = false, multiValued = false)
    private String overridesList;

    @Option(name = "-p", aliases = {"--pid"}, description = "Edit an OSGi configuration property, specified in the format <PID>/<Property>.", required = false, multiValued = true)
    private String[] configAdminProperties;

    @Option(name = "-s", aliases = {"--system"}, description = "Edit the Java system properties that affect installed bundles (analogous to editing etc/system.properties in a root container).", required = false, multiValued = true)
    private String[] systemProperties;

    @Option(name = "-c", aliases = {"--config"}, description = "Edit the Java system properties that affect the karaf container (analogous to editing etc/config.properties in a root container).", required = false, multiValued = true)
    private String[] configProperties;

    @Option(name = "-i", aliases = {"--import-pid"}, description = "Imports the pids that are edited, from local OSGi config admin", required = false, multiValued = false)
    private boolean importPid = false;

    @Option(name = "--resource", description = "Selects a resource under the profile to edit. This option should only be used alone.", required = false, multiValued = false)
    private String resource;

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

    private EditorFactory editorFactory;


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
        boolean editInLine = false;

        Map<String, Map<String, String>> config = profile.getConfigurations();
        Map<String, String> pidConfig = config.get(pid);

        if (pidConfig == null) {
            pidConfig = new HashMap<String, String>();
        }

        if (featuresList != null && !featuresList.isEmpty()) {
            editInLine = true;
            String[] features = featuresList.split(DELIMETER);
            for (String feature : features) {
                updateConfig(pidConfig, FEATURE_PREFIX + feature.replace('/', '_'), feature, set, delete);
            }
        }
        if (repositoryUriList != null && !repositoryUriList.isEmpty()) {
            editInLine = true;
            String[] repositoryURIs = repositoryUriList.split(DELIMETER);
            for (String repopsitoryURI : repositoryURIs) {
                updateConfig(pidConfig, REPOSITORY_PREFIX + repopsitoryURI.replace('/', '_'), repopsitoryURI, set, delete);
            }
        }
        if (bundlesList != null && !bundlesList.isEmpty()) {
            editInLine = true;
            String[] bundles = bundlesList.split(DELIMETER);
            for (String bundlesLocation : bundles) {
                updateConfig(pidConfig, BUNDLE_PREFIX + bundlesLocation.replace('/', '_'), bundlesLocation, set, delete);
            }
        }
        if (fabsList != null && !fabsList.isEmpty()) {
            editInLine = true;
            String[] fabs = fabsList.split(DELIMETER);
            for (String fabsLocation : fabs) {
                updateConfig(pidConfig, FAB_PREFIX + fabsLocation.replace('/', '_'), fabsLocation, set, delete);
            }
        }
        if (overridesList != null && !overridesList.isEmpty()) {
            editInLine = true;
            String[] overrides = overridesList.split(DELIMETER);
            for (String overridesLocation : overrides) {
                updateConfig(pidConfig, OVERRIDE_PREFIX + overridesLocation.replace('/', '_'), overridesLocation, set, delete);
            }
        }

        if (configAdminProperties != null && configAdminProperties.length > 0) {
            for (String configAdminProperty : configAdminProperties) {
                String currentPid = null;
                Map<String, String> existingConfig = null;

                if (configAdminProperty != null) {
                    String keyValue = "";
                    if (configAdminProperty.contains(PID_KEY_SEPARATOR)) {
                        currentPid = configAdminProperty.substring(0, configAdminProperty.indexOf(PID_KEY_SEPARATOR));
                        keyValue = configAdminProperty.substring(configAdminProperty.indexOf(PID_KEY_SEPARATOR) + 1);
                        editInLine = true;
                    } else {
                        currentPid = configAdminProperty;
                    }

                    existingConfig = config.get(currentPid);
                    if (existingConfig == null) {
                        existingConfig = new HashMap<String, String>();
                    }

                    //We only support import when a single pid is spcecified
                    if (configAdminProperties.length == 1 && importPid) {
                        editInLine = true;
                        importPidFromLocalConfigAdmin(currentPid, existingConfig);
                    }


                    Map<String, String> configMap = extractConfigs(keyValue);
                    for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                        String key = configEntries.getKey();
                        String value = configEntries.getValue();
                        updatedDelimitedList(existingConfig, key, value, delimiter, set, delete, append, remove);
                    }

                    config.put(currentPid, existingConfig);
                }
            }
        }

        if (systemProperties != null && systemProperties.length > 0) {
            editInLine = true;
            for (String systemProperty : systemProperties) {
                Map<String, String> configMap = extractConfigs(systemProperty);
                for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                    String key = configEntries.getKey();
                    String value = configEntries.getValue();
                    updatedDelimitedList(pidConfig, SYSTEM_PREFIX + key, value, delimiter, set, delete, append, remove);
                }
            }
        }

        if (configProperties != null && configProperties.length > 0) {
            editInLine = true;
            for (String configProperty : configProperties) {
                Map<String, String> configMap = extractConfigs(configProperty);
                for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                    String key = configEntries.getKey();
                    String value = configEntries.getValue();
                    updatedDelimitedList(pidConfig, CONFIG_PREFIX + key, value, delimiter, set, delete, append, remove);
                }
            }
        }

        if (editInLine) {
            config.put(pid, pidConfig);
            profile.setConfigurations(config);
        } else {
            resource = resource != null ? resource : "org.fusesource.fabric.agent.properties";
            //If a single pid has been selected, but not a key value has been specified or import has been selected,
            //then open the resource in the editor.
            if (configAdminProperties != null && configAdminProperties.length == 1) {
                resource = configAdminProperties[0] + ".properties";
            }
            openInEditor(profile, resource);
        }
    }

    private void openInEditor(Profile profile, String resource) throws Exception {
        String id = profile.getId();
        String version = profile.getVersion();
        String path = ZkProfiles.getPath(version, id) + "/" + resource;
        //Call the editor
        ConsoleEditor editor = editorFactory.create(getTerminal());
        editor.setTitle("Profile");
        editor.setOpenEnabled(false);
        editor.setContentManager(new ZookeeperContentManager(getZooKeeper()));
        editor.open(path, id + " " + version);
        editor.start();
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
            Configuration[] configuration = configurationAdmin.listConfigurations("(service.pid=" + pid + ")");
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
            String key = configs.substring(0, configs.indexOf("="));
            String value = configs.substring(configs.indexOf("=") + 1);
            configMap.put(key, value);
        }
        return configMap;
    }

    /**
     * Gets the {@link jline.Terminal} from the current session.
     *
     * @return
     * @throws Exception
     */
    private Terminal getTerminal() throws Exception {
        Object terminalObject = session.get(".jline.terminal");
        if (terminalObject instanceof Terminal) {
            return (Terminal) terminalObject;

        }
        throw new IllegalStateException("Could not get Terminal from CommandSession.");
    }

    public EditorFactory getEditorFactory() {
        return editorFactory;
    }

    public void setEditorFactory(EditorFactory editorFactory) {
        this.editorFactory = editorFactory;
    }
}

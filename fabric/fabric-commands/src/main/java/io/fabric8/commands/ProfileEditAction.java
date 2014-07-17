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
package io.fabric8.commands;

import io.fabric8.api.Constants;
import io.fabric8.api.DataStore;
import io.fabric8.api.FabricService;
import io.fabric8.api.Profile;
import io.fabric8.api.ProfileBuilder;
import io.fabric8.api.ProfileService;
import io.fabric8.api.Version;
import io.fabric8.commands.support.DatastoreContentManager;
import io.fabric8.utils.FabricValidations;

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
import org.apache.karaf.shell.console.AbstractAction;
import org.jledit.ConsoleEditor;
import org.jledit.EditorFactory;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@Command(name = "profile-edit", scope = "fabric", description = "Edits the specified version of the specified profile (where the version defaults to the current default version)", detailedDescription = "classpath:profileEdit.txt")
public class ProfileEditAction extends AbstractAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileEditAction.class);

    static final String FEATURE_PREFIX = "feature.";
    static final String REPOSITORY_PREFIX = "repository.";
    static final String BUNDLE_PREFIX = "bundle.";
    static final String FAB_PREFIX = "fab.";
    static final String OVERRIDE_PREFIX = "override.";
    static final String CONFIG_PREFIX = "config.";
    static final String SYSTEM_PREFIX = "system.";
    static final String LIB_PREFIX = "lib.";
    static final String ENDORSED_PREFIX = "endorsed.";
    static final String EXT_PREFIX = "ext.";
    static final String DELIMITER = ",";
    static final String PID_KEY_SEPARATOR = "/";

    static final String FILE_INSTALL_FILENAME_PROPERTY = "felix.fileinstall.filename";


    @Option(name = "-r", aliases = {"--repositories"}, description = "Edit the features repositories. To specify multiple repositories, specify this flag multiple times.", required = false, multiValued = true)
    private String[] repositories;

    @Option(name = "-f", aliases = {"--features"}, description = "Edit features. To specify multiple features, specify this flag multiple times. For example, --features foo --features bar.", required = false, multiValued = true)
    private String[] features;

    @Option(name = "-l", aliases = {"--libs"}, description = "Edit libraries. To specify multiple libraries, specify this flag multiple times.", required = false, multiValued = true)
    private String[] libs;

    @Option(name = "-n", aliases = {"--endorsed"}, description = "Edit endorsed libraries. To specify multiple libraries, specify this flag multiple times.", required = false, multiValued = true)
    private String[] endorsed;

    @Option(name = "-x", aliases = {"--extension"}, description = "Edit extension libraries. To specify multiple libraries, specify this flag multiple times.", required = false, multiValued = true)
    private String[] extension;

    @Option(name = "-b", aliases = {"--bundles"}, description = "Edit bundles. To specify multiple bundles, specify this flag multiple times.", required = false, multiValued = true)
    private String[] bundles;

    @Option(name = "--fabs", description = "Edit fabs. To specify multiple fabs, specify this flag multiple times.", required = false, multiValued = true)
    private String[] fabs;

    @Option(name = "-o", aliases = {"--overrides"}, description = "Edit overrides. To specify multiple libraries, specify this flag multiple times.", required = false, multiValued = true)
    private String[] overrides;

    @Option(name = "-p", aliases = {"--pid"}, description = "Edit an OSGi configuration property, specified in the format <PID>/<Property>. To specify multiple properties, specify this flag multiple times.", required = false, multiValued = true)
    private String[] pidProperties;

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

    @Option(name = "--delete", description = "Delete values. This option can be used to delete a feature, a bundle or a pid from the profile.")
    private boolean delete = false;

    @Option(name = "--append", description = "Append value to a delimited list. It is only usable with the system, config & pid options")
    private boolean append = false;

    @Option(name = "--remove", description = "Removes value from a delimited list. It is only usable with the system, config & pid options")
    private boolean remove = false;

    @Option(name = "--delimiter", description = "Specifies the delimiter to use for appends and removals.")
    private String delimiter = ",";

    @Argument(index = 0, name = "profile", description = "The target profile to edit", required = true, multiValued = false)
    private String profileName;

    @Argument(index = 1, name = "version", description = "The version of the profile to edit. Defaults to the current default version.", required = false, multiValued = false)
    private String versionId;

    private final ProfileService profileService;
    private final FabricService fabricService;
    private final ConfigurationAdmin configurationAdmin;
    private final EditorFactory editorFactory;

    ProfileEditAction(FabricService fabricService, ConfigurationAdmin configurationAdmin, EditorFactory editorFactory) {
        this.profileService = fabricService.adapt(ProfileService.class);
        this.fabricService = fabricService;
        this.configurationAdmin = configurationAdmin;
        this.editorFactory = editorFactory;
    }

    @Override
    protected Object doExecute() throws Exception {
        FabricValidations.validateProfileName(profileName);
        if (delete) {
            set = false;
        }
        
        Version version = versionId != null ? profileService.getRequiredVersion(versionId) : fabricService.getRequiredDefaultVersion();
        Profile profile = version.getProfile(profileName);
        if (profile != null) {
            editProfile(profile);
        }
        
        return null;
    }

    private void editProfile(Profile profile) throws Exception {
        boolean editInLine = false;

        ProfileBuilder builder = ProfileBuilder.Factory.createFrom(profile);
        
        if (delete || remove) {
            editInLine = true;
        }

        if (features != null && features.length > 0) {
            editInLine = true;
            handleFeatures(builder, features, profile);
        }
        if (repositories != null && repositories.length > 0) {
            editInLine = true;
            handleFeatureRepositories(builder, repositories, profile);
        }
        if (libs != null && libs.length > 0) {
            editInLine = true;
            handleLibraries(builder, libs, profile, "lib", LIB_PREFIX);
        }
        if (endorsed != null && endorsed.length > 0) {
            editInLine = true;
            handleLibraries(builder, endorsed, profile, "endorsed lib", ENDORSED_PREFIX);
        }
        if (extension != null && extension.length > 0) {
            editInLine = true;
            handleLibraries(builder, extension, profile, "extension lib", EXT_PREFIX);
        }
        if (bundles != null && bundles.length > 0) {
            editInLine = true;
            handleBundles(builder, bundles, profile);
        }
        if (fabs != null && fabs.length > 0) {
            editInLine = true;
            handleFabs(builder, fabs, profile);
        }
        if (overrides != null && overrides.length > 0) {
            editInLine = true;
            handleOverrides(builder, overrides, profile);
        }

        if (pidProperties != null && pidProperties.length > 0) {
            editInLine = handlePid(builder, pidProperties, profile);
        }

        if (systemProperties != null && systemProperties.length > 0) {
            editInLine = true;
            handleSystemProperties(builder, systemProperties, profile);
        }

        if (configProperties != null && configProperties.length > 0) {
            editInLine = true;
            handleConfigProperties(builder, configProperties, profile);
        }

        if (!editInLine) {
            resource = resource != null ? resource : "io.fabric8.agent.properties";
            //If a single pid has been selected, but not a key value has been specified or import has been selected,
            //then open the resource in the editor.
            if (pidProperties != null && pidProperties.length == 1) {
                resource = pidProperties[0] + ".properties";
            }
            openInEditor(profile, resource);
        }
        
        profileService.updateProfile(builder.getProfile());
    }

    /**
     * Adds or remove the specified features to the specified profile.
     */
    private void handleFeatures(ProfileBuilder builder, String[] features, Profile profile) {
        Map<String, String> conf = getConfigurationFromBuilder(builder, Constants.AGENT_PID);
        for (String feature : features) {
            if (delete) {
                System.out.println("Deleting feature:" + feature + " from profile:" + profile.getId() + " version:" + profile.getVersion());
            } else {
                System.out.println("Adding feature:" + feature + " to profile:" + profile.getId() + " version:" + profile.getVersion());
            }
            updateConfig(conf, FEATURE_PREFIX + feature.replace('/', '_'), feature, set, delete);
            builder.addConfiguration(Constants.AGENT_PID, conf);
        }
    }

    /**
     * Adds or remove the specified feature repositories to the specified profile.
     */
    private void handleFeatureRepositories(ProfileBuilder builder, String[] repositories, Profile profile) {
        Map<String, String> conf = getConfigurationFromBuilder(builder, Constants.AGENT_PID);
        for (String repositoryURI : repositories) {
            if (set) {
                System.out.println("Adding feature repository:" + repositoryURI + " to profile:" + profile.getId() + " version:" + profile.getVersion());
            } else if (delete) {
                System.out.println("Deleting feature repository:" + repositoryURI + " from profile:" + profile.getId() + " version:" + profile.getVersion());
            }
            updateConfig(conf, REPOSITORY_PREFIX + repositoryURI.replace('/', '_'), repositoryURI, set, delete);
        }
        builder.addConfiguration(Constants.AGENT_PID, conf);
    }

    /**
     * Adds or remove the specified libraries to the specified profile.
     * @param libs      The array of libs.
     * @param profile   The target profile.
     * @param libType   The type of lib. Used just for the command output.
     * @param libPrefix The prefix of the lib.
     */
    private void handleLibraries(ProfileBuilder builder, String[] libs, Profile profile, String libType, String libPrefix) {
        Map<String, String> conf = getConfigurationFromBuilder(builder, Constants.AGENT_PID);
        for (String lib : libs) {
            if (set) {
                System.out.println("Adding "+libType+":" + lib + " to profile:" + profile.getId() + " version:" + profile.getVersion());
            } else if (delete) {
                System.out.println("Deleting "+libType+":" + lib + " from profile:" + profile.getId() + " version:" + profile.getVersion());
            }
            updateConfig(conf, libPrefix + lib.replace('/', '_'), lib, set, delete);
        }
        builder.addConfiguration(Constants.AGENT_PID, conf);
    }

    /**
     * Adds or remove the specified bundles to the specified profile.
     * @param bundles   The array of bundles.
     * @param profile   The target profile.
     */
    private void handleBundles(ProfileBuilder builder, String[] bundles, Profile profile) {
        Map<String, String> conf = getConfigurationFromBuilder(builder, Constants.AGENT_PID);
        for (String bundle : bundles) {
            if (set) {
                System.out.println("Adding bundle:" + bundle + " to profile:" + profile.getId() + " version:" + profile.getVersion());
            } else if (delete) {
                System.out.println("Deleting bundle:" + bundle + " from profile:" + profile.getId() + " version:" + profile.getVersion());
            }
            updateConfig(conf, BUNDLE_PREFIX + bundle.replace('/', '_'), bundle, set, delete);
        }
        builder.addConfiguration(Constants.AGENT_PID, conf);
    }

    /**
     * Adds or remove the specified fabs to the specified profile.
     * @param fabs      The array of fabs.
     * @param profile   The target profile.
     */
    private void handleFabs(ProfileBuilder builder, String[] fabs, Profile profile) {
        Map<String, String> conf = getConfigurationFromBuilder(builder, Constants.AGENT_PID);
        for (String fab : fabs) {
            if (set) {
                System.out.println("Adding FAB:" + fab + " to profile:" + profile.getId() + " version:" + profile.getVersion());
            } else if (delete) {
                System.out.println("Deleting FAB:" + fab + " from profile:" + profile.getId() + " version:" + profile.getVersion());
            }
            updateConfig(conf, FAB_PREFIX + fab.replace('/', '_'), fab, set, delete);
        }
        builder.addConfiguration(Constants.AGENT_PID, conf);
    }

    /**
     * Adds or remove the specified overrides to the specified profile.
     * @param overrides     The array of overrides.
     * @param profile       The target profile.
     */
    private void handleOverrides(ProfileBuilder builder, String[] overrides, Profile profile) {
        Map<String, String> conf = getConfigurationFromBuilder(builder, Constants.AGENT_PID);
        for (String override : overrides) {
            if (set) {
                System.out.println("Adding override:" + override + " to profile:" + profile.getId() + " version:" + profile.getVersion());
            } else if (delete) {
                System.out.println("Deleting override:" + override + " from profile:" + profile.getId() + " version:" + profile.getVersion());
            }
            updateConfig(conf, OVERRIDE_PREFIX + override.replace('/', '_'), override, set, delete);
        }
        builder.addConfiguration(Constants.AGENT_PID, conf);
    }

    /**
     * Adds or remove the specified system properties to the specified profile.
     * @param pidProperties         The array of system properties.
     * @param profile               The target profile.
     * @return                      True if the edit can take place in line.
     */
    private boolean handlePid(ProfileBuilder builder, String[] pidProperties, Profile profile) {
        boolean editInline = true;
        for (String pidProperty : pidProperties) {
            String currentPid = null;


            String keyValuePair = "";
            if (pidProperty.contains(PID_KEY_SEPARATOR)) {
                currentPid = pidProperty.substring(0, pidProperty.indexOf(PID_KEY_SEPARATOR));
                keyValuePair = pidProperty.substring(pidProperty.indexOf(PID_KEY_SEPARATOR) + 1);
            } else {
                currentPid = pidProperty;
            }
            Map<String, String> conf = getConfigurationFromBuilder(builder, currentPid);
            
            // We only support import when a single pid is specified
            if (pidProperties.length == 1 && importPid) {
                System.out.println("Importing pid:" + currentPid + " to profile:" + profile.getId() + " version:" + profile.getVersion());
                importPidFromLocalConfigAdmin(currentPid, conf);
                builder.addConfiguration(currentPid, conf);
                return true;
            }


            Map<String, String> configMap = extractConfigs(keyValuePair);
            if (configMap.isEmpty() && set) {
                editInline = false;
            } else if (configMap.isEmpty() && delete) {
                editInline = true;
                System.out.println("Deleting pid:" + currentPid + " from profile:" + profile.getId() + " version:" + profile.getVersion());
                builder.deleteConfiguration(currentPid);
            } else {
                for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                    String key = configEntries.getKey();
                    String value = configEntries.getValue();
                    if (value == null && delete) {
                        System.out.println("Deleting key:" + key + " from pid:" + currentPid + " and profile:" + profile.getId() + " version:" +  profile.getVersion());
                        conf.remove(key);
                    } else {
                        if (append) {
                            System.out.println("Appending value:" + value + " key:" + key + " to pid:" + currentPid + " and profile:" + profile.getId() + " version:" +  profile.getVersion());
                        } else if (remove) {
                            System.out.println("Removing value:" + value + " key:" + key + " from pid:" + currentPid + " and profile:" + profile.getId() + " version:" +  profile.getVersion());
                        } else if(set) {
                            System.out.println("Setting value:" + value + " key:" + key + " on pid:" + currentPid + " and profile:" + profile.getId() + " version:" +  profile.getVersion());
                        }
                        updatedDelimitedList(conf, key, value, delimiter, set, delete, append, remove);
                    }
                }
                editInline = true;
                builder.addConfiguration(currentPid, conf);
            }
        }
        return editInline;
    }


    /**
     * Adds or remove the specified system properties to the specified profile.
     * @param systemProperties      The array of system properties.
     * @param profile               The target profile.
     */
    private void handleSystemProperties(ProfileBuilder builder, String[] systemProperties, Profile profile) {
        Map<String, String> conf = getConfigurationFromBuilder(builder, Constants.AGENT_PID);
        for (String systemProperty : systemProperties) {
            Map<String, String> configMap = extractConfigs(systemProperty);
            for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                String key = configEntries.getKey();
                String value = configEntries.getValue();
                if (append) {
                    System.out.println("Appending value:" + value + " key:" + key + " from system properties and profile:" + profile.getId() + " version:" + profile.getVersion());
                } else if (delete) {
                    System.out.println("Deleting key:" + key + " from system properties and profile:" + profile.getId() + " version:" + profile.getVersion());
                } else if (set) {
                    System.out.println("Setting value:" + value + " key:" + key + " from system properties and profile:" + profile.getId() + " version:" + profile.getVersion());
                } else {
                    System.out.println("Removing value:" + value + " key:" + key + " from system properties and profile:" + profile.getId() + " version:" + profile.getVersion());
                }
                updatedDelimitedList(conf, SYSTEM_PREFIX + key, value, delimiter, set, delete, append, remove);
            }
        }
        builder.addConfiguration(Constants.AGENT_PID, conf);
    }

    /**
     * Adds or remove the specified config properties to the specified profile.
     * @param configProperties      The array of config properties.
     * @param profile               The target profile.
     */
    private void handleConfigProperties(ProfileBuilder builder, String[] configProperties, Profile profile) {
        Map<String, String> conf = getConfigurationFromBuilder(builder, Constants.AGENT_PID);
        for (String configProperty : configProperties) {
            Map<String, String> configMap = extractConfigs(configProperty);
            for (Map.Entry<String, String> configEntries : configMap.entrySet()) {
                String key = configEntries.getKey();
                String value = configEntries.getValue();
                if (append) {
                    System.out.println("Appending value:" + value + " key:" + key + " from config properties and profile:" + profile + " version:" + versionId);
                } else if (delete) {
                    System.out.println("Deleting key:" + key + " from config properties and profile:" + profile + " version:" + versionId);
                } else if (set) {
                    System.out.println("Setting value:" + value + " key:" + key + " from config properties and profile:" + profile + " version:" + versionId);
                }
                updatedDelimitedList(conf, CONFIG_PREFIX + key, value, delimiter, set, delete, append, remove);
            }
        }
        builder.addConfiguration(Constants.AGENT_PID, conf);
    }




    private void openInEditor(Profile profile, String resource) throws Exception {
        String id = profile.getId();
        String version = profile.getVersion();
        String location = id + " " + version + " " + resource;
        //Call the editor
        ConsoleEditor editor = editorFactory.create("simple",getTerminal(), System.in, System.out);
        editor.setTitle("Profile");
        editor.setOpenEnabled(false);
        editor.setContentManager(new DatastoreContentManager(fabricService));
        editor.open(location, id + " " + version);
        editor.start();
    }


    public void updatedDelimitedList(Map<String, String> map, String key, String value, String delimiter, boolean set, boolean delete, boolean append, boolean remove) {
        if (append || remove) {
            String oldValue = map.containsKey(key) ? map.get(key) : "";
            List<String> parts = new LinkedList<String>(Arrays.asList(oldValue.split(delimiter)));
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
                    sb.append(delimiter);
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

    private void updateConfig(Map<String, String> map, String key, String value, boolean set, boolean delete) {
        if (set) {
            map.put(key, value);
        } else if (delete) {
            map.remove(key);
        }
    }

    /**
     * Imports the pid to the target Map.
     */
    private void importPidFromLocalConfigAdmin(String pid, Map<String, String> target) {
        try {
            Configuration[] configuration = configurationAdmin.listConfigurations("(service.pid=" + pid + ")");
            if (configuration != null && configuration.length > 0) {
                Dictionary dictionary = configuration[0].getProperties();
                Enumeration keyEnumeration = dictionary.keys();
                while (keyEnumeration.hasMoreElements()) {
                    String key = String.valueOf(keyEnumeration.nextElement());
                    //file.install.filename needs to be skipped as it specific to the current container.
                    if (!key.equals(FILE_INSTALL_FILENAME_PROPERTY)) {
                        String value = String.valueOf(dictionary.get(key));
                        target.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error while importing configuration {} to profile.", pid);
        }
    }

    /**
     * Extracts Key value pairs from a delimited string of key value pairs.
     * Note: The value may contain commas.
     */
    private Map<String, String> extractConfigs(String configs) {
        Map<String, String> configMap = new HashMap<String, String>();
        //If contains key values.
        String key = null;
        String value = null;
        if (configs.contains("=")) {
            key = configs.substring(0, configs.indexOf("="));
            value = configs.substring(configs.indexOf("=") + 1);

        }  else {
            key = configs;
            value = null;
        }
        if (key != null && !key.isEmpty()) {
            configMap.put(key, value);
        }
        return configMap;
    }

    /**
     * Gets the {@link jline.Terminal} from the current session.
     */
    private Terminal getTerminal() throws Exception {
        Object terminalObject = session.get(".jline.terminal");
        if (terminalObject instanceof Terminal) {
            return (Terminal) terminalObject;

        }
        throw new IllegalStateException("Could not get Terminal from CommandSession.");
    }

    private Map<String, String> getConfigurationFromBuilder(ProfileBuilder builder, String pid) {
        Map<String, String> config = builder.getConfiguration(pid);
        return config != null ? new HashMap<>(config) : new HashMap<String, String>();
    }
}

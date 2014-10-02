/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.agent.service;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

import io.fabric8.agent.download.DownloadManager;
import io.fabric8.agent.model.Config;
import io.fabric8.agent.model.ConfigFile;
import io.fabric8.agent.model.Feature;
import io.fabric8.common.util.Files;
import io.fabric8.common.util.Strings;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureConfigInstaller {
    public static final String FABRIC_ZOOKEEPER_PID = "fabric.zookeeper.pid";
    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureConfigInstaller.class);
    private static final String CONFIG_KEY = "org.apache.karaf.features.configKey";

    private final ConfigurationAdmin configAdmin;
    private final DownloadManager manager;

    public FeatureConfigInstaller(ConfigurationAdmin configAdmin, DownloadManager manager) {
        this.configAdmin = configAdmin;
        this.manager = manager;
    }

    private String[] parsePid(String pid) {
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[]{pid, factoryPid};
        } else {
            return new String[]{pid, null};
        }
    }

    private Configuration createConfiguration(ConfigurationAdmin configurationAdmin,
                                              String pid, String factoryPid) throws IOException, InvalidSyntaxException {
        if (factoryPid != null) {
            return configurationAdmin.createFactoryConfiguration(factoryPid, null);
        } else {
            return configurationAdmin.getConfiguration(pid, null);
        }
    }

    private Configuration findExistingConfiguration(ConfigurationAdmin configurationAdmin,
                                                    String pid, String factoryPid) throws IOException, InvalidSyntaxException {
        String filter;
        if (factoryPid == null) {
            filter = "(" + Constants.SERVICE_PID + "=" + pid + ")";
        } else {
            String key = createConfigurationKey(pid, factoryPid);
            filter = "(" + CONFIG_KEY + "=" + key + ")";
        }
        Configuration[] configurations = configurationAdmin.listConfigurations(filter);
        if (configurations != null && configurations.length > 0) {
            return configurations[0];
        }
        return null;
    }

    void installFeatureConfigs(Feature feature) throws IOException, InvalidSyntaxException {
    	for (Config config : feature.getConfigurations()) {
			Properties props = config.getProperties();
			String[] pid = parsePid(config.getName());
			Configuration cfg = findExistingConfiguration(configAdmin, pid[0], pid[1]);
			if (cfg == null) {
				Dictionary<String, String> cfgProps = convertToDict(props);

				cfg = createConfiguration(configAdmin, pid[0], pid[1]);
				String key = createConfigurationKey(pid[0], pid[1]);
				cfgProps.put(CONFIG_KEY, key);
				cfg.update(cfgProps);
			} else if (config.isAppend()) {
				Dictionary<String,Object> properties = cfg.getProperties();
                // Ignore already managed configurations
                String fabricManagedPid = (String) properties.get(FABRIC_ZOOKEEPER_PID);
                if (Strings.isNotBlank(fabricManagedPid)) {
                    continue;
                }
				for (Enumeration<String> propKeys = properties.keys(); propKeys.hasMoreElements();) {
					String key = propKeys.nextElement();
					// remove existing entry, since it's about appending.
					if (props.containsKey(key)) {
						props.remove(key);
					} 
				}
				if (props.size() > 0) {
					// convert props to dictionary
					Dictionary<String, String> cfgProps = convertToDict(props);
					cfg.update(cfgProps);
				}
			}
		}
        for (ConfigFile configFile : feature.getConfigurationFiles()) {
            installConfigurationFile(configFile.getLocation(), configFile.getFinalname(), configFile.isOverride());
        }

    }

	private Dictionary<String, String> convertToDict(Properties props) {
		Dictionary<String, String> cfgProps = new Hashtable<>();
		for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			String val = props.getProperty(key);
			cfgProps.put(key, val);
		}
		return cfgProps;
	}

    private String createConfigurationKey(String pid, String factoryPid) {
        return factoryPid == null ? pid : pid + "-" + factoryPid;
    }

    private void installConfigurationFile(String fileLocation, String finalname, boolean override) throws IOException {
        String basePath = System.getProperty("karaf.base");

        if (finalname.contains("${")) {
            //remove any placeholder or variable part, this is not valid.
            int marker = finalname.indexOf("}");
            finalname = finalname.substring(marker + 1);
        }

        finalname = basePath + File.separator + finalname;

        File file = new File(finalname);
        if (file.exists()) {
            if (!override) {
                LOGGER.debug("Configuration file {} already exist, don't override it", finalname);
                return;
            } else {
                LOGGER.info("Configuration file {} already exist, overriding it", finalname);
            }
        } else {
            LOGGER.info("Creating configuration file {}", finalname);
        }

        // Grab the file which must have been previously downloaded
        File downloaded = manager.getProviders().get(fileLocation).getFile();
        Files.copy(downloaded, file);
    }
}

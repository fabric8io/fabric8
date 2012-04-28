/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.fabric.service.jclouds.internal;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import com.google.common.base.Strings;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.linkedin.zookeeper.client.IZKClient;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CloudUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudUtils.class);

    private static final String FACTORY_FILTER = "(service.factoryPid=%s)";


    private CloudUtils() {
        //Utility Class
    }

    /**
     * Creates a {@link Map} of Strings, which contain provider specific options.
     * It's used to read from commands multi value key value pairs '=' separated.
     *
     * @param options An array of Strings which represent '=' key values.
     * @return
     */
    public static Map<String, String> parseProviderOptions(String[] options) {
        Map<String, String> providerOptions = new HashMap<String, String>();
        if (options != null && options.length > 0) {
            for (String option : options) {
                if (option.contains("=")) {
                    String key = option.substring(0, option.indexOf("="));
                    String value = option.substring(option.lastIndexOf("=") + 1);
                    providerOptions.put(key, value);
                }
            }
        }
        return providerOptions;
    }

    public static void registerProvider(final IZKClient zooKeeper, final ConfigurationAdmin configurationAdmin, final String provider, final String identity, final String credential, final Map<String, String> props) throws Exception {
        Runnable registrationTask = new Runnable() {
            @Override
            public void run() {
                try {
                    Configuration configuration = findOrCreateFactoryConfiguration(configurationAdmin, "org.jclouds.compute", provider);
                    if (configuration != null) {
                        Dictionary dictionary = configuration.getProperties();
                        if (dictionary == null) {
                            dictionary = new Properties();
                        }
                        dictionary.put("provider", provider);
                        dictionary.put("credential", credential);
                        dictionary.put("identity", identity);
                        dictionary.put("credential-store", "zookeeper");
                        if (provider != null && provider.equals("aws-ec2") && props != null && props.containsKey("owner") && props.get("owner") != null) {
                            dictionary.put("jclouds.ec2.ami-owners", props.get("owner"));

                        }
                        configuration.update(dictionary);

                        if (zooKeeper.isConnected()) {
                            if (zooKeeper.exists(ZkPath.CLOUD_PROVIDER.getPath(provider)) == null) {
                                ZooKeeperUtils.create(zooKeeper, ZkPath.CLOUD_PROVIDER.getPath(provider));
                            }
                            ZooKeeperUtils.set(zooKeeper, ZkPath.CLOUD_PROVIDER_IDENTIY.getPath(provider), identity);
                            ZooKeeperUtils.set(zooKeeper, ZkPath.CLOUD_PROVIDER_CREDENTIAL.getPath(provider), credential);
                        } else {
                            System.out.println("Fabric has not been initialized. Provider registration is local to the current container.");
                        }
                    }
                } catch (Exception ex) {
                   //noop
                }
            }
        };
        new Thread(registrationTask).start();
    }

    /**
     * Search the configuration admin for the specified factoryPid that refers to the provider.
     *
     * @param factoryPid
     * @param provider
     * @return
     * @throws java.io.IOException
     */
    private static Configuration findOrCreateFactoryConfiguration(ConfigurationAdmin configurationAdmin, String factoryPid, String provider) throws IOException {
        Configuration configuration = null;
        if (configurationAdmin != null) {
            try {
                Configuration[] configurations = configurationAdmin.listConfigurations(String.format(FACTORY_FILTER, factoryPid));
                if (configurations != null) {
                    for (Configuration conf : configurations) {
                        Dictionary dictionary = conf.getProperties();
                        if (dictionary != null && provider.equals(dictionary.get("provider"))) {
                            return conf;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to lookup configuration admin for existing cloud providers.", e);
            }
            LOGGER.debug("No configuration found with factoryPid org.jclouds.compute. Creating new one.");
            configuration = configurationAdmin.createFactoryConfiguration(factoryPid, null);
        }
        return configuration;
    }
}

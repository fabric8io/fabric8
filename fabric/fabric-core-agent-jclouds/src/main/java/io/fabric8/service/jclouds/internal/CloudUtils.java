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

package io.fabric8.service.jclouds.internal;

import org.apache.curator.framework.CuratorFramework;
import io.fabric8.zookeeper.ZkPath;
import org.jclouds.compute.ComputeService;
import org.jclouds.karaf.core.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.create;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

public class CloudUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudUtils.class);

    private static final String FACTORY_FILTER = "(service.factoryPid=%s)";
    private static final String AMI_QUERY_FORMAT = "owner-id=%s;state=available;image-type=machine;root-device-type=ebs";


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

    public static void registerProvider(final CuratorFramework curator, final ConfigurationAdmin configurationAdmin, final String name, final String provider, final String identity, final String credential, final Map<String, String> props) throws Exception {
        Runnable registrationTask = new Runnable() {
            @Override
            public void run() {
                try {
                    Configuration configuration = findOrCreateFactoryConfiguration(configurationAdmin, "org.jclouds.compute",name, provider, null);
                    if (configuration != null) {
                        Dictionary dictionary = configuration.getProperties();
                        if (dictionary == null) {
                            dictionary = new Properties();
                        }
                        dictionary.put(Constants.NAME, name);
                        dictionary.put(Constants.PROVIDER, provider);
                        dictionary.put(Constants.CREDENTIAL, credential);
                        dictionary.put(Constants.IDENTITY, identity);
                        dictionary.put("credential-store", "zookeeper");
                        //This is set to workaround race conditions with ssh pk copies.
                        //Required workaround for some images (e.g. Red Hat) on Amazon EC2.
                        dictionary.put("jclouds.ssh.max-retries", "40");
                        if (provider != null && provider.equals("aws-ec2") && props != null && props.containsKey("owner") && props.get("owner") != null) {
                            dictionary.put("jclouds.ec2.ami-query", String.format(AMI_QUERY_FORMAT,props.get("owner")));
                            dictionary.put("jclouds.ec2.cc-ami-query", String.format(AMI_QUERY_FORMAT,props.get("owner")));
                        }
                        for (Map.Entry<String, String> entry : props.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            dictionary.put(key, value);
                        }

                        configuration.update(dictionary);

                        if (curator.getZookeeperClient().isConnected()) {
                            if (exists(curator, ZkPath.CLOUD_SERVICE.getPath(name)) == null) {
                                create(curator, ZkPath.CLOUD_SERVICE.getPath(name));
                            }
							Enumeration keys = dictionary.keys();
							while (keys.hasMoreElements()) {
								Object key = keys.nextElement();
								Object value = dictionary.get(key);
								if (!key.equals("service.pid") && !key.equals("service.factoryPid")) {
									setData(curator, ZkPath.CLOUD_SERVICE_PROPERTY.getPath(name, String.valueOf(key)), String.valueOf(value));
								}
							}
							for (Map.Entry<String, String> entry : props.entrySet()) {
                                String key = entry.getKey();
                                String value = entry.getValue();
                                if (!key.equals("service.pid") && !key.equals("service.factoryPid")) {
                                    setData(curator, ZkPath.CLOUD_SERVICE_PROPERTY.getPath(name, key), value);
                                }
                            }
                        } else {
                            System.out.println("Fabric has not been initialized. Provider registration will be local to the current container.");
                        }
                    }
                } catch (Exception ex) {
                    //noop
                }
            }
        };
        new Thread(registrationTask).start();
    }


    public static void registerApi(final CuratorFramework curator, final ConfigurationAdmin configurationAdmin, final String name, final String api, final String endpoint, final String identity, final String credential, final Map<String, String> props) throws Exception {
        Runnable registrationTask = new Runnable() {
            @Override
            public void run() {
                try {
                    Configuration configuration = findOrCreateFactoryConfiguration(configurationAdmin, "org.jclouds.compute", name, null, api);
                    if (configuration != null) {
                        Dictionary dictionary = configuration.getProperties();
                        if (dictionary == null) {
                            dictionary = new Properties();
                        }
                        dictionary.put(Constants.NAME, name);
                        dictionary.put(Constants.API, api);
                        dictionary.put(Constants.ENDPOINT, endpoint);
                        dictionary.put(Constants.CREDENTIAL, credential);
                        dictionary.put(Constants.IDENTITY, identity);
                        dictionary.put("credential-store", "zookeeper");
                        //This is set to workaround race conditions with ssh pk copies.
                        //Required workaround for some images (e.g. Red Hat) on Amazon EC2.
                        dictionary.put("jclouds.ssh.max-retries", "40");
                        if (api != null && api.equals("aws-ec2") && props != null && props.containsKey("owner") && props.get("owner") != null) {
                            dictionary.put("jclouds.ec2.ami-owners", props.get("owner"));

                        }
                        for (Map.Entry<String, String> entry : props.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            dictionary.put(key, value);
                        }

                        configuration.update(dictionary);

                        if (curator.getZookeeperClient().isConnected()) {
                            if (exists(curator, ZkPath.CLOUD_SERVICE.getPath(name)) == null) {
                                create(curator, ZkPath.CLOUD_SERVICE.getPath(name));
                            }

							Enumeration keys = dictionary.keys();
							while (keys.hasMoreElements()) {
								Object key = keys.nextElement();
								Object value = dictionary.get(key);
								if (!key.equals("service.pid") && !key.equals("service.factoryPid")) {
									setData(curator, ZkPath.CLOUD_SERVICE_PROPERTY.getPath(name, String.valueOf(key)), String.valueOf(value));
								}
							}

                            for (Map.Entry<String, String> entry : props.entrySet()) {
                                String key = entry.getKey();
                                String value = entry.getValue();
                                if (!key.equals("service.pid") && !key.equals("service.factoryPid")) {
                                    setData(curator, ZkPath.CLOUD_SERVICE_PROPERTY.getPath(name, key), value);
                                }
                            }
                        } else {
                            System.out.println("Fabric has not been initialized. Provider registration will be local to the current container.");
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
     * @param name
     * @param provider
     * @return
     * @throws java.io.IOException
     */
    private static Configuration findOrCreateFactoryConfiguration(ConfigurationAdmin configurationAdmin, String factoryPid, String name, String provider, String api) throws IOException {
        Configuration configuration = null;
        if (configurationAdmin != null) {
            try {
                Configuration[] configurations = configurationAdmin.listConfigurations(String.format(FACTORY_FILTER, factoryPid));
                if (configurations != null) {
                    for (Configuration conf : configurations) {
                        Dictionary dictionary = conf.getProperties();
                        //If id has been specified only try to match by id, ignore the rest.
                        if (dictionary != null && name != null) {
                            if (name.equals(dictionary.get(Constants.NAME))) {
                                return conf;
                            }
                        } else {
                            if (dictionary != null && provider != null && provider.equals(dictionary.get("provider"))) {
                                return conf;
                            }
                            if (dictionary != null && api != null && api.equals(dictionary.get("api"))) {
                                return conf;
                            }
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

    /**
     * Returns the compute service when it becomes registered to the OSGi service registry.
     *
     * @param name
     * @return
     */
    public static synchronized ComputeService waitForComputeService(BundleContext bundleContext, String name) {
        ComputeService computeService = null;
        try {
            for (int r = 0; r < 6; r++) {
                ServiceReference[] references = bundleContext.getAllServiceReferences(ComputeService.class.getName(), "("+Constants.NAME+"=" + name + ")");
                if (references != null && references.length > 0) {
                    computeService = (ComputeService) bundleContext.getService(references[0]);
                    return computeService;
                }
                Thread.sleep(10000L);
            }
        } catch (Exception e) {
            LOGGER.error("Error while waiting for service.", e);
        }
        return computeService;
    }
}

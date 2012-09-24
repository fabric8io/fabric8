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

package org.fusesource.fabric.service.jclouds;

import java.util.Dictionary;
import java.util.Enumeration;
import org.fusesource.fabric.zookeeper.ZkPath;
import org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils;
import org.jclouds.karaf.core.Constants;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link LifecycleListener} that makes sure that whenever it connect to a new ensemble, it updates it with the cloud
 * provider information that are present in the {@link ConfigurationAdmin}.
 *
 * A typical use case is when creating a cloud ensemble and join it afterwards to update it after the join, with the
 * cloud provider information, so that the provider doesn't have to be registered twice.
 *
 * If for any reason the new ensemble already has registered information for a provider, the provider will be skipped.
 */
public class CloudProviderBridge implements LifecycleListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProviderBridge.class);

    private static final String COMPUTE_FILTER = "(service.factoryPid=org.jclouds.compute)";
    private static final String BLOBSTORE_FILTER = "(service.factoryPid=org.jclouds.blobstore)";

    private ConfigurationAdmin configurationAdmin;
    private IZKClient zooKeeper;


    @Override
    public void onConnected() {
       registerProviders(COMPUTE_FILTER);
       registerProviders(BLOBSTORE_FILTER);
    }

    @Override
    public void onDisconnected() {

    }

    public void registerProviders(String filter) {
        try {
            Configuration[] configurations = configurationAdmin.listConfigurations(filter);
            if (configurations != null) {
                for (Configuration configuration : configurations) {
                    Dictionary properties = configuration.getProperties();
                    if (properties != null) {
                        String id = String.valueOf(properties.get(Constants.JCLOUDS_SERVICE_ID));
                        String identity = String.valueOf(properties.get("identity"));
                        String credential = String.valueOf(properties.get("credential"));
                        if (id != null && identity != null && credential != null && getZooKeeper().isConnected()) {
                            if (getZooKeeper().exists(ZkPath.CLOUD_SERVICE.getPath(id)) == null) {
                                ZooKeeperUtils.create(getZooKeeper(), ZkPath.CLOUD_SERVICE.getPath(id));

                                Enumeration keys = properties.keys();
                                while (keys.hasMoreElements()) {
                                    String key = String.valueOf(keys.nextElement());
                                    String value = String.valueOf(properties.get(key));
                                    ZooKeeperUtils.set(getZooKeeper(), ZkPath.CLOUD_SERVICE_PROPERTY.getPath(id,key), value);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve compute service information from configuration admin.", e);
        }
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public IZKClient getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(IZKClient zooKeeper) {
        this.zooKeeper = zooKeeper;
    }
}

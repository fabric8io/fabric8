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

package io.fabric8.service.jclouds;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.create;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.zookeeper.ZkPath;
import org.jclouds.karaf.core.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ConnectionStateListener} that makes sure that whenever it connect to a new ensemble, it updates it with the cloud
 * provider information that are present in the {
 * @link ConfigurationAdmin}.
 *
 * A typical use case is when creating a cloud ensemble and join it afterwards to update it after the join, with the
 * cloud provider information, so that the provider doesn't have to be registered twice.
 *
 * If for any reason the new ensemble already has registered information for a provider, the provider will be skipped.
 */
@ThreadSafe
@Component(name = "io.fabric8.jclouds.bridge", label = "Fabric8 Jclouds Service Bridge", immediate = true, metatype = false)
@Service(ConnectionStateListener.class)
public final class CloudProviderBridge extends AbstractComponent implements ConnectionStateListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProviderBridge.class);

    private static final String COMPUTE_FILTER = "(service.factoryPid=org.jclouds.compute)";
    private static final String BLOBSTORE_FILTER = "(service.factoryPid=org.jclouds.blobstore)";

    @Reference(referenceInterface = ConfigurationAdmin.class)
    private final ValidatingReference<ConfigurationAdmin> configAdmin = new ValidatingReference<ConfigurationAdmin>();
    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    @Activate
    void activate() {
        activateComponent();
    }

    @Deactivate
    void deactivate() {
        deactivateComponent();
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        if (isValid()) {
            switch (newState) {
            case CONNECTED:
            case RECONNECTED:
                // FIXME impl calls scr method
                this.curator.bind(client);
                onConnected();
                break;
            default:
                onDisconnected();
        }
        }
    }

    private void onConnected() {
       registerServices(COMPUTE_FILTER);
       registerServices(BLOBSTORE_FILTER);
    }

    private void onDisconnected() {
    }

    private void registerServices(String filter) {
        try {
            Configuration[] configurations = configAdmin.get().listConfigurations(filter);
            if (configurations != null) {
                for (Configuration configuration : configurations) {
                    Dictionary properties = configuration.getProperties();
                    if (properties != null) {
                        String name = properties.get(Constants.NAME) != null ? String.valueOf(properties.get(Constants.NAME)) : null;
                        String identity = properties.get(Constants.IDENTITY) != null ? String.valueOf(properties.get(Constants.IDENTITY)) : null;
                        String credential = properties.get(Constants.CREDENTIAL) != null ? String.valueOf(properties.get(Constants.CREDENTIAL)) : null;
                        if (name != null && identity != null && credential != null && curator.get().getZookeeperClient().isConnected()) {
                            if (exists(curator.get(), ZkPath.CLOUD_SERVICE.getPath(name)) == null) {
                                create(curator.get(), ZkPath.CLOUD_SERVICE.getPath(name));

                                Enumeration keys = properties.keys();
                                while (keys.hasMoreElements()) {
                                    String key = String.valueOf(keys.nextElement());
                                    String value = String.valueOf(properties.get(key));
                                    if (!key.equals("service.pid") && !key.equals("service.factoryPid")) {
                                        setData(curator.get(), ZkPath.CLOUD_SERVICE_PROPERTY.getPath(name, key), value);
                                    }
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

    void bindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.bind(service);
    }

    void unbindConfigAdmin(ConfigurationAdmin service) {
        this.configAdmin.unbind(service);
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }
}

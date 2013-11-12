
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.fusesource.fabric.zookeeper.curator;

import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;
import static org.apache.felix.scr.annotations.ReferencePolicy.DYNAMIC;
import static org.fusesource.fabric.zookeeper.curator.Constants.CONNECTION_TIMEOUT;
import static org.fusesource.fabric.zookeeper.curator.Constants.DEFAULT_CONNECTION_TIMEOUT_MS;
import static org.fusesource.fabric.zookeeper.curator.Constants.DEFAULT_RETRY_INTERVAL;
import static org.fusesource.fabric.zookeeper.curator.Constants.DEFAULT_SESSION_TIMEOUT_MS;
import static org.fusesource.fabric.zookeeper.curator.Constants.MAX_RETRIES_LIMIT;
import static org.fusesource.fabric.zookeeper.curator.Constants.RETRY_POLICY_INTERVAL_MS;
import static org.fusesource.fabric.zookeeper.curator.Constants.RETRY_POLICY_MAX_RETRIES;
import static org.fusesource.fabric.zookeeper.curator.Constants.SESSION_TIMEOUT;
import static org.fusesource.fabric.zookeeper.curator.Constants.ZOOKEEPER_PASSWORD;
import static org.fusesource.fabric.zookeeper.curator.Constants.ZOOKEEPER_URL;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.EnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.imps.CuratorFrameworkImpl;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.framework.state.ConnectionStateManager;
import org.apache.curator.retry.RetryNTimes;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import org.fusesource.fabric.api.Constants;
import org.fusesource.fabric.api.jcip.ThreadSafe;
import org.fusesource.fabric.api.scr.AbstractComponent;
import org.fusesource.fabric.api.scr.ValidatingReference;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.io.Closeables;

@ThreadSafe
@Component(name = Constants.ZOOKEEPER_CLIENT_PID, description = "Fabric ZooKeeper Client Factory", policy = ConfigurationPolicy.OPTIONAL, immediate = true)
public final class ManagedCuratorFramework extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedCuratorFramework.class);

    @Reference(referenceInterface = ACLProvider.class)
    private final ValidatingReference<ACLProvider> aclProvider = new ValidatingReference<ACLProvider>();
    @Reference(referenceInterface = ConnectionStateListener.class, bind = "bindConnectionStateListener", unbind = "unbindConnectionStateListener", cardinality = OPTIONAL_MULTIPLE, policy = DYNAMIC)
    private final Set<ConnectionStateListener> connectionStateListeners = new HashSet<ConnectionStateListener>();

    private final DynamicEnsembleProvider ensembleProvider = new DynamicEnsembleProvider();
    private BundleContext bundleContext;
    private CuratorFramework curatorFramework;
    private ServiceRegistration<CuratorFramework> registration;
    private Map<String, ?> oldProperties;

    @Activate
    void activate(BundleContext bundleContext, Map<String, ?> configuration) throws ConfigurationException {
        this.bundleContext = bundleContext;
        if ((configuration == null || !configuration.containsKey(ZOOKEEPER_URL)) && Strings.isNullOrEmpty(System.getProperty(ZOOKEEPER_URL))) {
            return;
        }
        updateService(buildCuratorFramework(configuration));
        this.oldProperties = configuration;
        activateComponent();
    }

    @Modified
    void modified(Map<String, ?> configuration) throws ConfigurationException {
        if ((configuration == null || !configuration.containsKey(ZOOKEEPER_URL)) && Strings.isNullOrEmpty(System.getProperty(ZOOKEEPER_URL))) {
            return;
        } else if (isRestartRequired(oldProperties, configuration)) {
            updateService(buildCuratorFramework(configuration));
        } else {
            updateEnsemble(configuration);
            if (!propertyEquals(oldProperties, configuration, ZOOKEEPER_URL)) {
                try {
                    reset((CuratorFrameworkImpl) curatorFramework);
                } catch (Exception e) {
                    LOGGER.error("Failed to update the ensemble url.", e);
                }
            }
        }
        this.oldProperties = configuration;
    }

    @Deactivate
    synchronized void deactivate() throws IOException {
        deactivateComponent();
        if (registration != null) {
            registration.unregister();
        }
        Closeables.close(curatorFramework, true);
    }

    /**
     * Updates the {@link org.osgi.framework.ServiceRegistration} with the new {@link org.apache.curator.framework.CuratorFramework}.
     * Un-registers previously registered services, closes previously created {@link org.apache.curator.framework.CuratorFramework} instances and
     * registers the new {@link org.apache.curator.framework.CuratorFramework}.
     */
    private void updateService(CuratorFramework framework) {
        if (registration != null) {
            registration.unregister();
            try {
                Closeables.close(curatorFramework, true);
            } catch (IOException ex) {
                //this should not happen as we swallow the exception.
            }
            registration = null;
        }
        // Delay the registration of the curator framework until it is connected
        ConnectionStateListener listener = new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                if (newState == ConnectionState.CONNECTED) {
                    synchronized (ManagedCuratorFramework.this) {
                        if (registration == null) {
                            registration = bundleContext.registerService(CuratorFramework.class, curatorFramework, null);
                            client.getConnectionStateListenable().removeListener(this);
                        }
                    }
                }
            }
        };
        framework.getConnectionStateListenable().addListener(listener);
        this.curatorFramework = framework;
        if (framework.getZookeeperClient().isConnected()) {
            listener.stateChanged(framework, ConnectionState.CONNECTED);
        }
    }

    /**
     * Force the framework to close the underlying zookeeper client and start a fresh one.
     * Thre method is used to enforce updating the ensemble url when neeeded.
     *
     * @param curator
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void reset(final CuratorFrameworkImpl curator) throws Exception {
        Field field = CuratorFrameworkImpl.class.getDeclaredField("connectionStateManager");
        field.setAccessible(true);
        ConnectionStateManager connectionStateManager = (ConnectionStateManager) field.get(curator);
        connectionStateManager.addStateChange(ConnectionState.LOST);
    }

    /**
     * Builds a {@link org.apache.curator.framework.CuratorFramework} from the specified {@link java.util.Map<String, ?>}.
     */
    private synchronized CuratorFramework buildCuratorFramework(Map<String, ?> properties) {
        String connectionString = readString(properties, ZOOKEEPER_URL, System.getProperty(ZOOKEEPER_URL, ""));
        ensembleProvider.update(connectionString);
        int sessionTimeoutMs = readInt(properties, SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT_MS);
        int connectionTimeoutMs = readInt(properties, CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT_MS);

        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .ensembleProvider(ensembleProvider)
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .retryPolicy(buildRetryPolicy(properties));

        if (isAuthorizationConfigured(properties)) {
            String scheme = "digest";
            String password = readString(properties, ZOOKEEPER_PASSWORD, System.getProperty(ZOOKEEPER_PASSWORD, ""));
            byte[] auth = ("fabric:" + password).getBytes();
            builder = builder.authorization(scheme, auth).aclProvider(aclProvider.get());
        }

        CuratorFramework framework = builder.build();

        for (ConnectionStateListener listener : connectionStateListeners) {
            framework.getConnectionStateListenable().addListener(listener);
        }

        framework.start();
        return framework;
    }

    /**
     * Updates the {@link EnsembleProvider} with the new connection string.
     */
    private void updateEnsemble(Map<String, ?> properties) {
        String connectionString = readString(properties, ZOOKEEPER_URL, "");
        ensembleProvider.update(connectionString);
    }

    /**
     * Builds an {@link org.apache.curator.retry.ExponentialBackoffRetry} from the {@link java.util.Map<String, ?>}.
     */
    private RetryPolicy buildRetryPolicy(Map<String, ?> properties) {
        int maxRetries = readInt(properties, RETRY_POLICY_MAX_RETRIES, MAX_RETRIES_LIMIT);
        int intervalMs = readInt(properties, RETRY_POLICY_INTERVAL_MS, DEFAULT_RETRY_INTERVAL);
        return new RetryNTimes(maxRetries, intervalMs);
    }


    /**
     * Returns true if configuration contains authorization configuration.
     */
    private boolean isAuthorizationConfigured(Map<String, ?> properties) {
        return ((properties != null
                && !Strings.isNullOrEmpty((String) properties.get(ZOOKEEPER_PASSWORD))
                || (!Strings.isNullOrEmpty(System.getProperty(ZOOKEEPER_PASSWORD)))));
    }


    /**
     * Returns true if configuration contains authorization configuration.
     */
    private boolean isRestartRequired(Map<String, ?> oldProperties, Map<String, ?> properties) {
        if (oldProperties == null || properties == null) {
            return true;
        } else if (oldProperties.equals(properties)) {
            return false;
        } else if (!propertyEquals(oldProperties, properties, ZOOKEEPER_URL)) {
            return true;
        } else if (!propertyEquals(oldProperties, properties, ZOOKEEPER_PASSWORD)) {
            return true;
        } else if (!propertyEquals(oldProperties, properties, CONNECTION_TIMEOUT)) {
            return true;
        } else if (!propertyEquals(oldProperties, properties, SESSION_TIMEOUT)) {
            return true;
        } else if (!propertyEquals(oldProperties, properties, RETRY_POLICY_MAX_RETRIES)) {
            return true;
        } else if (!propertyEquals(oldProperties, properties, RETRY_POLICY_INTERVAL_MS)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean propertyEquals(Map<String, ?> left, Map<String, ?> right, String name) {
        if (left == null || right == null || left.get(name) == null || right.get(name) == null) {
            return (left == null || left.get(name) == null) && (right == null || right.get(name) == null);
        } else {
            return left.get(name).equals(right.get(name));
        }
    }

    /**
     * Reads an String value from the specified configuration.
     *
     * @param props        The source props.
     * @param key          The key of the property to read.
     * @param defaultValue The default value.
     * @return The value read or the defaultValue if any error occurs.
     */
    private static String readString(Map<String, ?> props, String key, String defaultValue) {
        try {
            Object obj = props.get(key);
            if (obj instanceof String) {
                return (String) obj;
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Reads an int value from the specified configuration.
     *
     * @param props        The source props.
     * @param key          The key of the property to read.
     * @param defaultValue The default value.
     * @return The value read or the defaultValue if any error occurs.
     */
    private static int readInt(Map<String, ?> props, String key, int defaultValue) {
        try {
            Object obj = props.get(key);
            if (obj instanceof Number) {
                return ((Number) obj).intValue();
            } else if (obj instanceof String) {
                return Integer.parseInt((String) obj);
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    void bindConnectionStateListener(ConnectionStateListener connectionStateListener) {
        connectionStateListeners.add(connectionStateListener);
        if (curatorFramework != null) {
            curatorFramework.getConnectionStateListenable().addListener(connectionStateListener);
            //We want listeners to receive the connected event upon registration.
            if (curatorFramework.getZookeeperClient().isConnected()) {
                connectionStateListener.stateChanged(curatorFramework, ConnectionState.CONNECTED);
            }
        }
    }

    void unbindConnectionStateListener(ConnectionStateListener connectionStateListener) {
        connectionStateListeners.remove(connectionStateListener);
        if (curatorFramework != null) {
            curatorFramework.getConnectionStateListenable().removeListener(connectionStateListener);
        }
    }

    void bindAclProvider(ACLProvider aclProvider) {
        this.aclProvider.bind(aclProvider);
    }

    void unbindAclProvider(ACLProvider aclProvider) {
        this.aclProvider.unbind(aclProvider);
    }
}
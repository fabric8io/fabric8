
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

package io.fabric8.zookeeper.curator;

import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;
import static org.apache.felix.scr.annotations.ReferencePolicy.DYNAMIC;
import static io.fabric8.zookeeper.curator.Constants.CONNECTION_TIMEOUT;
import static io.fabric8.zookeeper.curator.Constants.DEFAULT_CONNECTION_TIMEOUT_MS;
import static io.fabric8.zookeeper.curator.Constants.DEFAULT_RETRY_INTERVAL;
import static io.fabric8.zookeeper.curator.Constants.DEFAULT_SESSION_TIMEOUT_MS;
import static io.fabric8.zookeeper.curator.Constants.MAX_RETRIES_LIMIT;
import static io.fabric8.zookeeper.curator.Constants.RETRY_POLICY_INTERVAL_MS;
import static io.fabric8.zookeeper.curator.Constants.RETRY_POLICY_MAX_RETRIES;
import static io.fabric8.zookeeper.curator.Constants.SESSION_TIMEOUT;
import static io.fabric8.zookeeper.curator.Constants.ZOOKEEPER_PASSWORD;
import static io.fabric8.zookeeper.curator.Constants.ZOOKEEPER_URL;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Reference;
import io.fabric8.api.Constants;
import io.fabric8.api.RuntimeProperties;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
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

    @Reference(referenceInterface = RuntimeProperties.class)
    private final ValidatingReference<RuntimeProperties> runtimeProperties = new ValidatingReference<RuntimeProperties>();
    @Reference(referenceInterface = ACLProvider.class)
    private final ValidatingReference<ACLProvider> aclProvider = new ValidatingReference<ACLProvider>();
    @Reference(referenceInterface = ConnectionStateListener.class, bind = "bindConnectionStateListener", unbind = "unbindConnectionStateListener", cardinality = OPTIONAL_MULTIPLE, policy = DYNAMIC)
    private final List<ConnectionStateListener> connectionStateListeners = new CopyOnWriteArrayList<ConnectionStateListener>();

    private BundleContext bundleContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private AtomicReference<State> state = new AtomicReference<State>();

    class State implements ConnectionStateListener, Runnable {
        final Map<String, ?> configuration;
        final AtomicBoolean closed = new AtomicBoolean();
        ServiceRegistration<CuratorFramework> registration;
        CuratorFramework curator;

        State(Map<String, ?> configuration) {
            this.configuration = configuration;
        }

        @Override
        public void run() {
            if (curator != null) {
                curator.getZookeeperClient().stop();
            }
            if (registration != null) {
                registration.unregister();
                registration = null;
            }
            try {
                Closeables.close(curator, true);
            } catch (IOException e) {
                // Should not happen
            }
            curator = null;
            if (!closed.get()) {
                curator = buildCuratorFramework(configuration);
                curator.getConnectionStateListenable().addListener(this, executor);
                if (curator.getZookeeperClient().isConnected()) {
                    stateChanged(curator, ConnectionState.CONNECTED);
                }
                CuratorFrameworkLocator.bindCurator(curator);
            }
        }

        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState) {
            if (newState == ConnectionState.CONNECTED) {
                if (registration == null) {
                    registration = bundleContext.registerService(CuratorFramework.class, curator, null);
                }
            }
            for (ConnectionStateListener listener : connectionStateListeners) {
                listener.stateChanged(client, newState);
            }
            if (newState == ConnectionState.LOST) {
                run();
            }
        }

        public void close() {
            closed.set(true);
            CuratorFramework curator = this.curator;
            if (curator != null) {
                curator.getZookeeperClient().stop();
            }
            try {
                executor.submit(this).get();
            } catch (Exception e) {
                LOGGER.warn("Error while closing curator", e);
            }
        }

    }

    @Activate
    void activate(BundleContext bundleContext, Map<String, ?> configuration) throws ConfigurationException {
        this.bundleContext = bundleContext;
        String zookeeperURL = getZookeeperURL(configuration);
        if (!Strings.isNullOrEmpty(zookeeperURL)) {
            State next = new State(configuration);
            if (state.compareAndSet(null, next)) {
                executor.submit(next);
            }
        }
        activateComponent();
    }

    @Modified
    void modified(Map<String, ?> configuration) throws ConfigurationException {
        String zookeeperURL = getZookeeperURL(configuration);
        if (!Strings.isNullOrEmpty(zookeeperURL)) {
            State prev = state.get();
            Map<String, ?> oldConfiguration = prev != null ? prev.configuration : null;
            if (isRestartRequired(oldConfiguration, configuration)) {
                State next = new State(configuration);
                if (state.compareAndSet(prev, next)) {
                    executor.submit(next);
                    if (prev != null) {
                        prev.close();
                    }
                } else {
                    next.close();
                }
            }
        }
    }

    @Deactivate
    void deactivate() throws IOException {
        deactivateComponent();
        State prev = state.getAndSet(null);
        if (prev != null) {
            CuratorFrameworkLocator.unbindCurator(prev.curator);
            prev.close();
        }
        executor.shutdownNow();
    }

    private String getZookeeperURL(Map<String, ?> configuration) {
        String zookeeperURL = null;
        if (configuration != null) {
            RuntimeProperties sysprops = runtimeProperties.get();
            zookeeperURL = (String) configuration.get(ZOOKEEPER_URL);
            zookeeperURL = Strings.isNullOrEmpty(zookeeperURL) ? sysprops.getProperty(ZOOKEEPER_URL) : zookeeperURL;
        }
        return zookeeperURL;
    }

    /**
     * Builds a {@link org.apache.curator.framework.CuratorFramework} from the specified {@link java.util.Map<String, ?>}.
     */
    private synchronized CuratorFramework buildCuratorFramework(Map<String, ?> properties) {
        RuntimeProperties sysprops = runtimeProperties.get();
        String connectionString = readString(properties, ZOOKEEPER_URL, sysprops.getProperty(ZOOKEEPER_URL, ""));
        int sessionTimeoutMs = readInt(properties, SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT_MS);
        int connectionTimeoutMs = readInt(properties, CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT_MS);

        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .ensembleProvider(new FixedEnsembleProvider(connectionString))
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .retryPolicy(buildRetryPolicy(properties));

        if (isAuthorizationConfigured(properties)) {
            String scheme = "digest";
            String password = readString(properties, ZOOKEEPER_PASSWORD, sysprops.getProperty(ZOOKEEPER_PASSWORD, ""));
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
        String zkpass = properties != null ? (String)properties.get(ZOOKEEPER_PASSWORD) : null;
        if (zkpass == null) {
            zkpass = runtimeProperties.get().getProperty(ZOOKEEPER_PASSWORD);
        }
        return !Strings.isNullOrEmpty(zkpass);
    }

    /**
     * Returns true if configuration contains authorization configuration.
     */
    private boolean isRestartRequired(Map<String, ?> oldProperties, Map<String, ?> properties) {
        if (!propertyEquals(oldProperties, properties, ZOOKEEPER_URL)) {
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
        State curr = state.get();
        CuratorFramework curator = curr != null ? curr.curator : null;
        if (curator != null && curator.getZookeeperClient().isConnected()) {
            connectionStateListener.stateChanged(curator, ConnectionState.CONNECTED);
        }
    }

    void unbindConnectionStateListener(ConnectionStateListener connectionStateListener) {
        connectionStateListeners.remove(connectionStateListener);
    }

    void bindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.bind(service);
    }

    void unbindRuntimeProperties(RuntimeProperties service) {
        this.runtimeProperties.unbind(service);
    }

    void bindAclProvider(ACLProvider aclProvider) {
        this.aclProvider.bind(aclProvider);
    }

    void unbindAclProvider(ACLProvider aclProvider) {
        this.aclProvider.unbind(aclProvider);
    }
}
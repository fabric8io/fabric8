
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

import io.fabric8.api.scr.Configurer;

import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.felix.scr.annotations.*;

import io.fabric8.api.Constants;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.AbstractComponent;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.zookeeper.bootstrap.BootstrapConfiguration;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.io.Closeables;

@ThreadSafe
@Component(name = Constants.ZOOKEEPER_CLIENT_PID, label = "Fabric8 ZooKeeper Client Factory", policy = ConfigurationPolicy.OPTIONAL, immediate = true, metatype = true)
public final class ManagedCuratorFramework extends AbstractComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedCuratorFramework.class);

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = ACLProvider.class)
    private final ValidatingReference<ACLProvider> aclProvider = new ValidatingReference<ACLProvider>();
    @Reference(referenceInterface = ConnectionStateListener.class, bind = "bindConnectionStateListener", unbind = "unbindConnectionStateListener", cardinality = OPTIONAL_MULTIPLE, policy = DYNAMIC)
    private final List<ConnectionStateListener> connectionStateListeners = new CopyOnWriteArrayList<ConnectionStateListener>();
    @Reference(referenceInterface = BootstrapConfiguration.class)
    private final ValidatingReference<BootstrapConfiguration> bootstrapConfiguration = new ValidatingReference<BootstrapConfiguration>();

    @Property(name = ZOOKEEPER_PASSWORD, label = "ZooKeeper Password", description = "The password used for ACL authentication", value = "${zookeeper.password}")
    private String zookeeperPassword;
    @Property(name = RETRY_POLICY_MAX_RETRIES, label = "Maximum Retries Number", description = "The number of retries on failed retry-able ZooKeeper operations", value = "${zookeeper.retry.max}")
    private int zookeeperRetryMax = MAX_RETRIES_LIMIT;
    @Property(name = RETRY_POLICY_INTERVAL_MS, label = "Retry Interval", description = "The amount of time to wait between retries", value = "${zookeeper.retry.interval}")
    private int zookeeperRetryInterval = DEFAULT_RETRY_INTERVAL;
    @Property(name = CONNECTION_TIMEOUT, label = "Connection Timeout", description = "The amount of time to wait in ms for connection", value = "${zookeeper.connection.timeout}")
    private int zookeeperConnectionTimeOut = DEFAULT_CONNECTION_TIMEOUT_MS;
    @Property(name = SESSION_TIMEOUT, label = "Session Timeout", description = "The amount of time to wait before timing out the session", value = "${zookeeper.session.timeout}")
    private int zookeeperSessionTimeout = DEFAULT_SESSION_TIMEOUT_MS;
    @Property(name = ZOOKEEPER_URL, label = "ZooKeeper URL", description = "The URL to the ZooKeeper Server(s)", value = "${zookeeper.url}")
    private String zookeeperUrl;

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
                curator = buildCuratorFramework();
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
    void activate(BundleContext bundleContext, Map<String, ?> configuration) throws Exception {
        this.bundleContext = bundleContext;
        configurer.configure(configuration, this);
        if (!Strings.isNullOrEmpty(zookeeperUrl)) {
            State next = new State(configuration);
            if (state.compareAndSet(null, next)) {
                executor.submit(next);
            }
        }
        activateComponent();
    }

    @Modified
    void modified(Map<String, ?> configuration) throws Exception {
        configurer.configure(configuration, this);
        if (!Strings.isNullOrEmpty(zookeeperUrl)) {
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

    /**
     * Builds a {@link org.apache.curator.framework.CuratorFramework} from the specified {@link java.util.Map<String, ?>}.
     */
    private synchronized CuratorFramework buildCuratorFramework() {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .ensembleProvider(new FixedEnsembleProvider(zookeeperUrl))
                .connectionTimeoutMs(zookeeperConnectionTimeOut)
                .sessionTimeoutMs(zookeeperSessionTimeout)
                .retryPolicy(new RetryNTimes(zookeeperRetryMax, zookeeperRetryInterval));

        if (!Strings.isNullOrEmpty(zookeeperPassword)) {
            String scheme = "digest";
            byte[] auth = ("fabric:" + zookeeperPassword).getBytes();
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

    void bindAclProvider(ACLProvider aclProvider) {
        this.aclProvider.bind(aclProvider);
    }

    void unbindAclProvider(ACLProvider aclProvider) {
        this.aclProvider.unbind(aclProvider);
    }

    void bindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.bind(service);
    }

    void unbindBootstrapConfiguration(BootstrapConfiguration service) {
        this.bootstrapConfiguration.unbind(service);
    }
}
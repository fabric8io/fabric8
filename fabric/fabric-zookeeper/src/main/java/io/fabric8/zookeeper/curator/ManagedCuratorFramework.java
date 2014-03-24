
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

import io.fabric8.utils.PasswordEncoder;
import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.felix.scr.annotations.*;

import io.fabric8.api.Constants;
import io.fabric8.api.ManagedCuratorFrameworkAvailable;
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
@Service(ManagedCuratorFrameworkAvailable.class)
@Properties(
        {
                @Property(name = ZOOKEEPER_URL, label = "ZooKeeper URL", description = "The URL to the ZooKeeper Server(s)", value = "${zookeeper.url}"),
                @Property(name = ZOOKEEPER_PASSWORD, label = "ZooKeeper Password", description = "The password used for ACL authentication", value = "${zookeeper.password}"),
                @Property(name = RETRY_POLICY_MAX_RETRIES, label = "Maximum Retries Number", description = "The number of retries on failed retry-able ZooKeeper operations", value = "${zookeeper.retry.max}"),
                @Property(name = RETRY_POLICY_INTERVAL_MS, label = "Retry Interval", description = "The amount of time to wait between retries", value = "${zookeeper.retry.interval}"),
                @Property(name = CONNECTION_TIMEOUT, label = "Connection Timeout", description = "The amount of time to wait in ms for connection", value = "${zookeeper.connection.timeout}"),
                @Property(name = SESSION_TIMEOUT, label = "Session Timeout", description = "The amount of time to wait before timing out the session", value = "${zookeeper.session.timeout}")
        }
)
public final class ManagedCuratorFramework extends AbstractComponent implements ManagedCuratorFrameworkAvailable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedCuratorFramework.class);

    @Reference
    private Configurer configurer;
    @Reference(referenceInterface = ACLProvider.class)
    private final ValidatingReference<ACLProvider> aclProvider = new ValidatingReference<ACLProvider>();
    @Reference(referenceInterface = ConnectionStateListener.class, bind = "bindConnectionStateListener", unbind = "unbindConnectionStateListener", cardinality = OPTIONAL_MULTIPLE, policy = DYNAMIC)
    private final List<ConnectionStateListener> connectionStateListeners = new CopyOnWriteArrayList<ConnectionStateListener>();
    @Reference(referenceInterface = BootstrapConfiguration.class)
    private final ValidatingReference<BootstrapConfiguration> bootstrapConfiguration = new ValidatingReference<BootstrapConfiguration>();

    private BundleContext bundleContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private AtomicReference<State> state = new AtomicReference<State>();

    class State implements ConnectionStateListener, Runnable {
        final CuratorConfig configuration;
        final AtomicBoolean closed = new AtomicBoolean();
        ServiceRegistration<CuratorFramework> registration;
        CuratorFramework curator;

        State(CuratorConfig configuration) {
            this.configuration = configuration;
        }

        public void run() {
            try {
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
                    curator.start();
                    CuratorFrameworkLocator.bindCurator(curator);
                }
            } catch (Throwable th) {
                LOGGER.error("Cannot start curator framework", th);
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
        CuratorConfig config = new CuratorConfig();
        configurer.configure(configuration, config);

        if (!Strings.isNullOrEmpty(config.getZookeeperUrl())) {
            State next = new State(config);
            if (state.compareAndSet(null, next)) {
                executor.submit(next);
            }
        }
        activateComponent();
    }

    @Modified
    void modified(Map<String, ?> configuration) throws Exception {
        CuratorConfig config = new CuratorConfig();
        configurer.configure(configuration, this);
        configurer.configure(configuration, config);

        if (!Strings.isNullOrEmpty(config.getZookeeperUrl())) {
            State prev = state.get();
            CuratorConfig oldConfiguration = prev != null ? prev.configuration : null;
            if (!config.equals(oldConfiguration)) {
                State next = new State(config);
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
    private synchronized CuratorFramework buildCuratorFramework(CuratorConfig curatorConfig) {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .ensembleProvider(new FixedEnsembleProvider(curatorConfig.getZookeeperUrl()))
                .connectionTimeoutMs(curatorConfig.getZookeeperConnectionTimeOut())
                .sessionTimeoutMs(curatorConfig.getZookeeperSessionTimeout())
                .retryPolicy(new RetryNTimes(curatorConfig.getZookeeperRetryMax(), curatorConfig.getZookeeperRetryInterval()));

        if (!Strings.isNullOrEmpty(curatorConfig.getZookeeperPassword())) {
            String scheme = "digest";
            byte[] auth = ("fabric:" + PasswordEncoder.decode(curatorConfig.getZookeeperPassword())).getBytes();
            builder = builder.authorization(scheme, auth).aclProvider(aclProvider.get());
        }

        CuratorFramework framework = builder.build();

        for (ConnectionStateListener listener : connectionStateListeners) {
            framework.getConnectionStateListenable().addListener(listener);
        }
        return framework;
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
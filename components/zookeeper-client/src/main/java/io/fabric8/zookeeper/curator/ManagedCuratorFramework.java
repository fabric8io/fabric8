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
package io.fabric8.zookeeper.curator;

import com.google.common.base.Strings;
import com.google.common.io.Closeables;
import io.fabric8.utils.PasswordEncoder;
import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class ManagedCuratorFramework {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedCuratorFramework.class);

    private final CuratorConfig config;
    private final ACLProvider aclProvider;

    private final List<ConnectionStateListener> connectionStateListeners = new CopyOnWriteArrayList<ConnectionStateListener>();

/*
    @Reference(referenceInterface = ConnectionStateListener.class, bind = "bindConnectionStateListener", unbind = "unbindConnectionStateListener", cardinality = OPTIONAL_MULTIPLE, policy = DYNAMIC)
    @Reference(referenceInterface = BootstrapConfiguration.class)
    private final ValidatingReference<BootstrapConfiguration> bootstrapConfiguration = new ValidatingReference<BootstrapConfiguration>();
*/

    private BundleContext bundleContext;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private AtomicReference<State> state = new AtomicReference<State>();

    //@Inject
    public ManagedCuratorFramework(CuratorConfig config, ACLProvider aclProvider) {
        this.config = config;
        this.aclProvider = aclProvider;

        if (!Strings.isNullOrEmpty(config.getZookeeperUrl())) {
            State next = new State(config);
            if (state.compareAndSet(null, next)) {
                executor.submit(next);
            }
        }
    }

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
            if (newState == ConnectionState.CONNECTED || newState == ConnectionState.READ_ONLY || newState == ConnectionState.RECONNECTED) {
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
                for (ConnectionStateListener listener : connectionStateListeners) {
                    listener.stateChanged(curator, ConnectionState.LOST);
                }
                curator.getZookeeperClient().stop();
            }
            try {
                executor.submit(this).get();
            } catch (Exception e) {
                LOGGER.warn("Error while closing curator", e);
            }
        }

    }

    @PreDestroy
    void destroy() throws IOException {
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
        ACLProvider aclProviderInstance = aclProvider;
        List<ConnectionStateListener> connectionListenerList = connectionStateListeners;
        return createCuratorFramework(curatorConfig, aclProviderInstance, connectionListenerList);
    }

    public static CuratorFramework createCuratorFramework(CuratorConfig curatorConfig, ACLProvider aclProviderInstance, List<ConnectionStateListener> connectionListenerList) {
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .canBeReadOnly(true)
                .ensembleProvider(new FixedEnsembleProvider(curatorConfig.getZookeeperUrl()))
                .connectionTimeoutMs(curatorConfig.getZookeeperConnectionTimeOut())
                .sessionTimeoutMs(curatorConfig.getZookeeperSessionTimeout())
                .retryPolicy(new RetryNTimes(curatorConfig.getZookeeperRetryMax(), curatorConfig.getZookeeperRetryInterval()));

        if (!Strings.isNullOrEmpty(curatorConfig.getZookeeperPassword())) {
            String scheme = "digest";
            byte[] auth = ("fabric:" + PasswordEncoder.decode(curatorConfig.getZookeeperPassword())).getBytes();
            builder = builder.authorization(scheme, auth).aclProvider(aclProviderInstance);
        }

        CuratorFramework framework = builder.build();

        for (ConnectionStateListener listener : connectionListenerList) {
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
}

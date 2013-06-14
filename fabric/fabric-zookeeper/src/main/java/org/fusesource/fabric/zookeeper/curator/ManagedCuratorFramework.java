
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

import com.google.common.base.Strings;
import com.google.common.io.Closeables;
import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.EnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.framework.imps.CuratorFrameworkImpl;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.framework.state.ConnectionStateManager;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.fusesource.fabric.zookeeper.curator.Constants.ACL_PROVIDER;
import static org.fusesource.fabric.zookeeper.curator.Constants.CONNECTION_TIMEOUT;
import static org.fusesource.fabric.zookeeper.curator.Constants.DEFAULT_CONNECTION_TIMEOUT_MS;
import static org.fusesource.fabric.zookeeper.curator.Constants.DEFAULT_BASE_SLEEP_MS;
import static org.fusesource.fabric.zookeeper.curator.Constants.DEFAULT_MAX_SLEEP_MS;
import static org.fusesource.fabric.zookeeper.curator.Constants.DEFAULT_SESSION_TIMEOUT_MS;
import static org.fusesource.fabric.zookeeper.curator.Constants.MAX_RETRIES_LIMIT;
import static org.fusesource.fabric.zookeeper.curator.Constants.RETRY_POLICY_BASE_SLEEP_TIME_MS;
import static org.fusesource.fabric.zookeeper.curator.Constants.RETRY_POLICY_MAX_RETRIES;
import static org.fusesource.fabric.zookeeper.curator.Constants.RETRY_POLICY_MAX_SLEEP_TIME_MS;
import static org.fusesource.fabric.zookeeper.curator.Constants.SESSION_TIMEOUT;
import static org.fusesource.fabric.zookeeper.curator.Constants.ZOOKEEPER_PASSWORD;
import static org.fusesource.fabric.zookeeper.curator.Constants.ZOOKEEPER_URL;
import static org.fusesource.fabric.zookeeper.curator.TrackerUtils.closeQuietly;


public class ManagedCuratorFramework implements ManagedService, Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedCuratorFramework.class);

    private final BundleContext bundleContext;
    private final Map<String, ACLProvider> aclProviders = new HashMap<String, ACLProvider>();
    private final DynamicEnsembleProvider ensembleProvider = new DynamicEnsembleProvider();
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    private CuratorFramework curatorFramework;
    private ServiceRegistration registration;

    private ServiceTracker<ConnectionStateListener, ConnectionStateListener> connectionStateListenerTracker;
    private ServiceTracker<ACLProvider, ACLProvider> aclProviderTracker;
    private Dictionary oldProperties;

    public ManagedCuratorFramework(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
        registerAclProviderTracker();
    }

    @Override
    public void updated(Dictionary properties) throws ConfigurationException {
        if (isRestartRequired(oldProperties, properties)) {
            updateService(buildCuratorFramework(properties));
        } else {
            updateEnsemble(properties);
            if (!propertyEquals(oldProperties, properties, ZOOKEEPER_URL)) {
                try {
                    reset((CuratorFrameworkImpl) curatorFramework);
                } catch (Exception e) {
                    LOGGER.error("Failed to update the ensemble url.", e);
                }
            }
        }
        this.oldProperties = properties;
    }


    /**
     * Updates the {@link org.osgi.framework.ServiceRegistration} with the new {@link org.apache.curator.framework.CuratorFramework}.
     * Un-registers previously registered services, closes previously created {@link org.apache.curator.framework.CuratorFramework} instances and
     * registers the new {@link org.apache.curator.framework.CuratorFramework}.
     *
     * @param framework
     */
    void updateService(CuratorFramework framework) {
        if (registration != null) {
            closeQuietly(connectionStateListenerTracker);
            registration.unregister();
            try {
                Closeables.close(curatorFramework, true);
            } catch (IOException ex) {
                //this should not happen as we swallow the excpetion.
            }
        }
        this.registration = bundleContext.registerService(CuratorFramework.class.getName(), framework, new Hashtable<String, Object>());
        this.curatorFramework = framework;
        registerConnectionStateListenerTracker();
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
     * Creates a new {@link org.osgi.util.tracker.ServiceTracker} for tracking {@link org.apache.curator.framework.state.ConnectionStateListener} services.
     * The services will be registered to the {@link org.apache.curator.framework.CuratorFramework}.
     * Each call to the method will close previously opened trackers and open a new one.
     */
    void registerConnectionStateListenerTracker() {
        try {
            closeQuietly(connectionStateListenerTracker);
            this.connectionStateListenerTracker = new ServiceTracker<ConnectionStateListener, ConnectionStateListener>(bundleContext,
                    ConnectionStateListener.class,
                    new CuratorStateChangeListenerTracker(bundleContext,
                            curatorFramework, executor));
            this.connectionStateListenerTracker.open();
        } catch (Exception ex) {
            LOGGER.warn("Failed to register connection state listener tracker.");
        }
    }

    void registerAclProviderTracker() {
        try {
            closeQuietly(aclProviderTracker);
            this.aclProviderTracker = new ServiceTracker<ACLProvider, ACLProvider>(bundleContext,
                    ACLProvider.class,
                    new ACLProviderTracker(bundleContext, aclProviders));
            this.aclProviderTracker.open();
        } catch (Exception ex) {
            LOGGER.warn("Failed to register acl provider tracker.");
        }
    }

    /**
     * Builds a {@link org.apache.curator.framework.CuratorFramework} from the specified {@link java.util.Dictionary}.
     *
     * @param properties
     * @return
     */
    CuratorFramework buildCuratorFramework(Dictionary properties) {
        String connectionString = readString(properties, ZOOKEEPER_URL, System.getProperty(ZOOKEEPER_URL, ""));
        ensembleProvider.update(connectionString);
        String aclProviderId = readString(properties, ACL_PROVIDER, "");
        int sessionTimeoutMs = readInt(properties, SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT_MS);
        int connectionTimeoutMs = readInt(properties, CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT_MS);

        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .ensembleProvider(ensembleProvider)
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .retryPolicy(buildRetryPolicy(properties));

        if (isAuthorizationConfigured(properties)) {
            String scheme = "digest";
            String password = readString(properties, ZOOKEEPER_PASSWORD, System.getProperty(ZOOKEEPER_PASSWORD,""));
            byte[] auth = ("fabric:" + password).getBytes();
            builder = builder.authorization(scheme, auth);
        }

        if (!Strings.isNullOrEmpty(aclProviderId) && aclProviders.containsKey(aclProviderId)) {
            builder = builder.aclProvider(aclProviders.get(aclProviderId));
        }

        CuratorFramework framework = builder.build();
        framework.start();
        return framework;
    }

    /**
     * Updates the {@link EnsembleProvider} with the new connection string.
     *
     * @param properties
     */
    private void updateEnsemble(Dictionary properties) {
        String connectionString = readString(properties, ZOOKEEPER_URL, "");
        ensembleProvider.update(connectionString);
    }

    /**
     * Builds an {@link org.apache.curator.retry.ExponentialBackoffRetry} from the {@link java.util.Dictionary}.
     *
     * @param properties
     * @return
     */
    RetryPolicy buildRetryPolicy(Dictionary properties) {
        int maxRetries = readInt(properties, RETRY_POLICY_MAX_RETRIES, MAX_RETRIES_LIMIT);
        int baseSleepTimeMS = readInt(properties, RETRY_POLICY_BASE_SLEEP_TIME_MS, DEFAULT_BASE_SLEEP_MS);
        int maxSleepTimeMS = readInt(properties, RETRY_POLICY_MAX_SLEEP_TIME_MS, DEFAULT_MAX_SLEEP_MS);
        return new ExponentialBackoffRetry(baseSleepTimeMS, maxRetries, maxSleepTimeMS);
    }


    /**
     * Returns true if configuration contains authorization configuration.
     *
     * @param properties
     * @return
     */
    boolean isAuthorizationConfigured(Dictionary properties) {
        return properties != null
                && !Strings.isNullOrEmpty((String) properties.get(ZOOKEEPER_PASSWORD));
    }


    /**
     * Returns true if configuration contains authorization configuration.
     *
     * @param properties
     * @return
     */
    boolean isRestartRequired(Dictionary oldProperties, Dictionary properties) {
        if (oldProperties == null || properties == null) {
            return true;
        } else if (oldProperties.equals(properties)) {
            return false;
        } else if (!propertyEquals(oldProperties, properties, ZOOKEEPER_PASSWORD)) {
            return true;
        } else if (!propertyEquals(oldProperties, properties, CONNECTION_TIMEOUT)) {
            return true;
        } else if (!propertyEquals(oldProperties, properties, SESSION_TIMEOUT)) {
            return true;
        } else if (!propertyEquals(oldProperties, properties, RETRY_POLICY_MAX_RETRIES)) {
            return true;
        } else if (!propertyEquals(oldProperties, properties, RETRY_POLICY_BASE_SLEEP_TIME_MS)) {
            return true;
        } else if (!propertyEquals(oldProperties, properties, RETRY_POLICY_MAX_SLEEP_TIME_MS)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean propertyEquals(Dictionary left, Dictionary right, String name) {
        if (left == null || right == null || left.get(name) == null || right.get(name) == null) {
            return (left == null || left.get(name) == null) && (right == null || right.get(name) == null);
        } else {
            return left.get(name).equals(right.get(name));
        }
    }

    /**
     * Reads an String value from the specified configuration.
     *
     * @param dictionary   The source dictionary.
     * @param key          The key of the property to read.
     * @param defaultValue The default value.
     * @return The value read or the defaultValue if any error occurs.
     */
    private static String readString(Dictionary dictionary, String key, String defaultValue) {
        try {
            Object obj = dictionary.get(key);
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
     * @param dictionary   The source dictionary.
     * @param key          The key of the property to read.
     * @param defaultValue The default value.
     * @return The value read or the defaultValue if any error occurs.
     */
    private static int readInt(Dictionary dictionary, String key, int defaultValue) {
        try {
            Object obj = dictionary.get(key);
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

    /**
     * Reads a byte array from the specified configuration.
     *
     * @param dictionary
     * @param key
     * @param defaultValue
     * @return
     */
    private static byte[] readBytes(Dictionary dictionary, String key, byte[] defaultValue) {
        try {
            Object obj = dictionary.get(key);
            if (obj instanceof byte[]) {
                return (byte[]) obj;
            } else if (obj instanceof String) {
                return ((String) obj).getBytes();
            } else {
                return defaultValue;
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public void close() throws IOException {
        registration.unregister();
        closeQuietly(connectionStateListenerTracker);
        closeQuietly(aclProviderTracker);
        Closeables.close(curatorFramework, true);
        executor.shutdownNow();
    }
}
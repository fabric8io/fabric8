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

package io.fabric8.service.jclouds.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import io.fabric8.api.jcip.ThreadSafe;
import io.fabric8.api.scr.Validatable;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.api.scr.ValidationSupport;
import io.fabric8.zookeeper.ZkPath;
import org.jclouds.domain.Credentials;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.karaf.core.CredentialStore;
import org.jclouds.rest.ConfiguresCredentialStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.fabric8.zookeeper.utils.ZooKeeperUtils.deleteSafe;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.exists;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getChildren;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.getStringData;
import static io.fabric8.zookeeper.utils.ZooKeeperUtils.setData;

/**
 * A {@link CredentialStore} backed by Zookeeper.
 * This module supports up to 100 node credential store in memory.
 * Credentials stored in memory will be pushed to Zookeeper when it becomes available.
 */
@ThreadSafe
@Component(name = "io.fabric8.jclouds.credential.store.zookeeper", label = "Fabric8 Jclouds ZooKeeper Credential Store", immediate = true, metatype = false)
@Service({CredentialStore.class, ConnectionStateListener.class})
@ConfiguresCredentialStore
public final class ZookeeperCredentialStore extends CredentialStore implements ConnectionStateListener, Validatable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperCredentialStore.class);

    @Reference(referenceInterface = CuratorFramework.class)
    private final ValidatingReference<CuratorFramework> curator = new ValidatingReference<CuratorFramework>();

    // No synchronization needed, both of these are thread safe
    private final Cache<String, Credentials> cache = CacheBuilder.newBuilder().maximumSize(100).build();
    private final ValidationSupport active = new ValidationSupport();

    // Needed for thread safe access to the underlying {@link CredentialStore}
    private final Object storeLock = new Object();

    @Activate
    void activate() {
        setStore(new ZookeeperBacking(curator.get(), cache));
        active.setValid();
    }

    @Deactivate
    void deactivate() {
        active.setInvalid();
        cache.cleanUp();
    }

    @Override
    public boolean isValid() {
        return active.isValid();
    }

    @Override
    public void assertValid() {
        active.assertValid();
    }

    /**
     * The underlying credential store is not thread safe
     * Use this accessor instead of the protected 'store' field
     */
    @Override
    public Map<String, Credentials> getStore() {
        synchronized (storeLock) {
            return super.getStore();
        }
    }

    /**
     * The underlying credential store is not thread safe
     * Use this accessor instead of the protected 'store' field
     */
    @Override
    public void setStore(Map<String, Credentials> store) {
        synchronized (storeLock) {
            super.setStore(store);
        }
    }

    @Override
    public void stateChanged(CuratorFramework client, ConnectionState newState) {
        if (isValid()) {
            switch (newState) {
            case CONNECTED:
            case RECONNECTED:
                // FIXME impl call SCR method
                this.curator.bind(client);
                onConnected();
                break;
            default:
                onDisconnected();
        }
        }
    }

    private void onConnected() {
        //Whenever a connection to Zookeeper is made copy everything to Zookeeper.
        for (Map.Entry<String, Credentials> entry : cache.asMap().entrySet()) {
            String s = entry.getKey();
            Credentials credentials = entry.getValue();
            getStore().put(s, credentials);
        }
    }

    private void onDisconnected() {
    }

    void bindCurator(CuratorFramework curator) {
        this.curator.bind(curator);
    }

    void unbindCurator(CuratorFramework curator) {
        this.curator.unbind(curator);
    }

    /**
     * A map implementations which uses a local {@link Cache} and Zookeeper as persistent getStore().
     * [FIXME] Does not preserve data integrity (i.e. updates on the cache may succeed and fail on the ZK backing store)
     */
    @ThreadSafe
    private static final class ZookeeperBacking implements Map<String, Credentials> {

        private final CuratorFramework curator;
        private final Cache<String, Credentials> cache;

        private ZookeeperBacking(CuratorFramework curator, Cache<String, Credentials> cache) {
            this.curator = curator;
            this.cache = cache;
        }

        /**
         * Returns the size of the getStore().
         * If zookeeper is connected it returns the size of the zookeeper store, else it falls back to the cache.
         */
        public synchronized int size() {
            int size = 0;
            {
                try {
                    if (exists(curator, ZkPath.CLOUD_NODES.getPath()) != null) {
                        size = getChildren(curator, ZkPath.CLOUD_NODES.getPath()).size();
                    }
                } catch (Exception ex) {
                    size = (int) cache.size();
                }
            }
            return size;
        }

        public synchronized boolean isEmpty() {
            return size() == 0;
        }

        /**
         * Checks if {@link Cache} container the key and if not it checks the Zookeeper (if connected).
         */
        public synchronized boolean containsKey(Object o) {
            boolean result  = cache.asMap().containsKey(o);
            //If not found in the cache check the zookeeper if available.
            if (!result) {
                try {
                    result = (exists(curator, ZkPath.CLOUD_NODE.getPath(normalizeKey(o))) != null);
                } catch (Exception ex) {
                    //noop
                }
            }
            return result;
        }

        /**
         * Never used, always returns false.
         */
        public synchronized boolean containsValue(Object o) {
            return false;
        }

        /**
         * Gets the {@link Credentials} of the corresponding key from the {@link Cache}.
         * If the {@link Credentials} are not found, then it checks the Zookeeper.
         */
        public synchronized Credentials get(Object o) {
            Credentials credentials = cache.asMap().get(o);
            if (credentials == null) {
                try {
                    String identity = getStringData(curator, ZkPath.CLOUD_NODE_IDENTITY.getPath(normalizeKey(o)));
                    String credential = getStringData(curator, ZkPath.CLOUD_NODE_CREDENTIAL.getPath(normalizeKey(o)));
                    credentials = LoginCredentials.fromCredentials(new Credentials(identity, credential));
                } catch (Exception e) {
                    LOGGER.debug("Failed to read jclouds credentials from zookeeper due to {}.", e.getMessage());
                }
            }
            return credentials;

        }

        /**
         * Puts {@link Credentials} both in {@link Cache} and the Zookeeper.
         */
        public synchronized Credentials put(String s, Credentials credentials) {
            cache.put(s, credentials);
            try {
                setData(curator, ZkPath.CLOUD_NODE_IDENTITY.getPath(normalizeKey(s)), credentials.identity);
                setData(curator, ZkPath.CLOUD_NODE_CREDENTIAL.getPath(normalizeKey(s)), credentials.credential);
            } catch (Exception e) {
                LOGGER.warn("Failed to store jclouds credentials to zookeeper.", e);
            }
            return credentials;
        }

        /**
         * Removes {@link Credentials} for {@link Cache} and Zookeeper.
         */
        public synchronized Credentials remove(Object o) {
            Credentials credentials = cache.asMap().remove(o);
            try {
                if (credentials == null) {
                    credentials = get(o);
                }
                String normalizedKey = normalizeKey(o);
                deleteSafe(curator, ZkPath.CLOUD_NODE_IDENTITY.getPath(normalizedKey));
                deleteSafe(curator, ZkPath.CLOUD_NODE_CREDENTIAL.getPath(normalizedKey));
            } catch (Exception e) {
                LOGGER.warn("Failed to remove jclouds credentials to zookeeper.", e);
            }
            return credentials;
        }

        /**
         * Puts all {@link Map} {@link Entry} to the {@link Cache} and Zookeeper.
         * @param map
         */
        public synchronized void putAll(Map<? extends String, ? extends Credentials> map) {
            for (Map.Entry<? extends String, ? extends Credentials> entry : map.entrySet()) {
                String s = entry.getKey();
                Credentials credential = entry.getValue();
                put(s, credential);
            }
        }

        public synchronized void clear() {
            cache.cleanUp();
            try {
                for (String nodeId : keySet()) {
                    deleteSafe(curator, ZkPath.CLOUD_NODE_IDENTITY.getPath(nodeId));
                    deleteSafe(curator, ZkPath.CLOUD_NODE_CREDENTIAL.getPath(nodeId));
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to clear zookeeper jclouds credentials store.", e);
            }
        }

        /**
         * Clears {@link Cache} and Zookeeper from all {@link Credentials}.
         */
        public synchronized Set<String> keySet() {
            Set<String> keys = new HashSet<String>();
                try {
                    keys = new HashSet<String>(getChildren(curator, ZkPath.CLOUD_NODE.getPath()));
                } catch (Exception e) {
                    LOGGER.warn("Failed to read from zookeeper jclouds credentials store.", e);
                    keys = cache.asMap().keySet();
                }
            return keys;
        }

        public synchronized Collection<Credentials> values() {
            List<Credentials> credentialsList = new LinkedList<Credentials>();
            for (String key : keySet()) {
                credentialsList.add(get(key));
            }
            return credentialsList;
        }

        public synchronized Set<Map.Entry<String, Credentials>> entrySet() {
            Set<Map.Entry<String, Credentials>> entrySet = new HashSet<Entry<String, Credentials>>();

                for (String key : keySet()) {
                    entrySet.add(new CredentialsEntry(curator, key));
                }
            return entrySet;
        }
    }

    private static class CredentialsEntry implements Map.Entry<String, Credentials> {

        private final String key;
        private final CuratorFramework curator;

        private CredentialsEntry(CuratorFramework curator, String key) {
            this.curator = curator;
            this.key = key;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Credentials getValue() {
            Credentials credentials = null;
            try {
                String identity = getStringData(curator, ZkPath.CLOUD_NODE_IDENTITY.getPath(normalizeKey(key)));
                String credential = getStringData(curator, ZkPath.CLOUD_NODE_CREDENTIAL.getPath(normalizeKey(key)));
                credentials = LoginCredentials.fromCredentials(new Credentials(identity, credential));
            } catch (Exception e) {
                LOGGER.debug("Failed to read jclouds credentials from zookeeper due to {}.", e.getMessage());
            }
            return credentials;
        }

        @Override
        public Credentials setValue(Credentials credentials) {
            try {
                setData(curator, ZkPath.CLOUD_NODE_IDENTITY.getPath(normalizeKey(key)), credentials.identity);
                setData(curator, ZkPath.CLOUD_NODE_CREDENTIAL.getPath(normalizeKey(key)), credentials.credential);
            } catch (Exception e) {
                LOGGER.warn("Failed to store jclouds credentials to zookeeper.", e);
            }
            return credentials;
        }
    }

    private static String normalizeKey(Object key) {
        String result = String.valueOf(key);
        return result.replaceAll("node#", "").replaceAll("#","");
    }
}

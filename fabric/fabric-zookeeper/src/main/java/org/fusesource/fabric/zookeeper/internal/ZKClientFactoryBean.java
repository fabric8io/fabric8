/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.fabric.zookeeper.internal;

import java.util.ArrayList;
import java.util.List;

import org.apache.zookeeper.Watcher;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.linkedin.zookeeper.client.ZKClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory bean of ZooKeeper client objects
 */
public class ZKClientFactoryBean {
    private static final transient Logger LOG = LoggerFactory.getLogger(ZKClientFactoryBean.class);

    private String connectString = "";
    private String timeoutText = "30s";
    private Watcher watcher;
    private List<LifecycleListener> listeners = new ArrayList<LifecycleListener>();
    private Timespan timeout;
    protected ZKClient zkClient;
    private Timespan connectTimeout;
    private String connectTimeoutText = "10s";
    private List<LifecycleListener> dynListeners = new ArrayList<LifecycleListener>();


    public String getConnectString() {
        return connectString;
    }

    public void setConnectString(String connectString) {
        this.connectString = connectString;
    }

    public Timespan getTimeout() {
        if (timeout == null) {
            timeout = Timespan.parse(timeoutText);
        }
        return timeout;
    }

    public void setTimeout(Timespan timeout) {
        this.timeout = timeout;
    }

    public String getTimeoutText() {
        return timeoutText;
    }

    public void setTimeoutText(String timeoutText) {
        this.timeoutText = timeoutText;
    }

    public Watcher getWatcher() {
        return watcher;
    }

    public void setWatcher(Watcher watcher) {
        this.watcher = watcher;
    }

    public Timespan getConnectTimeout() {
        if (connectTimeout == null) {
            connectTimeout = Timespan.parse(connectTimeoutText);
        }
        return connectTimeout;
    }

    public void setConnectTimeout(Timespan connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public String getConnectTimeoutText() {
        return connectTimeoutText;
    }

    public void setConnectTimeoutText(String connectTimeoutText) {
        this.connectTimeoutText = connectTimeoutText;
    }

    public List<LifecycleListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<LifecycleListener> listeners) {
        this.listeners = listeners;
    }

    public synchronized void registerListener(LifecycleListener listener) {
        if (dynListeners.add(listener)) {
            if (zkClient != null) {
                zkClient.registerListener(listener);
            }
        }
    }

    public synchronized void unregisterListener(LifecycleListener listener) {
        if (dynListeners.remove(listener)) {
            if (zkClient != null) {
                zkClient.removeListener(listener);
            }
        }
    }


    // FactoryBean interface
    //-------------------------------------------------------------------------
    public synchronized IZKClient getObject() throws Exception {
        LOG.debug("Connecting to ZooKeeper at " + connectString);

        zkClient = new ZKClient(connectString, getTimeout(), watcher);

        if (connectString.length() == 0) {
            LOG.info("No ZooKeeper URL provided. No connection attempted.");
            return zkClient;
        }

        zkClient.registerListener(new LifecycleListener() {

            final String address = connectString;

            @Override
            public void onConnected() {
                LOG.debug("Connected to Zookeeper at " + address);
            }

            @Override
            public void onDisconnected() {
                LOG.debug("Disconnected from ZooKeeper at " + address);
            }
        });

        for (LifecycleListener listener : dynListeners) {
            zkClient.registerListener(listener);
        }
        for (LifecycleListener listener : listeners) {
            if (listener instanceof ZooKeeperAware) {
                ((ZooKeeperAware) listener).setZooKeeper(zkClient);
            }
            zkClient.registerListener(listener);
        }
        zkClient.start();
        return zkClient;
    }

    public Class<?> getObjectType() {
        return IZKClient.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public synchronized void destroy() throws Exception {
        if (zkClient != null) {
            for (LifecycleListener listener : dynListeners) {
                zkClient.removeListener(listener);
            }
            for (LifecycleListener listener : listeners) {
                zkClient.removeListener(listener);
            }
            zkClient.destroy();
            zkClient = null;
        }

    }
}

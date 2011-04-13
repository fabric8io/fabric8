/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper;

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

    private String connectString = "localhost:2181";
    private String timeoutText = "30s";
    private Watcher watcher;
    private List<LifecycleListener> listeners;
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
        LOG.debug("Connecting to ZooKeeper on " + connectString);

        zkClient = new ZKClient(connectString, getTimeout(), watcher);
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

/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.jaas;


import org.apache.felix.utils.properties.Properties;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.linkedin.zookeeper.client.ZKData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ZookeeperProperties extends Properties implements LifecycleListener, Watcher {

    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperProperties.class);

    protected String path;
    protected IZKClient zooKeeper;
    private  CountDownLatch connectedLatch = new CountDownLatch(1);
    private boolean connected = false;

    public ZookeeperProperties(IZKClient zooKeeper, String path) throws Exception {
        this.path = path;
        this.zooKeeper = zooKeeper;
        this.zooKeeper.registerListener(this);
        connectedLatch.await(1, TimeUnit.SECONDS);
    }

    @Override
    public void save() throws IOException {
        StringWriter writer = new StringWriter();
        saveLayout(writer);
        try {
            zooKeeper.setData(path, writer.toString());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if (watchedEvent.getType() == Event.EventType.NodeDataChanged
         || watchedEvent.getType() == Event.EventType.NodeDeleted) {
            try {
                fetchData();
            } catch (Exception e) {
                LOG.warn("failed refreshing authentication data", e);
            }
        }
    }

    protected void fetchData() throws Exception {
        ZKData<String> zkData = zooKeeper.getZKStringData(path, this);
        String value = zkData.getData();
        if (value != null) {
            load(new StringReader(value));
        }
    }

    @Override
    public void onConnected() {
        try {
            if (!connected) {
                fetchData();
                connected = true;
                connectedLatch.countDown();
            }
        } catch (Exception e) {
            LOG.warn("Failed initializing authentication plugin", e);
        }
    }

    @Override
    public void onDisconnected() {
        connected = false;
    }
}

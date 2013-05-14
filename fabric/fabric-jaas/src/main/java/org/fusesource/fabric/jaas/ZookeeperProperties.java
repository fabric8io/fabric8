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
package org.fusesource.fabric.jaas;


import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.felix.utils.properties.Properties;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.linkedin.zookeeper.client.LifecycleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.fusesource.fabric.zookeeper.utils.ZooKeeperUtils.*;

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
            setData(zooKeeper, path, writer.toString());
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
        String value = getStringData(zooKeeper, path, this);
        if (value != null) {
            clear();
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

/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel;

import org.apache.camel.impl.DefaultComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKClient;

import java.util.List;

/**
 */
public abstract class ZKComponentSupport extends DefaultComponent {
    private static final transient Log LOG = LogFactory.getLog(MasterComponent.class);
    private IZKClient zkClient;
    protected List<ACL> accessControlList = ZooDefs.Ids.OPEN_ACL_UNSAFE;
    private boolean shouldCloseZkClient = false;
    private long maximumConnectionTimeout = 10 * 1000L;
    private long connectionRetryTime = 100L;

    public IZKClient getZkClient() {
        return zkClient;
    }

    public void setZkClient(IZKClient zkClient) {
        this.zkClient = zkClient;
    }

    public boolean isShouldCloseZkClient() {
        return shouldCloseZkClient;
    }

    public void setShouldCloseZkClient(boolean shouldCloseZkClient) {
        this.shouldCloseZkClient = shouldCloseZkClient;
    }

    public List<ACL> getAccessControlList() {
        return accessControlList;
    }

    public void setAccessControlList(List<ACL> accessControlList) {
        this.accessControlList = accessControlList;
    }

    public long getConnectionRetryTime() {
        return connectionRetryTime;
    }

    public void setConnectionRetryTime(long connectionRetryTime) {
        this.connectionRetryTime = connectionRetryTime;
    }

    public long getMaximumConnectionTimeout() {
        return maximumConnectionTimeout;
    }

    public void setMaximumConnectionTimeout(long maximumConnectionTimeout) {
        this.maximumConnectionTimeout = maximumConnectionTimeout;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (zkClient == null) {
            zkClient = (IZKClient) getCamelContext().getRegistry().lookup("zkClient");
            if (zkClient != null) {
                LOG.debug("IZKClient found in camel registry. " + zkClient);
            }
        }
        if (zkClient == null) {
            String connectString = System.getProperty("zookeeper.url", "localhost:2181");
            ZKClient client = new ZKClient(connectString, Timespan.parse("10s"), null);
            LOG.debug("IZKClient not find in camel registry, creating new with connection " + connectString);
            zkClient = client;
            setShouldCloseZkClient(true);
        }

        // ensure we are started
        if (zkClient instanceof ZKClient) {
            if (!zkClient.isConnected()) {
                LOG.debug("Staring IZKClient " + zkClient);
                ((ZKClient) zkClient).start();
            }
        }
        checkZkConnected();
    }

    /**
     * Lets check if we are connected and throw an exception if we are not.
     * Note that if start() has just been called on IZKClient then it will take a little
     * while for the connection to be established, so we keep checking up to the {@link #getMaximumConnectionTimeout()}
     * until we throw the exception
     */
    protected void checkZkConnected() throws Exception {
        long start = System.currentTimeMillis();
        do {
            if (zkClient.isConnected()) {
                return;
            }
            try {
                Thread.sleep(getConnectionRetryTime());
            } catch (InterruptedException e) {
                // ignore
            }
        } while (System.currentTimeMillis() < start + getMaximumConnectionTimeout());

        if (!zkClient.isConnected()) {
            throw new Exception("Could not connect to ZooKeeper " + zkClient + " at " + zkClient.getConnectString());
        }
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (zkClient != null && isShouldCloseZkClient()) {
            zkClient.close();
        }
    }}

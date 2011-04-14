/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.Watcher;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKClient;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import java.util.concurrent.TimeoutException;

/**
 * A Spring factory bean for creating ZK Clients
 */
public class ZKClientFactoryBean implements FactoryBean<IZKClient>, DisposableBean {
    private static final transient Log LOG = LogFactory.getLog(ZKClientFactoryBean.class);

    private String connectString = "localhost:2181";
    private String timeoutText = "30s";
    private Watcher watcher;
    private Timespan timeout;
    protected ZKClient zkClient;
    private Timespan connectTimeout = Timespan.parse("10s");


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
        return connectTimeout;
    }

    public void setConnectTimeout(Timespan connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    // FactoryBean interface
    //-------------------------------------------------------------------------
    public IZKClient getObject() throws Exception {
        LOG.debug("Connecting to ZooKeeper on " + connectString);

        zkClient = new ZKClient(connectString, getTimeout(), watcher);
        zkClient.start();
        try {
            zkClient.waitForStart(connectTimeout);
        } catch (TimeoutException e) {
            throw new Exception("Failed to connect to ZooKeeper on " + connectString, e);
        }
        return zkClient;
    }

    public Class<?> getObjectType() {
        return IZKClient.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void destroy() throws Exception {
        if (zkClient != null) {
            zkClient.close();
            zkClient = null;
        }

    }
}
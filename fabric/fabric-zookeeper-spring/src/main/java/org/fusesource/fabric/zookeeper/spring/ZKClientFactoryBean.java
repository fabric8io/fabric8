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
package org.fusesource.fabric.zookeeper.spring;

import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.Watcher;
import org.fusesource.fabric.zookeeper.internal.ZKClient;
import org.fusesource.fabric.zookeeper.IZKClient;
import org.linkedin.util.clock.Timespan;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

/**
 * A Spring factory bean for creating ZK Clients
 */
@Deprecated
public class ZKClientFactoryBean implements FactoryBean<IZKClient>, DisposableBean {
    private static final transient Log LOG = LogFactory.getLog(ZKClientFactoryBean.class);

    private String connectString = "localhost:2181";
    private String password;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // FactoryBean interface
    //-------------------------------------------------------------------------
    public IZKClient getObject() throws Exception {
        LOG.debug("Connecting to ZooKeeper on " + connectString);

        zkClient = new ZKClient(connectString, getTimeout(), watcher);
        zkClient.setPassword(password);
        zkClient.start();
        try {
            zkClient.waitForConnected(connectTimeout);
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
            // Note we cannot use zkClient.close()
            // since you cannot currently close a client which is not connected
            zkClient.close();
            zkClient = null;
        }

    }
}
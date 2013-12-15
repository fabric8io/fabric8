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
package io.fabric8.agent.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.Watcher;


public class CuratorFactoryBean {

    private static final transient Log LOG = LogFactory.getLog(CuratorFactoryBean.class);

    private String connectString = System.getProperty("zookeeper.url", "localhost:2181");
    private String password = System.getProperty("zookeeper.password", "");
    private int timeout = 30000;
    private Watcher watcher;
    protected CuratorFramework curator;


    public String getConnectString() {
        return connectString;
    }

    public void setConnectString(String connectString) {
        this.connectString = connectString;
    }

    public Watcher getWatcher() {
        return watcher;
    }

    public void setWatcher(Watcher watcher) {
        this.watcher = watcher;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    // FactoryBean interface
    //-------------------------------------------------------------------------
    public CuratorFramework getObject() throws Exception {
        LOG.debug("Connecting to ZooKeeper on " + connectString);

        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .retryPolicy(new ExponentialBackoffRetry(5, 10))
                .connectionTimeoutMs(getTimeout());

        if (password != null && !password.isEmpty()) {
            builder.authorization("digest", ("fabric:" + password).getBytes("UTF-8"));
        }

        this.curator = builder.build();
        LOG.debug("Starting curator " + curator);
        curator.start();
        return curator;
    }

    public Class<?> getObjectType() {
        return CuratorFramework.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void destroy() throws Exception {
        if (curator != null) {
            // Note we cannot use zkClient.close()
            // since you cannot currently close a client which is not connected
            curator.close();
            curator = null;
        }

    }
}

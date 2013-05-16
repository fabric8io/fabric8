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
package org.fusesource.fabric.camel;

import org.apache.camel.impl.DefaultComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;

/**
 */
public abstract class ZKComponentSupport extends DefaultComponent {
    private static final transient Log LOG = LogFactory.getLog(MasterComponent.class);
    private static final String ZOOKEEPER_URL = "zookeeper.url";
    private static final String ZOOKEEPER_PASSWORD = "zookeeper.password";

    private CuratorFramework curator;
    private boolean shouldCloseZkClient = false;
    private int maximumConnectionTimeout = 10 * 1000;
    private String zooKeeperUrl;
    private String zooKeeperPassword;

    public CuratorFramework getCurator() {
        return curator;
    }

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
    }

    public boolean isShouldCloseZkClient() {
        return shouldCloseZkClient;
    }

    public void setShouldCloseZkClient(boolean shouldCloseZkClient) {
        this.shouldCloseZkClient = shouldCloseZkClient;
    }

    public int getMaximumConnectionTimeout() {
        return maximumConnectionTimeout;
    }

    public void setMaximumConnectionTimeout(int maximumConnectionTimeout) {
        this.maximumConnectionTimeout = maximumConnectionTimeout;
    }


    public String getZooKeeperUrl() {
        return zooKeeperUrl;
    }

    public void setZooKeeperUrl(String zooKeeperUrl) {
        this.zooKeeperUrl = zooKeeperUrl;
    }

    public String getZooKeeperPassword() {
        return zooKeeperPassword;
    }

    public void setZooKeeperPassword(String zooKeeperPassword) {
        this.zooKeeperPassword = zooKeeperPassword;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        if (curator == null) {
            try {
                curator = (CuratorFramework) getCamelContext().getRegistry().lookup("curator");
            } catch (Exception exception) {
                // try to get the curator from the OSGi service registry
                curator = (CuratorFramework) getCamelContext().getRegistry().lookup(CuratorFramework.class.getName());
            }
            if (curator != null) {
                LOG.debug("IZKClient found in camel registry. " + curator);
            }
        }
        if (curator == null) {
            String connectString = getZooKeeperUrl();
            if (connectString == null) {
                connectString = System.getProperty(ZOOKEEPER_URL, "localhost:2181");
            }
            String password = getZooKeeperPassword();
            if (password == null) {
                System.getProperty(ZOOKEEPER_PASSWORD);
            }
            LOG.debug("CuratorFramework not find in camel registry, creating new with connection " + connectString);
            CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                                                                             .connectString(connectString)
                                                                             .retryPolicy(new RetryOneTime(1000))
                                                                             .connectionTimeoutMs(getMaximumConnectionTimeout());

            if (password != null && !password.isEmpty()) {
                builder.authorization("digest", ("fabric:"+password).getBytes());
            }

            CuratorFramework client = builder.build();
            LOG.debug("Starting curator " + curator);
            client.start();
            curator = client;
            setShouldCloseZkClient(true);
        }
        // ensure we are started
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (curator != null && isShouldCloseZkClient()) {
            curator.close();
        }
    }
}

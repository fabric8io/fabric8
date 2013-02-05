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
import org.fusesource.fabric.zookeeper.IZKClient;
import org.fusesource.fabric.zookeeper.internal.ZKClient;
import org.linkedin.util.clock.Timespan;

/**
 */
public abstract class ZKComponentSupport extends DefaultComponent {
    private static final transient Log LOG = LogFactory.getLog(MasterComponent.class);
    private static final String ZOOKEEPER_URL = "zookeeper.url";
    private static final String ZOOKEEPER_PASSWORD = "zookeeper.password";

    private IZKClient zkClient;
    private boolean shouldCloseZkClient = false;
    private long maximumConnectionTimeout = 10 * 1000L;

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
            try {
                zkClient = (IZKClient) getCamelContext().getRegistry().lookup("zkClient");
            } catch (Exception exception) {
                // try to get the zkClient from the OSGi service registry
                zkClient = (IZKClient) getCamelContext().getRegistry().lookup(IZKClient.class.getName());
            }
            if (zkClient != null) {
                LOG.debug("IZKClient found in camel registry. " + zkClient);
            }
        }
        if (zkClient == null) {
            String connectString = System.getProperty(ZOOKEEPER_URL, "localhost:2181");
            String password = System.getProperty(ZOOKEEPER_PASSWORD);
            LOG.debug("IZKClient not find in camel registry, creating new with connection " + connectString);
            ZKClient client = new ZKClient(connectString, Timespan.parse("10s"), null);
            if (password != null && !password.isEmpty()) {
                client.setPassword(password);
            }
            LOG.debug("Starting IZKClient " + zkClient);
            client.start();
            zkClient = client;
            setShouldCloseZkClient(true);
        }
        // ensure we are started
        zkClient.waitForConnected(new Timespan(getMaximumConnectionTimeout()));
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        if (zkClient != null && isShouldCloseZkClient()) {
            zkClient.close();
        }
    }
}

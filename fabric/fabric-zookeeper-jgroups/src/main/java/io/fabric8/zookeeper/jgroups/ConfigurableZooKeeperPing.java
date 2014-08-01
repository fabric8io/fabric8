/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.zookeeper.jgroups;

import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.jgroups.annotations.MBean;
import org.jgroups.annotations.Property;
import org.jgroups.conf.ClassConfigurator;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Bela Ban
 */
@MBean(description = "ZooKeeper based discovery protocol. Acts as a ZooKeeper client and accesses ZooKeeper servers " +
    "to fetch discovery information")
public class ConfigurableZooKeeperPing extends AbstractZooKeeperPing {
    @Property
    protected String connection;

    @Property
    protected String password;

    @Property
    protected int connectionTimeout = Constants.DEFAULT_CONNECTION_TIMEOUT_MS;

    @Property
    protected int sessionTimeout = Constants.DEFAULT_SESSION_TIMEOUT_MS;

    @Property
    protected int maxRetry = Constants.MAX_RETRIES_LIMIT;

    @Property
    protected int retryInterval = Constants.DEFAULT_RETRY_INTERVAL;

    @Property
    protected int mode = CreateMode.EPHEMERAL.toFlag();

    private ACLProvider aclProvider;

    static {
        ClassConfigurator.addProtocol(Constants.CONFIGURABLE_ZK_PING_ID, ConfigurableZooKeeperPing.class);
    }

    @Override
    protected CreateMode getCreateMode() throws KeeperException {
        return CreateMode.fromFlag(mode);
    }

    @Override
    public void init() throws Exception {
        if (connection == null || connection.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing connection property!");
        }
        super.init();
        curator.start();
    }

    @Override
    public void destroy() {
        try {
            curator.close();
        } finally {
            super.destroy();
        }
    }

    protected String getScheme() {
        return "digest";
    }

    protected byte[] getAuth() {
        return password.getBytes();
    }

    protected CuratorFramework createCurator() throws KeeperException {
        log.info(String.format("Creating curator [%s], mode: %s", connection, getCreateMode()));

        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
            .ensembleProvider(new FixedEnsembleProvider(connection))
            .connectionTimeoutMs(connectionTimeout)
            .sessionTimeoutMs(sessionTimeout)
            .retryPolicy(new RetryNTimes(maxRetry, retryInterval));

        if (password != null && password.length() > 0) {
            builder = builder.authorization(getScheme(), getAuth()).aclProvider(aclProvider);
        }

        return builder.build();
    }

    public void setConnection(String connection) {
        this.connection = connection;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    public void setAclProvider(ACLProvider aclProvider) {
        this.aclProvider = aclProvider;
    }
}

/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.zookeeper.spring;

import java.io.File;
import java.net.InetSocketAddress;

import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * A simple ZK server for testing ZK related code in unit tests
 */
public class ZKServerFactoryBean implements FactoryBean<ZooKeeperServer>, InitializingBean, DisposableBean {
    private ZooKeeperServer zooKeeperServer = new ZooKeeperServer();
    private NIOServerCnxnFactory connectionFactory;
    private File dataLogDir;
    private File dataDir;
    protected int tickTime = ZooKeeperServer.DEFAULT_TICK_TIME;
    /**
     * defaults to -1 if not set explicitly
     */
    private int minSessionTimeout = -1;
    /**
     * defaults to -1 if not set explicitly
     */
    private int maxSessionTimeout = -1;
    private InetSocketAddress clientPortAddress;
    private int maxClientConnections;
    private int port = 2181;

    public ZooKeeperServer getObject() throws Exception {
        return zooKeeperServer;
    }

    public Class<ZooKeeperServer> getObjectType() {
        return ZooKeeperServer.class;
    }

    public boolean isSingleton() {
        return true;
    }

    public void afterPropertiesSet() throws Exception {
        FileTxnSnapLog ftxn = new FileTxnSnapLog(getDataLogDir(), getDataDir());
        zooKeeperServer.setTxnLogFactory(ftxn);
        zooKeeperServer.setTickTime(getTickTime());
        zooKeeperServer.setMinSessionTimeout(getMinSessionTimeout());
        zooKeeperServer.setMaxSessionTimeout(getMaxSessionTimeout());
        connectionFactory = new NIOServerCnxnFactory();
        connectionFactory.configure(getClientPortAddress(), getMaxClientConnections());
        connectionFactory.startup(zooKeeperServer);
    }

    public void destroy() throws Exception {
        shutdown();
    }

    protected void shutdown() {
        if (connectionFactory != null) {
            connectionFactory.shutdown();
            connectionFactory = null;
        }
    }

    // Properties
    //-------------------------------------------------------------------------

    public ZooKeeperServer getZooKeeperServer() {
        return zooKeeperServer;
    }

    public NIOServerCnxnFactory getConnectionFactory() {
        return connectionFactory;
    }

    public File getDataLogDir() {
        if (dataLogDir == null) {
            dataLogDir = new File(getZKOutputDir(), "log");
            dataLogDir.mkdirs();
        }
        return dataLogDir;
    }

    public File getDataDir() {
        if (dataDir == null) {
            dataDir = new File(getZKOutputDir(), "data");
            dataDir.mkdirs();
        }
        return dataDir;
    }

    public int getTickTime() {
        return tickTime;
    }

    public int getMinSessionTimeout() {
        return minSessionTimeout;
    }

    public int getMaxSessionTimeout() {
        return maxSessionTimeout;
    }


    public InetSocketAddress getClientPortAddress() {
        if (clientPortAddress == null) {
            clientPortAddress = new InetSocketAddress(port);
        }
        return clientPortAddress;
    }

    public int getMaxClientConnections() {
        return maxClientConnections;
    }

    public void setClientPortAddress(InetSocketAddress clientPortAddress) {
        this.clientPortAddress = clientPortAddress;
    }

    public void setConnectionFactory(NIOServerCnxnFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    public void setDataDir(File dataDir) {
        this.dataDir = dataDir;
    }

    public void setDataLogDir(File dataLogDir) {
        this.dataLogDir = dataLogDir;
    }

    public void setMaxClientConnections(int maxClientConnections) {
        this.maxClientConnections = maxClientConnections;
    }

    public void setMaxSessionTimeout(int maxSessionTimeout) {
        this.maxSessionTimeout = maxSessionTimeout;
    }

    public void setMinSessionTimeout(int minSessionTimeout) {
        this.minSessionTimeout = minSessionTimeout;
    }

    public void setTickTime(int tickTime) {
        this.tickTime = tickTime;
    }

    public void setZooKeeperServer(ZooKeeperServer zooKeeperServer) {
        this.zooKeeperServer = zooKeeperServer;
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    protected File getZKOutputDir() {
        String baseDir = System.getProperty("basedir", ".");
        File dir = new File(baseDir + "/target/zk");
        dir.mkdirs();
        return dir;
    }


}

/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.cxf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.ServerLifeCycleManager;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.ZooKeeperGroupFactory;
import org.fusesource.fabric.zookeeper.ZKClientFactoryBean;
import org.linkedin.zookeeper.client.IZKClient;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class FabricFeature extends AbstractFeature implements InitializingBean, DisposableBean {
    private static final transient Log LOG = LogFactory.getLog(FabricFeature.class);
    @Autowired
    private IZKClient zkClient;
    private String zkRoot = "/fabric/cxf/endpoints";
    private String fabricPath;
    private Group group;
    private List<ACL> accessControlList = ZooDefs.Ids.OPEN_ACL_UNSAFE;

    public void initialize(Client client, Bus bus) {
         // setup the failover conduit selector for it
    }

    public void initialize(Bus bus) {
        FabricServerListener lister = new FabricServerListener(group);
        // register the listener itself
        ServerLifeCycleManager mgr = bus.getExtension(ServerLifeCycleManager.class);
        if (mgr != null) {
            mgr.registerListener(lister);
        } else {
            LOG.warn("Cannot find the ServerLifeCycleManager ");
        }
    }

    protected void doStart() throws Exception {
        if (zkClient == null) {
            zkClient = new ZKClientFactoryBean().getObject();
        }
        checkZkConnected();
    }

    protected void checkZkConnected() throws Exception {
        if (!zkClient.isConnected()) {
            throw new Exception("Could not connect to ZooKeeper " + zkClient);
        }
    }

    protected void doStop() throws Exception {
        if (zkClient != null) {
            zkClient.close();
        }
    }


    public void afterPropertiesSet() throws Exception {
        doStart();
        group = ZooKeeperGroupFactory.create(getZkClient(), fabricPath, accessControlList);
    }

    public void destroy() throws Exception {
        doStop();
    }

    public String getFabricPath() {
        return fabricPath;
    }

    public void setFabricPath(String fabricPath) {
        this.fabricPath = fabricPath;
    }

    public List<ACL> getAccessControlList() {
        return accessControlList;
    }

    public void setAccessControlList(List<ACL> accessControlList) {
        this.accessControlList = accessControlList;
    }

    public IZKClient getZkClient() {
        return zkClient;
    }

    public void setZkClient(IZKClient zkClient) {
        this.zkClient = zkClient;
    }
}

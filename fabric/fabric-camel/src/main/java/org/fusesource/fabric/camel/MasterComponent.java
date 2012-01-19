/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.impl.ProducerCache;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.ServiceHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.data.ACL;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.ZooKeeperGroupFactory;
import org.linkedin.util.clock.Timespan;
import org.linkedin.zookeeper.client.IZKClient;
import org.linkedin.zookeeper.client.ZKClient;

import java.util.List;
import java.util.Map;

/**
 * The MASTER camel component ensures that only a single endpoint in a cluster is active at any
 * point in time with all other JVMs being hot standbys which wait until the master JVM dies before
 * taking over to provide high availability of a single consumer.
 */
public class MasterComponent extends ZKComponentSupport {
    private String zkRoot = "/fabric/camel/master";

    public String getZkRoot() {
        return zkRoot;
    }

    public void setZkRoot(String zkRoot) {
        this.zkRoot = zkRoot;
    }

    //  Implementation methods
    //-------------------------------------------------------------------------


    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> params) throws Exception {
        int idx = remaining.indexOf(':');
        if (idx <= 0) {
            throw new IllegalArgumentException("Missing : in URI so can't split the group name from the actual URI for '" + remaining + "'");
        }
        // we are registering a regular endpoint
        String name = remaining.substring(0, idx);
        String fabricPath = getFabricPath(name);
        String childUri = remaining.substring(idx + 1);

        Group group = ZooKeeperGroupFactory.create(getZkClient(), fabricPath, accessControlList);
        return new MasterEndpoint(uri, this, group, childUri);
    }

    protected String getFabricPath(String name) {
        String path = name;
        if (ObjectHelper.isNotEmpty(zkRoot)) {
            path = zkRoot + "/" + name;
        }
        return path;
    }

}

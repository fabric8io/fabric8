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
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.fusesource.fabric.groups.Group;

public class FabricServerListener implements ServerLifeCycleListener {
    private static final transient Log LOG = LogFactory.getLog(FabricServerListener.class);
    private final Group group;


    FabricServerListener(Group group) {
        this.group = group;
    }


    public void startServer(Server server) {
        // get the server address
        String address = server.getEndpoint().getEndpointInfo().getAddress();
        if (LOG.isDebugEnabled()) {
            LOG.debug("The CXF server is start with address " + address);
        }
        try {
            group.join(address, address.getBytes("UTF-8"));
        } catch (Exception ex) {
            LOG.warn("Cannot bind the address " + address + " to the group, due to ", ex);
        }
    }

    public void stopServer(Server server) {
        // get the server address
        String address = server.getEndpoint().getEndpointInfo().getAddress();
        if (LOG.isDebugEnabled()) {
            LOG.debug("The CXF server is stopped with address " + address);
        }
        group.leave(address);
    }
}

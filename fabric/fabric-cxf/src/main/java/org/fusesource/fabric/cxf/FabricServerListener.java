/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private String eid;


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
            eid = group.join(address.getBytes("UTF-8"));
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
        group.leave(eid);
    }
}

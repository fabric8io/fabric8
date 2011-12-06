/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.mq.fabric;

import org.apache.activemq.transport.discovery.DiscoveryAgent;
import org.apache.activemq.transport.discovery.DiscoveryAgentFactory;
import org.apache.activemq.util.IOExceptionSupport;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.apache.activemq.util.URISupport.*;

public class FabricDiscoveryAgentFactory extends DiscoveryAgentFactory {

    protected DiscoveryAgent doCreateDiscoveryAgent(URI uri) throws IOException {
        try {
            
            FabricDiscoveryAgent rc = new FabricDiscoveryAgent();
            if( uri.getSchemeSpecificPart()!=null && uri.getSchemeSpecificPart().length() > 0 ){
                String ssp = stripPrefix(uri.getSchemeSpecificPart(), "//");
                Map<String, String> query = parseQuery(ssp);
                String groupName = ssp.split("\\?")[0];
                if( query.get("id")!=null ) {
                    rc.setId(query.get("id"));
                }
                rc.setGroupName(groupName);
            }
            return rc;
            
        } catch (Throwable e) {
            throw IOExceptionSupport.create("Could not create discovery agent: " + uri, e);
        }
    }
}

/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel;

/**
 * Thrown when no physical endpoint could be found in the ZooKeeper registry for the global FABRIC name
 */
public class NoEndpointAvailableException extends Exception {
    private final FabricEndpoint endpoint;

    public NoEndpointAvailableException(FabricEndpoint endpoint) {
        super("No endpoint available for FABRIC name: " + endpoint.getFabricPath());
        this.endpoint = endpoint;
    }

    public FabricEndpoint getEndpoint() {
        return endpoint;
    }

    public String getFabricName() {
        return endpoint.getFabricPath();
    }
}

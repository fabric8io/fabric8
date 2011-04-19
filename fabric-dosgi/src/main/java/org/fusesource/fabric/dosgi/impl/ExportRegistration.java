/**
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.dosgi.impl;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import org.fusesource.fabric.dosgi.util.BundleDelegatingClassLoader;
import org.osgi.framework.ServiceReference;

public class ExportRegistration {

    final ServiceReference exportedService;
    final EndpointDescription exportedEndpoint;
    final String zooKeeperNode;
    final XStream xstream;
    boolean closed;

    public ExportRegistration(ServiceReference exportedService, EndpointDescription exportedEndpoint, String zooKeeperNode) {
        this.exportedService = exportedService;
        this.exportedEndpoint = exportedEndpoint;
        this.zooKeeperNode = zooKeeperNode;
        this.xstream = new XStream(null, new StaxDriver(), new BundleDelegatingClassLoader(exportedService.getBundle(), Manager.class.getClassLoader()));
    }

    public EndpointDescription getExportedEndpoint() {
        return closed ? null : exportedEndpoint;
    }

    public ServiceReference getExportedService() {
        return closed ? null : exportedService;
    }

    public String getZooKeeperNode() {
        return closed ? null : zooKeeperNode;
    }

    public XStream getXStream() {
        return xstream;
    }

    public void close() {
        closed = true;
    }

}

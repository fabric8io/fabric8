/*
 * Copyright (C) 2011, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel;

import org.apache.camel.*;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.fabric.groups.ChangeListener;
import org.fusesource.fabric.groups.Group;

import java.io.UnsupportedEncodingException;

/**
 * Creates an endpoint which uses FABRIC to map a logical name to physical endpoint names
 */
public class FabricPublisherEndpoint extends DefaultEndpoint {
    private static final transient Log LOG = LogFactory.getLog(FabricPublisherEndpoint.class);

    private final FabricComponent component;
    private final Group group;
    private final String child;

    public FabricPublisherEndpoint(String uri, FabricComponent component, Group group, String child) {
        super(uri, component);
        this.component = component;
        this.group = group;
        this.child = child;
    }

    public Producer createProducer() throws Exception {
        return getCamelContext().getEndpoint(child).createProducer();
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        return getCamelContext().getEndpoint(child).createConsumer(processor);
    }

    public boolean isSingleton() {
        return true;
    }

    @Override
    public void start() throws Exception {
        super.start();
        group.join(child.getBytes("UTF-8"));
    }

    @Override
    public void stop() throws Exception {
        group.close();
        super.stop();
    }

    // Properties
    //-------------------------------------------------------------------------
    public FabricComponent getComponent() {
        return component;
    }

    public String getChild() {
        return child;
    }
}

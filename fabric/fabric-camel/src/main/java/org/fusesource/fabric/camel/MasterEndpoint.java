/**
 * Copyright (C) 2010, FuseSource Corp.  All rights reserved.
 * http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * AGPL license a copy of which has been included with this distribution
 * in the license.txt file.
 */
package org.fusesource.fabric.camel;

import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.fabric.groups.ClusteredSingleton;
import org.fusesource.fabric.groups.Group;
import org.fusesource.fabric.groups.TextNodeState;

/**
 * Represents an endpoint which only becomes active when it obtains the master lock
 */
public class MasterEndpoint extends DefaultEndpoint {
    private static final transient Log LOG = LogFactory.getLog(MasterEndpoint.class);

    private final MasterComponent component;
    private final Group group;
    private final String child;
    private ClusteredSingleton<TextNodeState> cluster;


    public MasterEndpoint(String uri, MasterComponent component, Group group, String child) {
        super(uri, component);
        this.component = component;
        this.group = group;
        this.child = child;
        cluster = new ClusteredSingleton<TextNodeState>(TextNodeState.class, uri);
    }

    @Override
    public Producer createProducer() throws Exception {
        return getChildEndpoint().createProducer();
    }

    public Consumer createConsumer(Processor processor) throws Exception {
        return new MasterConsumer(this, processor);
    }

    public boolean isSingleton() {
        return true;
    }

    @Override
    protected void doStop() throws Exception {
        getCluster().stop();
        super.doStop();
    }

    @Override
    protected void doStart() throws Exception {
        getCluster().start(group);
        super.doStart();
    }

    // Properties
    //-------------------------------------------------------------------------
    public MasterComponent getComponent() {
        return component;
    }

    public Group getGroup() {
        return group;
    }

    public ClusteredSingleton<TextNodeState> getCluster() {
        return cluster;
    }

    public String getChild() {
        return child;
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    Endpoint getChildEndpoint() {
        return getCamelContext().getEndpoint(child);
    }
}

/**
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private final String singletonId;
    private final Group group;
    private final String child;
    private final ClusteredSingleton<TextNodeState> cluster;


    public MasterEndpoint(String uri, MasterComponent component, String singletonId, Group group, String child) {
        super(uri, component);
        this.component = component;
        this.singletonId = singletonId;
        this.group = group;
        this.child = child;
        this.cluster = new ClusteredSingleton<TextNodeState>(TextNodeState.class);
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
    public boolean isLenientProperties() {
        // to allow properties to be propagated to the child endpoint
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

    public String getSingletonId() {
        return singletonId;
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    Endpoint getChildEndpoint() {
        return getCamelContext().getEndpoint(child);
    }
}
